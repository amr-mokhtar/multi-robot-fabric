/*
 * Copyright (c) 2018 Amr Mokhtar.
 * All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.dcu;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dcu.prm.PRMPlanner;
import org.dcu.prm.Path;
import org.dcu.prm.Workspace;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.ChaincodeEvent;
import org.hyperledger.fabric.sdk.ChaincodeEventListener;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric_ca.sdk.HFCAClient;

public class Robot extends RNode implements Runnable {

	private HFClient client;
	private Channel channel;
	private final Logger log;
	private Thread thread;
	private long startTime;

	// up-to-date workspace
	private Workspace workspace;
	// my planned path
	private Path myPath = null;
	private String myName;

	public final Object finished = new Object();

	public int maxAttempts = 10;
	public int numNodes = 1000;
	public int numEdges = 40;
	public double stepSize = 0.1;

	public Robot(String name) throws Exception {
		super();
		// save robot name
		myName = name;
		log = Logger.getLogger(myName);
		Logger.getRootLogger().setLevel(Level.INFO);

		// create fabric-ca client
		HFCAClient caClient = getHfCaClient(CA_ORG_URL, null);

		// enroll or load admin
		RUser admin = getAdmin(caClient);

		// register and enroll new user
		RUser robotUser = getUser(caClient, admin, myName);
		log.debug(robotUser);

		// get HFC client instance
		client = getHfClient();
		// set user context
		client.setUserContext(admin);

		// get HFC channel using the client
		channel = getChannel(client);
		log.debug("Joined channel[" + channel.getName() + "]");

		ChaincodeEventListener pathCommittedEventListener = new ChaincodeEventListener() {

			@Override
			public void received(String handle, BlockEvent blockEvent, ChaincodeEvent chaincodeEvent) {

				if (chaincodeEvent.getEventName().equals(PATH_COMMITTED_EVENT)) {

					try {
						Path committedPath = new Path(new String(chaincodeEvent.getPayload(), "UTF-8"));
						log.info("Robot[" + committedPath.id + "] committed Block# " + blockEvent.getBlockNumber() + " | TxID: " + chaincodeEvent.getTxId());

						if (committedPath.id.equals(myName)) {

							log.info("Blockchain Commit Latency = " +
									new Long((System.nanoTime() - startTime)/1000000) + " ms");
							// this is my path
							synchronized (finished) {
								// path is committed - done
								finished.notify();
							}
						}
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			}
		};

		// register event listener/handler for path-committed event
		channel.registerChaincodeEventListener(Pattern.compile(CHAINCODE_NAME),
				Pattern.compile(PATH_COMMITTED_EVENT), pathCommittedEventListener);

		this.thread = new Thread(this);
	}

	public void go() {
		this.thread.start();
	}

	private void updateWorkspace(ArrayList<Path> paths)
			throws InvalidArgumentException, ProposalException {
		// workspace is static, delete only stored paths
		workspace.deleteAllPaths();
		// add them to recreated workspace
		for (int i = 0; i < paths.size(); i++) {
			// add paths to workspace
			workspace.addPath(paths.get(i));
		}
	}

	// return false if failed finding a path
	private boolean findPath(Workspace workspace)
			throws ProposalException, InvalidArgumentException, IOException, InterruptedException,
			ExecutionException, TimeoutException {
		// allocate new path planner
		PRMPlanner planner = new PRMPlanner();
		// build the road map for planning
		planner.buildRoadMap(workspace,	numNodes, numEdges, stepSize);
		// find a path
		Path path = planner.findPath(workspace, maxAttempts);

		if (path != null) {
			// save planned path
			myPath = path;
			log.info("Found a path with " + myPath.points.size() + " nodes");
			log.debug("New Path: " + myPath.toJSONString());

			log.info("Path Planning Time = " +
					new Long((System.nanoTime() - startTime)/1000000) + " ms");

			// reset counter for blockchain latency measurement
			startTime = System.nanoTime();
			// invoke chaincode
			invk_setMyPath(client, myName, myPath);
			return true;
		} else {
			log.info("DID NOT FIND A PATH!!");
			return false;
		}
	}

	@Override
	public void run(){

		// start counter for path planning execution time
		startTime = System.nanoTime();

		try {
			// retrieve workspace from blockchain
			workspace = qry_getWorkspace(client);
			// get all pre-planned paths by other robots
			ArrayList<Path> peerPaths = qry_getAllPaths(client, myName);
			// update workspace with all planned paths on blockchain
			updateWorkspace(peerPaths);
			// perform path planning
			findPath(workspace);

		} catch (InvalidArgumentException | ProposalException | IOException | InterruptedException | ExecutionException
				| TimeoutException e1) {
			e1.printStackTrace();
		}

		System.out.println(String.format("[%s] submitted path to Fabric!", myName));
	}
}
