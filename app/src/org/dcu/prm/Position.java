/*
 * Copyright (c) 2004 Patric Jensfelt.
 * All Rights Reserved.
 */

package org.dcu.prm;

public class Position {

	protected double m_X;
	protected double m_Y;

	public Position() {
		this.m_X = 0;
		this.m_Y = 0;
	}

	public Position(double x, double y) {
		this.m_X = x;
		this.m_Y = y;
	}

	public void setX(double x) {
		m_X = x;
	}

	public double getX() {
		return m_X;
	}

	public void setY(double y) {
		m_Y = y;
	}

	public double getY() {
		return m_Y;
	}

	@Override
	public String toString() {
		return "<"+m_X+","+m_Y+">";
	}
}
