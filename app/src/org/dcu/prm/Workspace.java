/*
 * Copyright (c) 2004 Patric Jensfelt.
 * All Rights Reserved.
 */

package org.dcu.prm;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

class Circle {
	double xC, yC, radius;

	public Circle(double xC, double yC, double radius) {
		this.xC = xC;
		this.yC = yC;
		this.radius = radius;
	}
};

class Rectangle {
	double xC, yC, width, height, angle;

	public Rectangle(double xC, double yC, double width, double height) {
		this.xC = xC;
		this.yC = yC;
		this.width = width;
		this.height = height;
	}
};

class Vector {

	double x, y;

	public Vector(double xHead, double yHead, double xTail, double yTail) {
		x = xHead - xTail;
		y = yHead - yTail;
	}

	// return the dot product of vectors this.that
	double dot(Vector that) {
		return ((this.x*that.x)+(this.y*that.y));
	}

	// return the cross product of vectors thisXthat
	double cross(Vector that) {
		return ((this.x*that.y)-(this.y*that.x));
	}

	// return the Euclidean norm of this Vector
	double magnitude() {
		return Math.sqrt(this.dot(this));
	}

	// return the cos(theta) with that vector
	public double cosTheta(Vector that) {
		return (this.dot(that)/(this.magnitude()*that.magnitude()));
	}

	// return the angle with that vector
	public double angle(Vector that) {
		return Math.acos(this.cosTheta(that));
	}
}

/**
 * The base class for represents the world
 */
public class Workspace {

	public double xStart, yStart;
	public double xGoal, yGoal;
	public double xMin, xMax, yMin, yMax;
	public List<Circle> circles;
	public List<Rectangle> rectangles;
	public List<Path> paths;

	private static final double CLEARANCE = 0.1;

	public Workspace(JsonObject ws) {

		circles = new ArrayList<Circle>();
		rectangles = new ArrayList<Rectangle>();
		paths = Collections.synchronizedList(new ArrayList<Path>());

		xStart = ws.getJsonObject("start").getJsonNumber("x").doubleValue();
		yStart = ws.getJsonObject("start").getJsonNumber("y").doubleValue();
		xGoal = ws.getJsonObject("goal").getJsonNumber("x").doubleValue();
		yGoal = ws.getJsonObject("goal").getJsonNumber("y").doubleValue();

		xMin = ws.getJsonObject("bounds").getJsonNumber("xMin").doubleValue();
		xMax = ws.getJsonObject("bounds").getJsonNumber("xMax").doubleValue();
		yMin = ws.getJsonObject("bounds").getJsonNumber("yMin").doubleValue();
		yMax = ws.getJsonObject("bounds").getJsonNumber("yMax").doubleValue();

		double xCenter, yCenter, radius, width, height;
		JsonArray jsonCircles = ws.getJsonArray("circles");
		JsonArray jsonRectangles = ws.getJsonArray("rectangles");

		for (int i = 0; i < jsonCircles.size(); i++) {

			xCenter = jsonCircles.getJsonObject(i).getJsonNumber("xCenter").doubleValue();
			yCenter = jsonCircles.getJsonObject(i).getJsonNumber("yCenter").doubleValue();
			radius = jsonCircles.getJsonObject(i).getJsonNumber("radius").doubleValue();

			circles.add(i, new Circle(xCenter, yCenter, radius));
		}

		for (int i = 0; i < jsonRectangles.size(); i++) {

			xCenter = jsonRectangles.getJsonObject(i).getJsonNumber("xCenter").doubleValue();
			yCenter = jsonRectangles.getJsonObject(i).getJsonNumber("yCenter").doubleValue();
			width = jsonRectangles.getJsonObject(i).getJsonNumber("width").doubleValue();
			height = jsonRectangles.getJsonObject(i).getJsonNumber("height").doubleValue();

			rectangles.add(i, new Rectangle(xCenter, yCenter, width, height));
		}
	}

	/**
	 * Use this function to check if a certain point collides with any
	 * of the obstales in the world
	 *
	 * @param x x-coordinate of point to check for collision
	 * @param y y-coordinate of point to check for collision
	 * @return true if point (x,y) collides with any of the obstacles
	 */
	public boolean collidesWith(double x, double y) {

		Circle cir;
		Rectangle rec;
		double dx, dy, w2, h2, radius;

		Iterator<Circle> iC = circles.iterator();

		while(iC.hasNext()){
			cir = iC.next();

			dx = x - cir.xC;
			dy = y - cir.yC;
			radius = cir.radius;

			if (Math.sqrt(dx*dx+dy*dy) <= (radius + CLEARANCE))
				return true;
		}

		Iterator<Rectangle> iR = rectangles.iterator();

		while(iR.hasNext()){
			rec = iR.next();

			//dx = x - rec.xC;
			//dy = y - rec.yC;
			w2 = rec.width/2;
			h2 = rec.height/2;
			//radius = Math.sqrt(w2*w2+h2*h2);

			//if (Math.sqrt(dx*dx+dy*dy) <= (radius + CLEARANCE))
			//	return true;

			Vector ca = new Vector(x, y, rec.xC, rec.yC);
			Vector ck = new Vector(rec.xC, (rec.yC+h2), rec.xC, rec.yC);
			Vector cl = new Vector((rec.xC+w2), rec.yC, rec.xC, rec.yC);

			if ((Math.abs(ca.magnitude() * ck.cosTheta(ca)) <= (h2+ CLEARANCE)) &&
					(Math.abs(ca.magnitude() * cl.cosTheta(ca)) <= (w2+ CLEARANCE)))
				return true;
		}

		return false;
	}

/*
	boolean collidesWithPath(double xB1, double yB1, double xB2, double yB2) {

		Path path;
		Iterator<Path> iP = paths.iterator();

		while(iP.hasNext()){
			path = iP.next();

			// it is required to add new b1-b2 line, ensure that it is not
			// colliding with points of existing path a

			// b1 select closest 2* points from existing path a -> a1 & a2
			List<Distance> b1distances = path.getDistances(xB1, yB1);
			Position a1 = path.points.get((b1distances.get(0).i));
			Position a2 = path.points.get((b1distances.get(1).i));
			// b2 select closest 2* points from existing path a -> a3 & a4
			List<Distance> b2distances = path.getDistances(xB2, yB2);
			Position a3 = path.points.get((b2distances.get(0).i));
			Position a4 = path.points.get((b2distances.get(1).i));

			// b1 calculate angels to points a1, a2 and b2
			// If b2^ angle < (a1^ angle + a2^ angle) => collision detected
			Vector b1a1 = new Vector(a1.getX(), a1.getY(), xB1, yB1);
			Vector b1a2 = new Vector(a2.getX(), a2.getY(), xB1, yB1);
			Vector b1b2 = new Vector(xB2, yB2, xB1, yB1);

			// b2 calculate angels to points a4, a5 and b1
			// If b1^ angle < (a4^ angle + a5^ angle) => collision detected
			Vector b2a3 = new Vector(a3.getX(), a3.getY(), xB2, yB2);
			Vector b2a4 = new Vector(a4.getX(), a4.getY(), xB2, yB2);
			Vector b2b1 = new Vector(xB1, yB1, xB2, yB2);

			// select a point falling in between b1 & b2 points -> a5
			search: {
				for (int i = 0; i < b1distances.size(); i++) {
					for (int j = i; j < b2distances.size(); j++) {
						if(b1distances.get(i).i == b2distances.get(j).i) {
							// found a common point
							Position a5 = path.points.get((b1distances.get(i).i));

							Vector b1a5 = new Vector(a5.getX(), a5.getY(), xB1, yB1);
							Vector b2a5 = new Vector(a5.getX(), a5.getY(), xB2, yB2);

							//ensure it is falling in between
							// this by checking that both angels are acute
							if ((b2a5.cosTheta(b2b1) > 0) && (b1a5.cosTheta(b1b2) > 0)) {
								//System.out.println("\nxxxx: Point a5" + a5 + "falls in between b1<"+xB1+","+yB1+"> & b2<"+xB2+","+yB2+">\n");
								//System.out.println("\n+++\nclf;hold on;grid on;grid minor;\nplot(" + a5.getX() +", " + a5.getY() + ", 'b');");
								//System.out.println("plot([" + xB1 + ", " + xB2 + "], ["
								//		+ yB1 + ", " + yB2 + "], 'r');");
								//System.out.println("text(" + a5.getX() + ", " + a5.getY() + ",\'a5\');");
								//System.out.println("text(" + xB1 + ", " + yB1 + ",\'b1\');");
								//System.out.println("text(" + xB2 + ", " + yB2 + ",\'b2\');");
								//System.out.println("hold off;\n+++");
								break search;
							}
						}
					}
				}

				// didn't find point in between, fail to try again
				System.out.println("Didn't find a5 point!");
				return true;
			}

			if ((b1a1.cross(b1b2) * b1a1.cross(b1a2) >= 0) &&
					(b1a2.cross(b1b2) * b1a2.cross(b1a1) >= 0) &&
					(b2a3.cross(b2b1) * b2a3.cross(b2a4) >= 0) &&
					(b2a4.cross(b2b1) * b2a4.cross(b2a3) >= 0)) {
				//System.out.println("aaa edge a1"+a1+" a2"+a2+" collides with edge b1<"+xB1+","+yB1+"> b2<"+xB2+","+yB2+">");
				//System.out.println("\n+++\nb1a1^b1b2 (" + b1a1.cross(b1b2) + ") * b1a1^b1a2 (" + b1a1.cross(b1a2)+") && "
				//		+ "b1a2^b1b2 (" + b1a2.cross(b1b2) + ") b1a2^b1a1 (" + b1a2.cross(b1a1));
				//System.out.println("\n+++\nclf;hold on;grid on;grid minor;\nplot([" + a1.getX() + ", " + a2.getX() + "], ["
				//		+ a1.getY() + ", " + a2.getY() + "], 'b');");
				//System.out.println("plot([" + xB1 + ", " + xB2 + "], ["
				//		+ yB1 + ", " + yB2 + "], 'r');");
				//System.out.println("text(" + a1.getX() + ", " + a1.getY() + ",\'a1\');");
				//System.out.println("text(" + a2.getX() + ", " + a2.getY() + ",\'a2\');");
				//System.out.println("text(" + xB1 + ", " + yB1 + ",\'b1\');");
				//System.out.println("text(" + xB2 + ", " + yB2 + ",\'b2\');");
				//System.out.println("hold off;\n+++");
				return true;
			}
		}

		System.out.println("no collision!");
		// If none detected, all clear
		return false;
	}

	// Returns true if the lines intersect, otherwise false.
	boolean isLineIntersection(double xB1, double yB1, double xB2, double yB2,
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
*/
	public boolean collidesWithPath(double x1, double y1, double x2, double y2) {

		Path path;
		Iterator<Path> iP = paths.iterator();
		while(iP.hasNext()){
			path = iP.next();
			if (path.intersectWith(x1, y1, x2, y2))
				return true;
		}
		// If none detected, all clear
		return false;
	}

	/**
	 * This function will go through all the obstacles in the world and
	 * ask for them to to write matlab display code to the stream.
	 *
	 * @param fs reference to and output stream, for example an fstream (file)
	 * @return N/A
	 */
	public void writeMatlabDisplayCode(String mfile)
			throws IOException {

		Circle cir;
		Rectangle rec;
		Path path;

		FileWriter fw = new FileWriter(mfile);

		fw.write("% Auto-generated workspace m-code description\n");
		fw.write("% xMin=" + xMin + "\n");
		fw.write("% xMax=" + xMax + "\n");
		fw.write("% yMin=" + yMin + "\n");
		fw.write("% yMax=" + yMax + "\n");
		fw.write("clf\nhold on\n");

		Iterator<Circle> iC = circles.iterator();

		while(iC.hasNext()){
			cir = iC.next();

			fw.write("fill(" +
					cir.xC + " + " + cir.radius + "*cos((0:5:360)/180*pi)," +
					cir.yC + " + " + cir.radius + "*sin((0:5:360)/180*pi) , 'b')\n");
		}

		double x, y, w, h;
		Iterator<Rectangle> iR = rectangles.iterator();

		while(iR.hasNext()){
			rec = iR.next();

			x = rec.xC;
			y = rec.yC;
			w = rec.width;
			h = rec.height;

			fw.write("fill([" +
					new Double(x-(w/2)) + ", " + new Double(x+(w/2)) + ", " + new Double(x+(w/2)) + ", " +
					new Double(x-(w/2)) + ", " + new Double(x-(w/2)) + "], [" +
					new Double(y-(h/2)) + ", " + new Double(y-(h/2)) + ", " + new Double(y+(h/2)) + ", " +
					new Double(y+(h/2)) + ", " + new Double(y-(h/2)) + "], 'b')\n");
		}

		Iterator<Path> iP = paths.iterator();

		while(iP.hasNext()){
			path = iP.next();

			for (int i = 0; i < path.points.size()-1; i++) {
				Position p1 = path.points.get(i);
				Position p2 = path.points.get(i+1);
				fw.write("plot([" + p1.getX() + ", " + p2.getX() + "], ["
						+ p1.getY() + ", " + p2.getY() + "], 'r')\n");
				//fw.write("text(" + p1.getX() + ", " + p1.getY() + ",\'["+ String.format("%.2f",p1.getX()) + "," + String.format("%.2f",p1.getY()) +"]\');");
				//fw.write("text(" + p2.getX() + ", " + p2.getY() + ",\'["+ String.format("%.2f",p2.getX()) + "," + String.format("%.2f",p2.getY()) +"]\');");
			}
			// put some label on the plotted path
			fw.write(String.format("plot(%f,%f,\'->\',\'MarkerSize\',10,\'MarkerFaceColor\',\'y\');\n",
					path.points.get(5).getX(), path.points.get(5).getY()));
			fw.write(String.format("text(%f-0.1,%f-0.1,\'[%s]\');\n",
					path.points.get(5).getX(), path.points.get(5).getY(), path.id));
		}

		fw.write("plot(" + xStart + ", " + yStart + ",\'-s\',\'MarkerSize\',10, \'MarkerFaceColor\',\'g\');\n");
		fw.write("plot(" + xGoal + ", " + yGoal + ",\'-s\',\'MarkerSize\',10, \'MarkerFaceColor\',\'g\');\n");
		fw.write("text(" + xStart + ", " + yStart + "-0.2,\'S\',\'FontSize\',12);\n");
		fw.write("text(" + xGoal + ", " + yGoal + "-0.2,\'G\',\'FontSize\',12);\n");
		fw.write("hold off;grid on;grid minor;\n");
		fw.write("xlim(["+ xMin + " " + xMax + "]);\n");
		fw.write("ylim(["+ yMin + " " + yMax + "]);\n");
		fw.write("title(\'Multi-Robot Workspace\');\n");
		//fw.write("axis equal");

		fw.close();
	}

	public void deleteAllPaths() {
		for (int i = paths.size(); i > 0; i--) {
			paths.remove(i-1);
		}
	}

	public void addPath(Path path) {
		// if robot has a path already.. overwrite
		for (int i = 0; i < paths.size(); i++) {
			if(paths.get(i).id.equals(path.id)) {
				paths.set(i, path);
				return;
			}
		}
		// no, this is new.. append
		paths.add(path);
	}

	@Override
	public String toString() {
		StringBuffer ws = new StringBuffer();
		ws.append("\nWorkspace:\n==========\n");
		ws.append("xStart: " + xStart + "\n");
		ws.append("yStart: " + xStart + "\n");
		ws.append("xGoal: " + xGoal + "\n");
		ws.append("yGoal: " + yGoal + "\n");

		for (int i = 0; i < circles.size(); i++) {
			ws.append("Circle["+i+"]: " +
					circles.get(i).xC +" "+
					circles.get(i).yC +" "+
					circles.get(i).radius + "\n");
		}

		for (int i = 0; i < rectangles.size(); i++) {
			ws.append("Rectangle["+i+"]: " +
					rectangles.get(i).xC +" "+
					rectangles.get(i).yC +" "+
					rectangles.get(i).width +" "+
					rectangles.get(i).height + "\n");
		}

		return ws.toString();
	}
}
