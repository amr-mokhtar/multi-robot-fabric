/*
 * Copyright (c) 2018 Amr Mokhtar.
 * All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.dcu.prm;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class Path {
	public String id;
	public ArrayList<Position> points;

	public Path() {
		this.points = new ArrayList<Position>();
	}

	public Path(ArrayList<Position> points, String id) {
		this.id = id;
		this.points = points;
	}

	public Path(String pathJSON) {

		JsonReader jsonReader = Json.createReader(new StringReader(pathJSON));
		JsonObject jsonPath = jsonReader.readObject();
		jsonReader.close();

		this.id = jsonPath.getString("robotId");
		this.points = new ArrayList<Position>();

		double x, y;
		JsonArray jsonPoints = jsonPath.getJsonArray("points");
		for (int i = 0; i < jsonPoints.size(); i++) {
			x = jsonPoints.getJsonObject(i).getJsonNumber("x").doubleValue();
			y = jsonPoints.getJsonObject(i).getJsonNumber("y").doubleValue();
			this.points.add(new Position(x, y));
		}
	}

	// return true if intersects
	public boolean intersectWith(double x1, double y1, double x2, double y2) {

		double x1p, y1p, x2p, y2p;
		for (int i = 0; i < this.points.size()-1; i++) {
			x1p = this.points.get(i).getX();
			y1p = this.points.get(i).getY();
			x2p = this.points.get(i+1).getX();
			y2p = this.points.get(i+1).getY();

			if (isLineIntersection(x1, y1, x2, y2, x1p, y1p, x2p, y2p)) {
				return true;
			}
		}
		// If none detected, all clear
		return false;
	}

	// return true if this path collides with that
	public boolean collidesWith(Path that) {

		double xB1, yB1, xB2, yB2;
		double xA1, yA1, xA2, yA2;

		for (int i = 0; i < this.points.size()-1; i++) {
			xB1 = this.points.get(i).getX();
			yB1 = this.points.get(i).getY();
			xB2 = this.points.get(i+1).getX();
			yB2 = this.points.get(i+1).getY();

			for (int j = 0; j < that.points.size()-1; j++) {
				xA1 = that.points.get(j).getX();
				yA1 = that.points.get(j).getY();
				xA2 = that.points.get(j+1).getX();
				yA2 = that.points.get(j+1).getY();

				if (isLineIntersection(xB1, yB1, xB2, yB2, xA1, yA1, xA2, yA2)) {
					return true;
				}
			}
		}

		// If none detected, all clear
		return false;
	}

	public List<Distance> getDistances(double x, double y) {

		List<Distance> distances = new ArrayList<Distance>();

		for (int i = 0; i < points.size(); i++) {

			double d = Math.sqrt(
					(points.get(i).getX() - x) *
					(points.get(i).getX() - x) +
					(points.get(i).getY() - y) *
					(points.get(i).getY() - y) );

			distances.add(new Distance(d, i));
		}

		// Sort the list with distances so that the closest one comes first
		distances.sort(new Comparator<Distance>() {
			public int compare(Distance D1, Distance D2) {
				return (D1.d > D2.d) ? 1 : -1;
			}
		});

		return distances;
	}

	public String toJSONString() {

		JsonArrayBuilder builder = Json.createArrayBuilder();
		for (Position point : points) {
			builder.add(Json.createObjectBuilder()
					.add("x", point.getX())
					.add("y", point.getY()));
		}
		JsonArray jsonPath = builder.build();
		return jsonPath.toString();
	}

	// Returns true if the lines intersect, otherwise false.
	private boolean isLineIntersection(double xB1, double yB1, double xB2, double yB2,
			double xA1, double yA1, double xA2, double yA2)
	{
		double dxB, dyB, dxA, dyA;
		dxB = xB2 - xB1;
		dyB = yB2 - yB1;
		dxA = xA2 - xA1;
		dyA = yA2 - yA1;

		double s, t;
		s = (-dyB * (xB1 - xA1) + dxB * (yB1 - yA1)) / (-dxA * dyB + dxB * dyA);
		t = ( dxA * (yB1 - yA1) - dyA * (xB1 - xA1)) / (-dxA * dyB + dxB * dyA);

		if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
			// Collision detected
			double xI = xB1 + (t * dxB);
			double yI = yB1 + (t * dyB);

			// check if intersection belongs to line segment
			if ((Math.min(xB1,xB2) < xI) &&
				(xI < Math.max(xB1,xB2)) &&
				(Math.min(yB1,yB2) < yI) &&
				(yI < Math.max(yB1,yB2)))
				return true;
		}

		return false; // No collision
	}

	private boolean isLineIntersection2(double p0_x, double p0_y, double p1_x, double p1_y,
			double p2_x, double p2_y, double p3_x, double p3_y)
	{
		double s02_x, s02_y, s10_x, s10_y, s32_x, s32_y, s_numer, t_numer, denom, t;
		s10_x = p1_x - p0_x;
		s10_y = p1_y - p0_y;
		s32_x = p3_x - p2_x;
		s32_y = p3_y - p2_y;

		denom = s10_x * s32_y - s32_x * s10_y;
		if (denom == 0)
			return false; // Collinear

		boolean denomPositive = denom > 0;

		s02_x = p0_x - p2_x;
		s02_y = p0_y - p2_y;
		s_numer = s10_x * s02_y - s10_y * s02_x;
		if ((s_numer < 0) == denomPositive)
			return false; // No collision

		t_numer = s32_x * s02_y - s32_y * s02_x;
		if ((t_numer < 0) == denomPositive)
			return false; // No collision

		if (((s_numer > denom) == denomPositive) || ((t_numer > denom) == denomPositive))
			return false; // No collision

		// Collision detected
		t = t_numer / denom;
		double i_x = p0_x + (t * s10_x);
		double i_y = p0_y + (t * s10_y);

		// check if intersection belongs to line segment
		if ((Math.min(p0_x,p1_x) < i_x) &&
				(i_x < Math.max(p0_x,p1_x)) &&
				(Math.min(p0_y,p1_y) < i_y) &&
				(i_y < Math.max(p0_y,p1_y)))
			return true;

		return false;
	}
}
