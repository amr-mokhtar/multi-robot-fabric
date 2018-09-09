/*
 * Copyright (c) 2018 Amr Mokhtar.
 * All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.dcu;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.xml.DOMConfigurator;

public class MultiRobotTest extends Thread {

	private static List<Robot> robots = new ArrayList<Robot>();
	private int numOfRobots;

	public MultiRobotTest(int numOfRobots) {
		this.numOfRobots = numOfRobots;
	}

	public static void main(String[] args) {

		if (args.length < 1) {
			System.err.println("Number of robots is missing");
			return;
		}

		System.out.println("-- SYNCHRONIZED MULTI-ROBOT FABRIC TEST --");
		DOMConfigurator.configure("log4j.xml");

		MultiRobotTest app = new MultiRobotTest(Integer.parseInt(args[0]));
		app.start();
	}

	@Override
	public void run() {
		super.run();

		try {
			// start with Grapher
			Grapher g = new Grapher(numOfRobots);
			g.go();

		} catch (Exception e1) {
			e1.printStackTrace();
		}

		Robot robot;
		for (int i = 0; i < numOfRobots; i++) {
			try {
				System.out.println(String.format("[robot%02d] -> go", i));
				robot = new Robot(String.format("robot%02d", i));
				robots.add(robot);
				robot.go();

				synchronized (robot.finished) {
					// wait for robot to finish path planning
					robot.finished.wait();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
