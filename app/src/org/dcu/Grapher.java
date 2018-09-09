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
import java.util.regex.Pattern;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
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

public class Grapher extends RNode implements Runnable {

	private HFClient client;
	private Channel channel;
	private Thread thread;
	private boolean active = true;
	private final Logger log = Logger.getLogger(Grapher.class);
	private long startTime;

	private String myName = "grapher";
	private int numOfRobots;

	public Grapher(int numOfRobots) throws Exception {
		super();
		this.numOfRobots = numOfRobots;
		Logger.getRootLogger().setLevel(Level.INFO);
		// create fabric-ca client
		HFCAClient caClient = getHfCaClient(CA_ORG_URL, null);
		// enroll or load admin
		RUser admin = getAdmin(caClient);
		// register and enroll new user
		RUser robotUser = getUser(caClient, admin, myName);
		// get HFC client instance
		client = getHfClient();
		// set user context
		client.setUserContext(admin);
		// get HFC channel using the client
		channel = getChannel(client);

		ChaincodeEventListener chaincodeEventListener = new ChaincodeEventListener() {

			@Override
			public void received(String handle, BlockEvent blockEvent, ChaincodeEvent chaincodeEvent) {

				if (chaincodeEvent.getEventName().equals(PATH_COMMITTED_EVENT)) {

					try {
						Path committedPath = new Path(new String(chaincodeEvent.getPayload(), "UTF-8"));
						log.info("Robot[" + committedPath.id + "] committed Block# " + blockEvent.getBlockNumber() + " | TxID: " + chaincodeEvent.getTxId());
					} catch (UnsupportedEncodingException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					try {
						if (isConsensusReached()) {
							log.info("Consensus Latency = " +
									new Long((System.nanoTime() - startTime)/1000000) + " ms");
							drawWorkspace();
						}

					} catch (IOException | InvalidArgumentException | ProposalException e) {
						e.printStackTrace();
					}
				}
			}
		};

		// register event listener/handler for path-committed event
		channel.registerChaincodeEventListener(Pattern.compile(CHAINCODE_NAME),
				Pattern.compile(PATH_COMMITTED_EVENT), chaincodeEventListener);

		this.thread = new Thread(this);
	}

	public void go() {
		this.thread.start();
	}

	// draw workspace + paths in MATLAB
	private void drawWorkspace()
			throws ProposalException, InvalidArgumentException, IOException {
		// get workspace
		Workspace workspace = qry_getWorkspace(client);
		// get all paths
		ArrayList<Path> paths = qry_getAllPaths(client, myName);

		// add paths to workspace
		for (int i = 0; i < paths.size(); i++) {
			workspace.addPath(paths.get(i));
		}

		// draw in Matlab
		workspace.writeMatlabDisplayCode("workspace.m");
		log.debug("Workspace can be visualized in Matlab by running (multi-robot-workspace.m)");
		log.debug(workspace.toString());
	}

	// return true if consensus was reached
	private boolean isConsensusReached()
			throws InvalidArgumentException, ProposalException {
		// get all paths
		ArrayList<Path> paths = qry_getAllPaths(client, myName);

		if (numOfRobots != paths.size())
			// not all robots are done planning
			return false;

		// loop on all paths and check if any are colliding
		// if colliding, then re-planning is on-going
		for (int i = 0; i < paths.size(); i++) {
			for (int j = i+1; j < paths.size(); j++) {

				if (paths.get(i).collidesWith(paths.get(j)))
					return false;
			}
		}

		log.info("!! CONSENSUS REACHED !!");
		return true;
	}

	@Override
	public void run() {

		// start counter for consensus latency
		startTime = System.nanoTime();

		while(active){
			try{
				Thread.sleep(1);
			}
			catch(InterruptedException e){
				System.out.println("Thread Interrupted!");
			}
		}
	}
}
