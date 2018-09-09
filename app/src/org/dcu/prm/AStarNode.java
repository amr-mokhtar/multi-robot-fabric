/*
 * Copyright (c) 2004 Patric Jensfelt.
 * All Rights Reserved.
 */

package org.dcu.prm;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

class Connection {
	AStarNode node;
	double cost;

	public Connection(AStarNode node, double cost) {
		this.node = node;
		this.cost = cost;
	}
}

/**
 * Base class for nodes in agraph that you want to be able to use for
 * A* search. You need to implement a function getConnections that
 * will return a list of connnections to other nodes and the cost to
 * go there but then you should be able to use A*.
 *
 * @author Patric Jensfelt
 * @see
 */
public abstract class AStarNode extends Position {

	/** Temporary variable used when finding a path */
	protected double costG;

	/** Temporary variable used when finding a path */
	protected double costH;

	/** Points to the next node to step to when a path has been found */
	public AStarNode parent;

	/**
	 * Estimate the cost to get to a certain node. This is the
	 * heuristics function. The better this function is the faster the
	 * search will be.
	 */
	protected abstract double guessCostTo(AStarNode n);

	protected abstract LinkedList<Connection> getConnections();

	public AStarNode() {
		super();
		costG = 0;
		costH = 0;
		parent = null;
	}

	/**
	 * Find a path from the current node to another target node
	 * @throws Exception
	 */
	public boolean findPath(AStarNode target) {

		if (this.equals(target)) {
			this.parent = null;
			return true;
		}

		LinkedList<AStarNode> open = new LinkedList<AStarNode>();
		LinkedList<AStarNode> closed = new LinkedList<AStarNode>();

		addPathElement(this, 0, null, target, open, closed);

		boolean found = false;
		while (!found && !open.isEmpty()) {

			// Move the first item from the open list and move it to the list
			// of closed nodes
			AStarNode node = open.getFirst();
			open.pollFirst();
			closed.offerFirst(node);

			// Check if we have reached the target node
			if (node.equals(target)) {
				found = true;
			} else {
				// Add all the nodes connected to this node
				LinkedList<Connection> conns = node.getConnections();

				Iterator<Connection> e = conns.iterator();
				Connection conn;
				while(e.hasNext()) {
					conn = e.next();
					double costF = node.costG + conn.cost;
					addPathElement(conn.node, costF, node, target, open, closed);
				}
			}

		}

		return found;
	}

	protected void addToOpenList(LinkedList<AStarNode> open, AStarNode n) {

		ListIterator<AStarNode> i = open.listIterator();

		while(i.hasNext()) {
			AStarNode e = i.next();
			if (n.costG + n.costH < e.costG + e.costH) {
				// Add current node => previousIndex
				open.add(i.previousIndex(), n);
				return;
			}
		}
		open.offerLast(n);
	}

	protected void addPathElement(AStarNode n, double cost,
			AStarNode parent, AStarNode target,
			LinkedList<AStarNode> open,
			LinkedList<AStarNode> closed) {

		ListIterator<AStarNode> i;

		// Check if the node is already in the open or closed list and if
		// the cost there is lower. If this is the case there is no point in
		// going on because a path already exist to this node with lower
		// cost.
		i = open.listIterator();

		while(i.hasNext()) {
			AStarNode e = i.next();
			if (n.equals(e)) {
				if (e.costG > cost) {
					// The cost is lower and we could find a better path
					// Remove current node => previousIndex
					open.remove(i.previousIndex());
					break;
				} else {
					// Found a lower cost no point in going on
					return;
				}
			}
		}

		i = closed.listIterator();

		while(i.hasNext()) {
			AStarNode e = i.next();
			if (n.equals(e)) {
				if (e.costG > cost) {
					// The cost is lower and we could find a better path
					closed.remove(i.previousIndex());
					break;
				} else {
					// Found a lower cost no point in going on
					return;
				}
			}
		}

		// Create new node or use found one from list
		n.parent = parent;              // Parent node
		n.costG  = cost;                // cost from start
		n.costH  = guessCostTo(target); // Guessed cost to target

		addToOpenList(open,n);
	}
}
