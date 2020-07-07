package edu.cg;

import java.awt.Component;
import java.util.List;

import javax.swing.JOptionPane;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;

import edu.cg.algebra.Point;
import edu.cg.algebra.Vec;
import edu.cg.models.BoundingSphere;
import edu.cg.models.Track;
import edu.cg.models.TrackSegment;
import edu.cg.models.Car.F1Car;
import edu.cg.models.Car.Specification;

/**
 * An OpenGL 3D Game.
 *
 */
public class NeedForSpeed implements GLEventListener {
	private GameState gameState = null; // Tracks the car movement and orientation
	private F1Car car = null; // The F1 car we want to render
	private Vec carCameraTranslation = null; // The accumulated translation that should be applied on the car, camera and light sources
	private Track gameTrack = null; // The game track we want to render
	private FPSAnimator ani; // This object is responsible to redraw the model with a constant FPS
	private Component glPanel; // The canvas we draw on.
	private boolean isModelInitialized = false; // Whether model.init() was called.
	private boolean isDayMode = true; // Indicates whether the lighting mode is day/night.
	private boolean isBirdseyeView = false; // Indicates whether the camera is looking from above on the scene or looking
	// towards the car direction.
	// add fields as you want. For example:
	// - Car initial position (should be fixed).
	// - Camera initial position (should be fixed)
	// - Different camera settings
	// - Light colors
	// Or in short anything reusable - this make it easier for your to keep track of your implementation.
	private Vec initialPosition;
	private Vec newCarPosition;
	
	
	public NeedForSpeed(Component glPanel) {
		this.glPanel = glPanel;
		gameState = new GameState();
		gameTrack = new Track();
		carCameraTranslation = new Vec(0.0);
		initialPosition = new Vec(0, 0.5, -6);
		car = new F1Car();
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		if (!isModelInitialized) {
			initModel(gl);
		}
		if (isDayMode) {
			// Setup background when day mode is on
            gl.glClearColor(0.52f, 0.824f, 1.0f, 1.0f);
		} else {
            gl.glClearColor(0.0f, 0.0f, 0.32f, 1.0f);
		}
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		// This is the flow in which we render the scene.
		// Step (1) Update the accumulated translation that needs to be
		// applied on the car, camera and light sources.
		updateCarCameraTranslation(gl);
		// Step (2) Position the camera and setup its orientation
		setupCamera(gl);
		// Step (3) setup the lights.
		setupLights(gl);
		// Step (4) render the car.
		renderCar(gl);
		// Step (5) render the track.
		renderTrack(gl);
		// Step (6) check collision. Note this has nothing to do with OpenGL.
		if (checkCollision()) {
			JOptionPane.showMessageDialog(this.glPanel, "Game is Over");
			this.gameState.resetGameState();
			this.carCameraTranslation = new Vec(0.0);
		}

	}

	/**
	 * @return Checks if the car intersects the one of the boxes on the track. # TODO CHANGE VARIABLE NAMES AND ADD COMMENTS
	 */
	private boolean checkCollision() {
		// Implement this function to check if the car collides into one of the boxes.
		// You can get the bounding spheres of the track by invoking: 
		List<BoundingSphere> trackBoundingSpheres = gameTrack.getBoundingSpheres();
		List<BoundingSphere> carComponentsBoundingSpheres = car.getBoundingSpheres();
		for (BoundingSphere trackBS : trackBoundingSpheres) {
			boolean wholeCar = true;
			for (BoundingSphere carComponentBS : carComponentsBoundingSpheres) {

				// We create a new bounding sphere for the current car component
				double newRadius =  carComponentBS.getRadius() * 4; // car is scaled by 4 as well
				Point previousCenter = carComponentBS.getCenter();
				Point newCenter = new Point(previousCenter.x + carCameraTranslation.x, Specification.TIRE_RADIUS,previousCenter.z + carCameraTranslation.z - 7.5);
				BoundingSphere newCarComponentBS = new BoundingSphere(newRadius, newCenter);

				// We check if this car component is s1 (the whole f1 car)
				if (wholeCar) {
					wholeCar = false;

					// if the box doesn't intersect with the bounding sphere of
					// the whole car, no need to check the smaller car components
					if (!trackBS.checkIntersection(newCarComponentBS))
						break;
					else
						continue;
				}

				// This isn't s1
				if (trackBS.checkIntersection(newCarComponentBS)) {
					return true;
				}
			}
		}
		return false;
	}

	private void updateCarCameraTranslation(GL2 gl) {
		// Update the car and camera translation values (not the ModelView-Matrix).
		// - Always keep track of the car offset relative to the starting
		// point.
		// - Change the track segments here.
		Vec ret = gameState.getNextTranslation();
		carCameraTranslation = carCameraTranslation.add(ret);
		double dx = Math.max(carCameraTranslation.x, -TrackSegment.ASPHALT_TEXTURE_DEPTH / 2.0 - 2);
		carCameraTranslation.x = (float) Math.min(dx, TrackSegment.ASPHALT_TEXTURE_DEPTH / 2.0 + 2);
		if (Math.abs(carCameraTranslation.z) >= TrackSegment.TRACK_LENGTH + 10.0) {
			carCameraTranslation.z = -(float) (Math.abs(carCameraTranslation.z) % TrackSegment.TRACK_LENGTH);
			gameTrack.changeTrack(gl);
		}
	}

	private void setupCamera(GL2 gl) {
		newCarPosition = initialPosition.add(carCameraTranslation);
		final GLU glu = new GLU();
        if (this.isBirdseyeView) {
        	glu.gluLookAt(newCarPosition.x, 40, -18+newCarPosition.z,
					newCarPosition.x, 1, -18+newCarPosition.z,
					0, 0, -1);
        } else {
        	glu.gluLookAt(newCarPosition.x,2.0,6 + newCarPosition.z,
					newCarPosition.x,2.0,-1.0 + newCarPosition.z,
					0.0,1.0,0.0);
        }
	}

	private void setupLights(GL2 gl) {
		if (isDayMode) {
			// Setup day lighting.
			// * Remember: switch-off any light sources that were used in night mode and are not use in day mode.
			gl.glDisable(16385);
            this.setupSun(gl, 16384);
		} else {
			// Setup night lighting.
			// * Remember: switch-off any light sources that are used in day mode
			// * Remember: spotlight sources also move with the camera.
			// * You may simulate moon-light using ambient light.
			this.setupMoon(gl);
			gl.glDisable(gl.GL_LIGHT0);
			gl.glEnable(gl.GL_LIGHT1);
			gl.glEnable(gl.GL_LIGHT2);
			setupCarLight(gl, GL2.GL_LIGHT1);
			setupCarLight(gl, GL2.GL_LIGHT2);
            
		}

	}
	
	private void setupSun(final GL2 gl, final int light) {
        final float[] sunColor = { 1.0f, 1.0f, 1.0f, 1.0f };
        final Vec dir = new Vec(0.0, 1.0, 1.0).normalize();
        final float[] pos = { dir.x, dir.y, dir.z, 0.0f };
        gl.glLightfv(light, 4610, sunColor, 0);
        gl.glLightfv(light, 4609, sunColor, 0);
        gl.glLightfv(light, 4611, pos, 0);
        gl.glLightfv(light, 4608, new float[] { 0.1f, 0.1f, 0.1f, 1.0f }, 0);
        gl.glEnable(light);
    }
    
    private void setupMoon(final GL2 gl) {
        gl.glLightModelfv(2899, new float[] { 0.25f, 0.25f, 0.3f, 1.0f }, 0);
    }
    
    private void setupCarLight(GL2 gl, int light) {
		float[] spotLightPosition;
		if (light == gl.GL_LIGHT1) {
			// 1st car spotlight
			spotLightPosition = new float[]{this.newCarPosition.x, carCameraTranslation.y + 2, -8.5f + this.carCameraTranslation.z,  1.0f};
		} else {
			// 2nd car spotlight
			spotLightPosition = new float[]{this.newCarPosition.x, carCameraTranslation.y + 2, -8.5f + this.carCameraTranslation.z,  1.0f};
		}
		float[] lightArr = new float[]{ 0.6f,  0.6f,  0.6f, 1};
		gl.glLightfv(light, GL2.GL_DIFFUSE, lightArr, 0);
		gl.glLightfv(light, GL2.GL_SPECULAR, lightArr, 0);
		gl.glLightfv(light, GL2.GL_SPOT_DIRECTION, new float[] {((float)Math.sin(gameState.getCarRotation())), 0, - (float) Math.cos((gameState.getCarRotation()))}, 0);
		gl.glLightfv(light, GL2.GL_POSITION, spotLightPosition, 0);
		gl.glLightf(light, GL2.GL_SPOT_CUTOFF,  90.0f);
	}
   
    	
	
	private void renderTrack(GL2 gl) {
		// * Note: the track is not translated. It should be fixed.
		gl.glPushMatrix();
		gameTrack.render(gl);
		gl.glPopMatrix();
	}

	private void renderCar(GL2 gl) {
		// Render the car.
		// * Remember: the car position should be the initial position + the accumulated translation.
		//             This will simulate the car movement.
		// * Remember: the car was modeled locally, you may need to rotate/scale and translate the car appropriately.
		// * Recommendation: it is recommended to define fields (such as car initial position) that can be used during rendering.
//		final double carRotation = this.gameState.getCarRotation();
//        gl.glPushMatrix();
//        gl.glTranslated(
//        		this.carInitialPosition[0] + this.carCameraTranslation.x, 
//        		this.carInitialPosition[1] + this.carCameraTranslation.y, 
//        		this.carInitialPosition[2] + this.carCameraTranslation.z);
//        gl.glRotated(90.0 - carRotation, 0.0, 1.0, 0.0);
//        gl.glScaled(this.carScale, this.carScale, this.carScale);
//        this.car.render(gl);
//        gl.glPopMatrix();
//	}
		double carRotation = this.gameState.getCarRotation();
		gl.glPushMatrix();
		gl.glTranslated(newCarPosition.x, newCarPosition.y, newCarPosition.z);
		gl.glRotated(-carRotation, 0, 1, 0);
		gl.glRotated(90, 0, 1, 0);
		gl.glScaled(4, 4, 4);
		this.car.render(gl);
		gl.glPopMatrix();
	}

	public GameState getGameState() {
		return gameState;
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();

		// Initialize display callback timer
		ani = new FPSAnimator(30, true);
		ani.add(drawable);
		glPanel.repaint();

		initModel(gl);
		ani.start();
	}

	public void initModel(GL2 gl) {
		gl.glCullFace(GL2.GL_BACK);
		gl.glEnable(GL2.GL_CULL_FACE);

		gl.glEnable(GL2.GL_NORMALIZE);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glEnable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_SMOOTH);

		car.init(gl);
		gameTrack.init(gl);
		isModelInitialized = true;
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		// Setup the projection matrix here.
		 final GL2 gl = drawable.getGL().getGL2();
		 gl.glMatrixMode(GL2.GL_PROJECTION);
		 gl.glLoadIdentity();

		 GLU glu = new GLU();
		 double aspectRatio = (double)(width/height);
		 glu.gluPerspective(60,aspectRatio, 2, 500);
		}


	/**
	 * Start redrawing the scene with 30 FPS
	 */
	public void startAnimation() {
		if (!ani.isAnimating())
			ani.start();
	}

	/**
	 * Stop redrawing the scene with 30 FPS
	 */
	public void stopAnimation() {
		if (ani.isAnimating())
			ani.stop();
	}

	public void toggleNightMode() {
		isDayMode = !isDayMode;
	}

	public void changeViewMode() {
		isBirdseyeView = !isBirdseyeView;
	}

}
