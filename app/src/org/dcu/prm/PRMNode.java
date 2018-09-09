/*
 * Copyright (c) 2004 Patric Jensfelt.
 * All Rights Reserved.
 */

package org.dcu.prm;

import java.util.LinkedList;
import java.util.ListIterator;

public class PRMNode extends AStarNode {

	/// List of edges
	protected LinkedList<PRMNode> m_Edges;

	public PRMNode() {
		m_Edges = new LinkedList<PRMNode>();
	}

	/**
	 * Call this function to add a new edge to a certain node
	 *
	 * @param node pointer to node to add edge to
	 */
	public void addNewEdge(PRMNode node) {
		m_Edges.offerLast(node);
	}

	/**
	 * Call this function to remove an edge (if it exists) to a certain node
	 *
	 * @param node pointer to node to delete the edge to
	 */
	public void deleteEdge(PRMNode node) {

		ListIterator<PRMNode> i = m_Edges.listIterator();

		while(i.hasNext()) {
			PRMNode e = i.next();
			if (node.equals(e)) {
				// Remove current node => previousIndex
				m_Edges.remove(i.previousIndex());
			}
		}
	}

	/**
	 * This function should return a list of nodes connected to this one
	 * and the cost to move there
	 *
	 * @param l list of connections to neighboring nodes and their costs
	 * @return true always
	 */
	protected LinkedList<Connection> getConnections() {

		LinkedList<Connection> connections = new LinkedList<Connection>();
		ListIterator<PRMNode> i = m_Edges.listIterator();

		while(i.hasNext()) {
			PRMNode e = i.next();

			double dx = this.getX() - e.getX();
			double dy = this.getY() - e.getY();
			double d = Math.sqrt(dx*dx + dy*dy);

			connections.offerLast(new Connection(e,d));
		}
		return connections;
	}

	/**
	 * Estimate the cost to get to a certain node. This is the
	 * heuristics function. The better this function is the faster the
	 * search will be.
	 */
	@Override
	protected double guessCostTo(AStarNode n) {
		double dx = n.getX() - getX();
		double dy = n.getY() - getY();
		return Math.sqrt(dx*dx+dy*dy);
	}
}