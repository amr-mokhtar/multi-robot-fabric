/*
 * Copyright (c) Lukas Kolisko.
 * All Rights Reserved.
 */

/*
 * Copyright (c) 2018 Amr Mokhtar.
 * All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.dcu;

import static java.lang.String.format;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dcu.prm.Path;
import org.dcu.prm.Position;
import org.dcu.prm.Workspace;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.SDKUtils;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;

public abstract class RNode {

	protected static String ORG_MSP = "Org1MSP";
	protected static String ORG = "org1";
	protected static String ADMIN = "admin";
	protected static String ADMIN_PASSWORD = "adminpw";
	protected static String CA_ORG_URL = "http://localhost:7054";
	protected static String ORDERER_URL = "grpc://localhost:7050";
	protected static String ORDERER_NAME = "orderer.dcu.ie";
	protected static String CHANNEL_NAME = "mychannel";
	protected static String ORG_PEER_NAME = "peer0.org1.dcu.ie";
	protected static String ORG_PEER_URL = "grpc://localhost:7051";

	protected static String CHAINCODE_NAME = "multi-robot";
	protected static String PATH_COMMITTED_EVENT = "path-committed";

	protected static Logger log = Logger.getLogger(RNode.class);

	public RNode() {
		Logger.getRootLogger().setLevel(Level.DEBUG);
	}

	/**
	 * Initialize and get HF channel
	 *
	 * @param client The HFC client
	 * @return Initialized channel
	 * @throws InvalidArgumentException
	 * @throws TransactionException
	 */
	protected static Channel getChannel(HFClient client)
			throws InvalidArgumentException, TransactionException {
		// initialize channel
		// peer name and endpoint in multi-robot network
		Peer peer = client.newPeer(ORG_PEER_NAME, ORG_PEER_URL);
		// orderer name and endpoint in multi-robot network
		Orderer orderer = client.newOrderer(ORDERER_NAME, ORDERER_URL);
		// channel name in multi-robot network
		Channel channel = client.newChannel(CHANNEL_NAME);

		//PeerOptions eventingPeerOptions = createPeerOptions().setPeerRoles(EnumSet.of(PeerRole.EVENT_SOURCE));
		//eventingPeerOptions.registerEventsForFilteredBlocks();
		//channel.addPeer(peer, eventingPeerOptions.startEventsNewest());

		channel.addPeer(peer);
		channel.addOrderer(orderer);
		channel.initialize();
		return channel;
	}

	/**
	 * Create new HLF client
	 *
	 * @return new HLF client instance. Never null.
	 * @throws CryptoException
	 * @throws InvalidArgumentException
	 */
	protected static HFClient getHfClient()
			throws Exception {
		// initialize default cryptosuite
		CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite();
		// setup the client
		HFClient client = HFClient.createNewInstance();
		client.setCryptoSuite(cryptoSuite);
		return client;
	}

	/**
	 * Register and enroll user with robotId.
	 * If RUser object with the name already exist on fs it will be loaded and
	 * registration and enrollment will be skipped.
	 *
	 * @param caClient  The fabric-ca client.
	 * @param registrar The registrar to be used.
	 * @param robotId    The user id.
	 * @return RUser instance with robotId, affiliation,mspId and enrollment set.
	 * @throws Exception
	 */
	protected static RUser getUser(HFCAClient caClient, RUser registrar, String robotId)
			throws Exception {

		RUser robotUser /*= tryDeserialize(robotId)*/;
		//if (robotUser == null) {
			RegistrationRequest rr = new RegistrationRequest(robotId, ORG);
			String enrollmentSecret = caClient.register(rr, registrar);
			Enrollment enrollment = caClient.enroll(robotId, enrollmentSecret);
			robotUser = new RUser(robotId, ORG, ORG_MSP, enrollment);
		//	serialize(robotUser);
		//}
		return robotUser;
	}

	/**
	 * Enroll admin into fabric-ca using {@code admin/adminpw} credentials.
     * If RUser object already exist serialized on fs it will be loaded and
     * new enrollment will not be executed.
     *
	 * @param caClient The fabric-ca client
	 * @return RUser instance with robotId, affiliation, mspId and enrollment set
	 * @throws Exception
	 */
	protected static RUser getAdmin(HFCAClient caClient) throws Exception {

		RUser admin /*= tryDeserialize("admin")*/;
		//if (admin == null) {
			Enrollment adminEnrollment = caClient.enroll(ADMIN, ADMIN_PASSWORD);
			admin = new RUser(ADMIN, ORG, ORG_MSP, adminEnrollment);
		//	serialize(admin);
		//}
		return admin;
	}

	/**
	 * Get new fabic-ca client
	 *
	 * @param caUrl              The fabric-ca-server endpoint url
	 * @param caClientProperties The fabri-ca client properties. Can be null.
	 * @return new client instance. never null.
	 * @throws Exception
	 */
	protected static HFCAClient getHfCaClient(String caUrl, Properties caClientProperties)
			throws Exception {
		CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite();
		HFCAClient caClient = HFCAClient.createNewInstance(caUrl, caClientProperties);
		caClient.setCryptoSuite(cryptoSuite);
		return caClient;
	}

	protected static Workspace qry_getWorkspace(HFClient client)
			throws InvalidArgumentException, ProposalException {
		// get channel instance from client
		Channel channel = client.getChannel(CHANNEL_NAME);
		// create chaincode request
		QueryByChaincodeRequest query = client.newQueryProposalRequest();
		// build cc id providing the chaincode name. Version is omitted here.
		ChaincodeID multiRobotCCId = ChaincodeID.newBuilder().setName(CHAINCODE_NAME).build();
		query.setChaincodeID(multiRobotCCId);
		// CC function to be called
		query.setFcn("getWorkspace");
		// query blockchain
		Collection<ProposalResponse> response = channel.queryByChaincode(query, channel.getPeers());
		// display response
		for (ProposalResponse pRsp : response) {
			String stringResponse = new String(pRsp.getChaincodeActionResponsePayload());

			JsonReader jsonReader = Json.createReader(new StringReader(stringResponse));
			JsonObject ws = jsonReader.readObject();
			jsonReader.close();
			// parse workspace configuration
			Workspace workspace = new Workspace(ws);
			return workspace;
		}
		return null;
	}

	protected static ArrayList<Path> qry_getAllPaths(HFClient client, String robotId)
			throws InvalidArgumentException, ProposalException {

		ArrayList<Path> paths = new ArrayList<Path>();

		// get channel instance from client
		Channel channel = client.getChannel(CHANNEL_NAME);
		// create chaincode request
		QueryByChaincodeRequest query = client.newQueryProposalRequest();
		// build cc id providing the chaincode name. Version is omitted here.
		ChaincodeID multiRobotCCId = ChaincodeID.newBuilder().setName(CHAINCODE_NAME).build();
		query.setChaincodeID(multiRobotCCId);
		// CC function to be called
		query.setFcn("getAllPaths");
		ArrayList<String> args = new ArrayList<String>();
		args.add(robotId);
		query.setArgs(args);

		// query blockchain
		Collection<ProposalResponse> response = channel.queryByChaincode(query, channel.getPeers());
		// display response
		for (ProposalResponse pRsp : response) {

			String stringResponse = new String(pRsp.getChaincodeActionResponsePayload());
			JsonReader jsonReader = Json.createReader(new StringReader(stringResponse));
			JsonArray jsonPaths = jsonReader.readArray();
			jsonReader.close();

			int i, j;
			for (i = 0; i < jsonPaths.size(); i++) {

				String pathOwnerId = jsonPaths.getJsonObject(i).getString("robotId");

				double x, y;
				// Collect points in path
				JsonArray jsonPoints = jsonPaths.getJsonObject(i).getJsonArray("points");
				ArrayList<Position> points = new ArrayList<Position>();

				for (j = 0; j < jsonPoints.size(); j++) {

					x = jsonPoints.getJsonObject(j).getJsonNumber("x").doubleValue();
					y = jsonPoints.getJsonObject(j).getJsonNumber("y").doubleValue();
					points.add(new Position(x, y));
				}

				Path path = new Path(points, pathOwnerId);
				log.debug("Retrieved path(" + i + ") from blockchain");
				log.debug(path.toJSONString());

				paths.add(path);
			}
		}
		return paths;
	}

/*
	protected static Path qry_getMyPath(HFClient client, String robotId)
			throws InvalidArgumentException, ProposalException {

		// get channel instance from client
		Channel channel = client.getChannel(CHANNEL_NAME);
		// create chaincode request
		QueryByChaincodeRequest query = client.newQueryProposalRequest();
		// build cc id providing the chaincode name. Version is omitted here.
		ChaincodeID multiRobotCCId = ChaincodeID.newBuilder().setName(CHAINCODE_NAME).build();
		query.setChaincodeID(multiRobotCCId);
		// CC function to be called
		query.setFcn("getMyPath");
		ArrayList<String> args = new ArrayList<String>();
		args.add(robotId);
		query.setArgs(args);

		// query blockchain
		Collection<ProposalResponse> response = channel.queryByChaincode(query, channel.getPeers());
		// display response
		for (ProposalResponse pRsp : response) {

			String pathJSON = new String(pRsp.getChaincodeActionResponsePayload());
			Path myPath = new Path(pathJSON, robotId);
			log.info("Retrieved my path from blockchain");
			log.debug(myPath.toJSONString());
			return myPath;
		}
		return null;
	}
*/
	protected static void invk_setMyPath(HFClient client, String robotId, Path path)
			throws ProposalException, InvalidArgumentException, UnsupportedEncodingException,
			InterruptedException, ExecutionException, TimeoutException {

		// List of signed proposal responses from peers
		Collection<ProposalResponse> successful = new LinkedList<>();
		// get channel instance from client
		Channel channel = client.getChannel(CHANNEL_NAME);
		// create chaincode request
		TransactionProposalRequest request = client.newTransactionProposalRequest();
		// build cc id providing the chaincode name. Version is omitted here.
		ChaincodeID multiRobotCCId = ChaincodeID.newBuilder().setName(CHAINCODE_NAME).build();
		request.setChaincodeID(multiRobotCCId);
		// CC function to be called
		request.setFcn("setMyPath");
		ArrayList<String> args = new ArrayList<String>();
		args.add(robotId);
		args.add(path.toJSONString());
		request.setArgs(args);
		//request.setProposalWaitTime(1000);

		// send transaction proposal
		Collection<ProposalResponse> transactionPropResp = channel.sendTransactionProposal(request, channel.getPeers());
		for (ProposalResponse response : transactionPropResp) {
			if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
				log.info(format("Successful transaction proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName()));
				successful.add(response);
			}
		}

		// Check that all the proposals are consistent with each other. We should have only one set
		// where all the proposals above are consistent.
		Collection<Set<ProposalResponse>> proposalConsistencySets = SDKUtils.getProposalConsistencySets(transactionPropResp);
		if (proposalConsistencySets.size() != 1) {
			log.error("Expected only one set of consistent proposal responses but got "+ proposalConsistencySets.size());
			return;
		}

		// Send Transaction to orderer
		channel.sendTransaction(successful);
	}

	// user serialization and deserialization utility functions
	// files are stored in the base directory
/*
	*//**
	 * Serialize RUser object to file
	 *
	 * @param robotUser The object to be serialized
	 * @throws IOException
	 *//*
	private static void serialize(RUser robotUser)
			throws IOException {
		try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(
				Paths.get(robotUser.getName() + ".jso")))) {
			oos.writeObject(robotUser);
		}
	}

	*//**
	 * Deserialize RUser object from file
	 *
	 * @param name The name of the user. Used to build file name ${name}.jso
	 * @return
	 * @throws Exception
	 *//*
	private static RUser tryDeserialize(String name)
			throws Exception {
		if (Files.exists(Paths.get(name + ".jso"))) {
			return deserialize(name);
		}
		return null;
	}

	private static RUser deserialize(String name)
			throws Exception {
		try (ObjectInputStream decoder = new ObjectInputStream(
				Files.newInputStream(Paths.get(name + ".jso")))) {
			return (RUser) decoder.readObject();
		}
	}
*/
}
