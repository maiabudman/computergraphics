package edu.cg.models;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;

import edu.cg.algebra.Point;
import edu.cg.models.Car.Materials;

public class BoundingSphere implements IRenderable {
	private double radius = 0.0;
	private Point center;
	private double color[];

	public BoundingSphere(double radius, Point center) {
		color = new double[3];
		this.setRadius(radius);
		this.setCenter(new Point(center.x, center.y, center.z));
	}

	public void setSphereColore3d(double r, double g, double b) {
		this.color[0] = r;
		this.color[1] = g;
		this.color[2] = b;
	}

	/**
	 * Given a sphere s - check if this sphere and the given sphere intersect.
	 * 
	 * @return true if the spheres intersects, and false otherwise
	 */
	public boolean checkIntersection(BoundingSphere s) {
		// Checking the distance between current sphere and given sphere
		float sphereDistances = this.center.dist(s.center);
		// Claiming a threshold of minimum distance between the spheres such that
		// we can't have an intersection
		double radiusThreshold = this.radius + s.radius;
		return sphereDistances <= radiusThreshold;
	}

	public void translateCenter(double dx, double dy, double dz) {
		this.center = this.center.add(new Point(dx, dy, dz));
	}

	@Override
	public void render(GL2 gl) {	
	    //adding Matrix to the stack 
		 gl.glPushMatrix();
	     GLU glu = new GLU();
	     GLUquadric quad = glu.gluNewQuadric();
	     
	     gl.glTranslated((double) this.center.x,(double) this.center.y,(double) this.center.z);
	     //specifying colors
	     float[] colorRendering = new float[this.color.length];
	     for (int i = 0; i < colorRendering.length; i++)
	         colorRendering[i] = (float) this.color[i];

	     // Rendering the sphere
	     Materials.SetMetalMaterial(gl, colorRendering);
	     glu.gluSphere(quad, this.radius, 10, 20);
	     //pop matrix from stack
	     gl.glPopMatrix();
	}

	@Override
	public void init(GL2 gl) {
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	public Point getCenter() {
		return center;
	}

	public void setCenter(Point center) {
		this.center = center;
	}

	@Override
	public void destroy(GL2 gl) {
		// TODO Auto-generated method stub

	}

}
