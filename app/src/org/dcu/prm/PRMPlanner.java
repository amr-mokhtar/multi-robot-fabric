/*
 * Copyright (c) 2004 Patric Jensfelt.
 * All Rights Reserved.
 */

package org.dcu.prm;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

class Edge {
	int a;
	int b;

	public Edge(int a, int b) {
		this.a = a;
		this.b = b;
	}
}

class Distance {
	double d;
	int i;

	public Distance(double d, int i) {
		this.d = d;
		this.i = i;
	}
}

/**
 * This class implements a basic version of the probabilistic road map
 * (PRM) method for path planning.
 */
public class PRMPlanner {

	private static final int RAND_MAX = 0x7FFF;
	/**
	 * Vector with all randomly generated nodes generated that do not
	 * collide with any obstacles
	 */
	protected LinkedList<PRMNode> m_Nodes;

	/**
	 * Vector with pairs of indices coding the edges (only for debugging)
	 */
	protected LinkedList<Edge> m_Edges;

	/**
	 * Store the step size used when building the road map to use when
	 * looking for the closest node in findPath
	 */
	protected double m_Step;

	public PRMPlanner() {
		// Make sure that the random generator has been initialize
		//int time = (int) (new Date().getTime()/1000);
		//srand(time);
		m_Nodes = new LinkedList<PRMNode>();
		m_Edges = new LinkedList<Edge>();
	}

	/**
	 * This function builds a structure with the distances to all the
	 * other nodes. It can be used to try to connect nodes with edges.
	 *
	 * The list will contain pairs of node index in m_Nodes and the
	 * corresponding distances. Notice that the distance to the noe
	 * given in the argument will also be in there.
	 *
	 * @param n node to calculate distances to
	 * @param distances the list with
	 */
	private List<Distance> getNodeDistances(PRMNode n) {

		List<Distance> distances = new ArrayList<Distance>();

		for (int i = 0; i < m_Nodes.size(); i++) {

			double d = Math.sqrt(
					(m_Nodes.get(i).getX() - n.getX()) *
					(m_Nodes.get(i).getX() - n.getX()) +
					(m_Nodes.get(i).getY() - n.getY()) *
					(m_Nodes.get(i).getY() - n.getY()) );

			distances.add(new Distance(d, i) );
		}

		// Sort the list with distances so that the closest one comes first
		distances.sort(new Comparator<Distance>() {
			public int compare(Distance D1, Distance D2) {
				return (D1.d > D2.d) ? 1 : -1;
			}
		});

		//System.out.print("Distances: [");
		//for (int i = 0; i < distances.size(); i++) {
		//	System.out.printf("%.2f, ", distances.get(i).d);
		//}
		//System.out.println("]");
		return distances;
	}

	/**
	 * Use this function to check if a line (xS,yS)->(xE,yE) collides
	 * with any of the obstacles in the workspace. It does this by sampling
	 * the line with a certain step size specified as argument to the function.
	 *
	 * @param workspace a reference to the workspace model
	 * @param xS x-coordinate of the start point for the edge
	 * @param yS y-coordinate of the start point for the edge
	 * @param xE x-coordinate of the end point for the edge
	 * @param yE y-coordinate of the end point for the edge
	 * @param step the step size to use when checking for collisions
	 * @return true if the line (xS,yS)->(xE,xE) does not collide with any of the obstacles
	 */
	protected boolean isCollisionFreePath(Workspace workspace,
			double xS, double yS, double xE, double yE,
			double step) {

		double dx = xE - xS;
		double dy = yE - yS;

		// The angle of the line
		double ang = Math.atan2(dy, dx);

		// The length of the line
		double len = Math.sqrt(dx*dx + dy*dy);

		// The direction cosines
		double kx = Math.cos(ang);
		double ky = Math.sin(ang);

		boolean last = false;
		boolean done = false;
		double pos = 0; // where on the line are we

		while (!done) {

			double x = xS + pos * kx;
			double y = yS + pos * ky;

			if (workspace.collidesWith(x,y))
				return false;

			if (!last && pos > len) {
				last = true;
				pos = len;
			} else if (last) {
				done = true;
			}

			pos += step;
		}

		// check if collides with another robot's path in the workspace
		return !(workspace.collidesWithPath(xS, yS, xE, yE));
	}

	/**
	 * Use this function to find list of nodes on the graph closest
	 * (collision free straight line) to a certain point (x,y).
	 *
	 * @param workspace reference to the workspace holding the obstacles
	 * @param x x-coordinate of the point to connect to graph
	 * @param y y-coordinate of the point to connect to graph
	 * @return index of closest node if found, -1 if not
	 */
	private List<Distance> findClosestNodesOnGraph(Workspace workspace, double x, double y)
	{
		List<Distance> distances = new ArrayList<Distance>();

		for (int i = 0; i < m_Nodes.size(); i++) {

			double dx = x - m_Nodes.get(i).getX();
			double dy = y - m_Nodes.get(i).getY();
			double d = Math.sqrt(dx*dx+dy*dy);

			if (isCollisionFreePath(workspace,
					m_Nodes.get(i).getX(), m_Nodes.get(i).getY(),
					x, y, m_Step)) {
				distances.add(new Distance(d, i));
			}
		}

		// Sort the list with distances so that the closest one comes first
		distances.sort(new Comparator<Distance>() {
			public int compare(Distance D1, Distance D2) {
				return (D1.d > D2.d) ? 1 : -1;
			}
		});

		return distances;
	}

	/**
	 * This function creates a new node with random position
	 *
	 * @param xMin min value for x-coordinate to generate nodes for
	 * @param xMax max value for x-coordinate to generate nodes for
	 * @param yMin min value for y-coordinate to generate nodes for
	 * @param yMax max value for y-coordinate to generate nodes for
	 * @return pointer to new node
	 */
	protected PRMNode generateNodeWithRandomPosition(double xMin, double xMax,
			double yMin, double yMax)
	{
		PRMNode n = new PRMNode();

		n.setX(xMin + ((xMax - xMin) * ((int)(Math.random() * RAND_MAX) / (RAND_MAX + 1.0))));
		n.setY(yMin + ((yMax - yMin) * ((int)(Math.random() * RAND_MAX) / (RAND_MAX + 1.0))));

		return n;
	}

	/**
	 * Calculates the path from a start to an end position. It is assume
	 * that you have already called buildRoadMap. The workspace passed as an
	 * argument to this function is only used to make sure that the
	 * specified start and goal positions can be connected correctly to
	 * the road map.
	 *
	 * @param workspace reference to the workspace holding the obstacles
	 * @param xStart x-coordinate for the start position
	 * @param yStart y-coordinate for the start position
	 * @param xEnd x-coordinate for the end position
	 * @param yEnd y-coordinate for the end position
	 * @return path reference to the list that will contain the
	 * resulting path (if found), null otherwise
	 *
	 * @see buildRoadMap
	 */
	public Path findPath(Workspace workspace, int maxAttempts) {

		double xStart = workspace.xStart;
		double yStart = workspace.yStart;
		double xGoal = workspace.xGoal;
		double yGoal = workspace.yGoal;

		Path path = new Path();
		// clear path buffer
		path.points.clear();

		// Find list of nodes in the graph that are closest to the start and goal
		// point and offer a collision free path from these points to the
		// road map.
		List<Distance> startClosestNodeDistances = findClosestNodesOnGraph(workspace, xStart, yStart);
		List<Distance> goalClosestNodeDistances = findClosestNodesOnGraph(workspace, xGoal, yGoal);

		int attempt = 0;
		while ((attempt < maxAttempts) &&
				(attempt < startClosestNodeDistances.size()) &&
				(attempt < goalClosestNodeDistances.size())) {

			int start = startClosestNodeDistances.get(attempt).i;
			int goal = goalClosestNodeDistances.get(attempt).i;

			if (m_Nodes.get(goal).findPath(m_Nodes.get(start))) {

				// add start point to path
				path.points.add(new Position(xStart, yStart));

				PRMNode n = m_Nodes.get(start);
				while (n != null) {
					// add successful point to path
					path.points.add(new Position( n.getX(), n.getY() ) );
					n = (PRMNode)n.parent;
				}
				// add goal point to path
				path.points.add(new Position(xGoal, yGoal));
				return path;
			}

			// get another starting point from constructed conflict-free roadmap and retry
			System.out.printf("[Attempt# %d] Failed to find path.. retrying..!\n", attempt);
			attempt++;
		}

		return null;
	}

	/**
	 * Call this function to build up the road map that allows you to
	 * plan paths through the given workspace.
	 *
	 * @param workspace reference to the workspace holding the obstacles
	 * @param xMin min value for x-coordinate to generate nodes for
	 * @param xMax max value for x-coordinate to generate nodes for
	 * @param yMin min value for y-coordinate to generate nodes for
	 * @param yMax max value for y-coordinate to generate nodes for
	 * @param nNodes number of nodes to use for the road map
	 * @param K number of nearest nodes to try to connect a new node to
	 * @param step step size when shecking for collisions along edges
	 *
	 * Note1: This is a probabilistic method and the particular road map
	 * generated with this call might not provide a solution for a given
	 * problem so you might have to generate a new one in some cases.
	 *
	 * Note2: If the workspace changes you need to update the road-map. The
	 * brute force way to do so is to call this function again and
	 * generate a new road map from scratch.
	 */
	public void buildRoadMap(Workspace workspace,
			int nNodes, int K, double step) {

		double xMin = workspace.xMin;
		double xMax = workspace.xMax;
		double yMin = workspace.yMin;
		double yMax = workspace.yMax;

		// Store step size so that the same value can be used in findPath
		m_Step = step;

		// Clear the list of nodes and delete the node objects
		m_Nodes.clear();

		// Clear the vector with edges (used for debug display stuff)
		m_Edges.clear();

		// Create new set of n nodes with random positions. Each new node is
		// first checked for collisions before being added
		int n = 0;
		while (m_Nodes.size() < nNodes) {

			PRMNode node = generateNodeWithRandomPosition(xMin, xMax, yMin, yMax);

			// Check if we can add it, is it does not collide with obstacles
			if (workspace.collidesWith(node.getX(), node.getY())) {
				// Go back up and create a new one
				continue;
			}

			m_Nodes.offerLast(node);
			n++;
		}

		for (n = 0; n < m_Nodes.size(); n++) {
			// get node
			PRMNode node = m_Nodes.get(n);

			// Try to connect this node to existing nodes in the neighborhod
			// of this node

			// Get list of distances to other nodes
			List<Distance> distances = getNodeDistances(node);

			int k = 0;
			ListIterator<Distance> i = distances.listIterator();

			while(i.hasNext()) {
				Distance dist = i.next();

				// Skip the nodes if it is the same as we try to connect to
				if (dist.i == n) {
					continue;
				}

				// Check if the path between the nodes is free from collisions
				if (isCollisionFreePath(workspace, node.getX(), node.getY(),
						m_Nodes.get(dist.i).getX(), m_Nodes.get(dist.i).getY(), step)) {
					// link nodes together
					m_Nodes.get(dist.i).addNewEdge(node);
					node.addNewEdge(m_Nodes.get(dist.i));
					m_Edges.offerLast(new Edge(n, dist.i));
				}
				// If we have tested enough of the neighbors we break here
				k++;
				if (k >= K) break;
			}
		}
	}

	/**
	 * Use this function to generate matlab code to display the nodes and edges.
	 *
	 * @param s reference to stream to output matlab code to
	 * @param dispEdges flag telling if we should generate display code for edges
	 * @param dispNodes flag telling if we should generate display code for nodes
	 * @return N/A
	 */
	public void writeMatlabDisplayCode(String mfile, boolean dispEdges, boolean dispNodes)
			throws IOException {

		FileWriter fw = new FileWriter(mfile, true);

		fw.write("hold on\n");

		if (dispNodes) {
			for (int i = 0; i < m_Nodes.size(); i++) {
				fw.write("plot(" + m_Nodes.get(i).getX() + "," + m_Nodes.get(i).getY() + ",\'.m\')\n");
			}
		}

		// We always write the code for displaying the edges but in case we
		// are ordered not to displayit we add a "if 0 ... end" around it
		// that can be removed manually
		fw.write("if " + (dispEdges? 1 : 0) + "\n");

		ListIterator<Edge> i = m_Edges.listIterator();

		while(i.hasNext()) {
			Edge e = i.next();

			fw.write("  plot([" +
					m_Nodes.get(e.a).getX() + "," +
					m_Nodes.get(e.b).getX() + "] ,[" +
					m_Nodes.get(e.a).getY() + ", " +
					m_Nodes.get(e.b).getY() + "], \'g\')\n");
		}
		fw.write("end\n");
		fw.write("hold off\n");
		fw.close();
	}
}
