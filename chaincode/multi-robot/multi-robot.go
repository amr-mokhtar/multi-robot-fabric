/*
 * Copyright (c) 2018 Amr Mokhtar.
 * All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)

// Invokations
const (
	INVK_GET_WORKSPACE = "getWorkspace"
	INVK_GET_ALL_PATHS = "getAllPaths"
	INVK_SET_MY_PATH   = "setMyPath"
)

// Database keys & indexes
const (
	K_WORKSPACE = "workspace"
	K_ROBOTPATH  = "allpaths~robot"
	K_ALLPATHS  = "allpaths"
)

// events
const (
	E_PATH_COMMITTED = "path-committed"
)

// MultiRobotChaincode Smart Contract structure
type MultiRobotChaincode struct {
}

type Workspace struct {
	Start Point `json:"start"`
	Goal Point `json:"goal"`
	Bounds Boundary `json:"bounds"`
	Circles []Circle `json:"circles"`
	Rectangles []Rectangle `json:"rectangles"`
}

type Point struct {
	X float32 `json:"x"`
	Y float32 `json:"y"`
}

type Boundary struct {
	xMin float32 `json:"xMin"`
	xMax float32 `json:"xMax"`
	yMin float32 `json:"yMin"`
	yMax float32 `json:"yMax"`
}

type Circle struct {
	XCenter float32 `json:"xCenter"`
	YCenter float32 `json:"yCenter"`
	Radius float32 `json:"radius"`
}

type Rectangle struct {
	XCenter float32 `json:"xCenter"`
	YCenter float32 `json:"yCenter"`
	Width float32 `json:"width"`
	Height float32 `json:"height"`
}

// Path Table store in DB
type Path struct {
	RobotId string  `json:"robotId"` // use as primary key - index
	Points []Point `json:"points"`
}

var logger = shim.NewLogger("multi-robot")

// =====
// Main
// =====
func main() {
	err := shim.Start(new(MultiRobotChaincode))
	if err != nil {
		fmt.Printf("Error starting MULTI-ROBOT chaincode: %s", err)
	}
}

// ===========================
// Init initializes chaincode
// ===========================
func (t *MultiRobotChaincode) Init(stub shim.ChaincodeStubInterface) pb.Response {
	// set logging level
	logger.SetLevel(shim.LogInfo)

	// === Save workspace to state ===
	err := stub.PutState(K_WORKSPACE, []byte(workspaceDef))
	if err != nil {
		return shim.Error(err.Error())
	}

	logger.Info("MULTI-ROBOT chaincode instantiated successfully!")
	
	var workspace Workspace
	err = json.Unmarshal([]byte(workspaceDef), &workspace)
	logger.Debug("Workspace.Start: ", workspace.Start)
	logger.Debug("Workspace.Goal: ", workspace.Goal)
	logger.Debug("Workspace.Bounds: ", workspace.Bounds)
	logger.Debug("Workspace.Circles: ", workspace.Circles)
	logger.Debug("Workspace.Rectangles: ", workspace.Rectangles)
	
	return shim.Success(nil)
}

// ========================================
// Invoke - Our entry point for Invocations
// ========================================
func (t *MultiRobotChaincode) Invoke(stub shim.ChaincodeStubInterface) pb.Response {
	function, args := stub.GetFunctionAndParameters()

	// Handle different functions
	if function == INVK_GET_WORKSPACE {
		return t.getWorkspace(stub, args)
	} else if function == INVK_GET_ALL_PATHS {
		return t.getAllPaths(stub, args)
	} else if function == INVK_SET_MY_PATH {
		return t.setMyPath(stub, args)
	}

	logger.Info("Invoke did not find func: " + function) //error
	return shim.Error("Received unknown function invocation")
}


// ======================================
// ==== Invocation Functions Section ====
// ======================================

func (t *MultiRobotChaincode) getWorkspace(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	var jsonResp string
	var err error

	if len(args) != 0 {
		jsonResp = "{\"Error\": \"Expecting no arguments for " + INVK_GET_WORKSPACE + "\"}"
		return shim.Error(jsonResp)
	}

	// read workspace from database
	bytesWorkspace, err := stub.GetState(K_WORKSPACE)
	if err != nil {
		jsonResp = "{\"Error\": \"Failed to get state for " + K_WORKSPACE + "\"}"
		return shim.Error(jsonResp)
	} else if bytesWorkspace == nil {
		jsonResp = "{\"Error\": \"Workspace does not exist!\"}"
		return shim.Error(jsonResp)
	}

	return shim.Success(bytesWorkspace)
}

func (t *MultiRobotChaincode) getAllPaths(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	var jsonResp string

	if len(args) != 1 {
		jsonResp = "{\"Error\": \"Expecting Id argument for " + INVK_GET_ALL_PATHS + "\"}"
		return shim.Error(jsonResp)
	}

	// Query the allpaths~robotId index by color
	resultsIterator, err := stub.GetStateByPartialCompositeKey(K_ROBOTPATH, []string{K_ALLPATHS})
	if err != nil {
		return shim.Error(err.Error())
	}
	defer resultsIterator.Close()

	// buffer is a JSON array containing QueryResults
	var bytesResults bytes.Buffer
	bytesResults.WriteString("[")

	// Iterate through result set and for each marble found, transfer to newOwner
	var i int
	for i = 0; resultsIterator.HasNext(); i++ {
		// Note that we don't get the value (2nd return variable), we'll just get the marble name from the composite key
		responseRange, err := resultsIterator.Next()
		if err != nil {
			return shim.Error(err.Error())
		}

		// get allpaths and robotId from allpaths~robotId composite key
		_, compositeKeyParts, err := stub.SplitCompositeKey(responseRange.Key)

		if err != nil {
			return shim.Error(err.Error())
		}
		
		pathRobotId := compositeKeyParts[1]
		
		// exclude this robot's from returned paths
		if pathRobotId == args[0] {
			continue;
		}

		bytesPath, err := stub.GetState(pathRobotId)
		if err != nil {
			return shim.Error(err.Error())
		} else if bytesPath == nil {
			return shim.Error(err.Error())
		}

		bytesResults.Write(bytesPath)
		bytesResults.WriteString(",")
	}

	// check if the result list is bigger than just two SQUARECLOSE '[]'
	if bytesResults.Len() > 2 {
		// revert back the last appended comma ','
		bytesResults.Truncate(bytesResults.Len() - 1)
	}
	bytesResults.WriteString("]")

	logger.Info("Found" , i , "paths!")
	logger.Debug("All paths: ", bytesResults.String())
	return shim.Success(bytesResults.Bytes())
}

func (t *MultiRobotChaincode) setMyPath(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	var jsonResp string
	var err error
	var path Path

	if len(args) != 2 {
		jsonResp = "{\"Error\": \"Expecting 2 argument for " + INVK_SET_MY_PATH + "\"}"
		return shim.Error(jsonResp)
	}

	// Get Robot Id claiming this path
	robotId := args[0];

	// ==== Check if this robot already has a registered path with us ====
	bytesPath, err := stub.GetState(robotId)
	if err != nil {
		return shim.Error(err.Error())
	}

	if bytesPath == nil {

		path.RobotId = robotId
		err = json.Unmarshal([]byte(args[1]), &path.Points)
		if err != nil {
			jsonResp = "{\"Error\": \"Failed to unmarshal given path: " + args[1] + "\"}"
			return shim.Error(jsonResp)
		}

		// === save path to state ===
		bytesPath, _ = json.Marshal(path)
		err = stub.PutState(robotId, bytesPath)
		if err != nil {
			return shim.Error(err.Error())
		}
		logger.Info("Robot[" + robotId + "] first time set path")
		logger.Debug(path.Points)

		//  ==== Index all paths and their robot id using allpaths~robotId composite key to enable querying for all paths ====
		pathsIndexKey, err := stub.CreateCompositeKey(K_ROBOTPATH, []string{K_ALLPATHS, robotId})
		if err != nil {
			return shim.Error(err.Error())
		}

		//  Save index entry to state. Only the key name is needed, no need to store a duplicate value
		value := []byte{0x00}
		stub.PutState(pathsIndexKey, value)

	} else {

		err = json.Unmarshal(bytesPath, &path)
		if err != nil {
			return shim.Error(err.Error())
		}

		err = json.Unmarshal([]byte(args[1]), &path.Points)
		if err != nil {
			jsonResp = "{\"Error\": \"Failed to unmarshal given path: " + args[1] + "\"}"
			return shim.Error(jsonResp)
		}

		// === update path to state ===
		bytesPath, _ = json.Marshal(path)
		err = stub.PutState(robotId, bytesPath)
		if err != nil {
			return shim.Error(err.Error())
		}
		
		logger.Info("Robot[" + robotId + "] updated path")
		logger.Debug(path.Points)
	}

	err = stub.SetEvent(E_PATH_COMMITTED, bytesPath)
	logger.Info("Event[" + E_PATH_COMMITTED + "] set from Robot[" + robotId + "]")
	if err != nil {
		return shim.Error(err.Error())
	}

	return shim.Success(nil)
}
