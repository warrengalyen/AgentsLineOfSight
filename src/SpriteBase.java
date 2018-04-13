package AgentsLineOfSight;

import java.util.List;

public class SpriteBase {

	PVector location;
	PVector velocity;
	PVector acceleration;

	double maxSpeed = 1;
	double maxForce = 0.3;

	public SpriteBase() {

		this.location = new PVector(0, 0);
		this.velocity = new PVector(0, 0);
		this.acceleration = new PVector(0, 0);

	}

	public void applyForce(PVector force) {
		acceleration.add(force);
	}

	public void move() {

		// set velocity depending on acceleration
		velocity.add(acceleration);

		// limit velocity to max speed
		velocity.limit(maxSpeed);

		// change location depending on velocity
		location.add(velocity);

		// clear acceleration
		acceleration.mult(0);
	}

	public void setLocation(double x, double y) {
		location.x = x;
		location.y = y;
	}

	public void setVelocity(double x, double y) {
		velocity.x = x;
		velocity.y = y;
	}

	public double getAngleRad() {
		return velocity.heading2D();
	}

	public PVector seek(PVector target) {

		PVector desired = PVector.sub(target, location);

		desired.normalize();
		desired.mult(maxSpeed);

		PVector steer = PVector.sub(desired, velocity);
		steer.limit(maxForce);

		return steer;
	}

	public PVector separate(List<Mover> sprites) {

		double desiredseparation = 100;

		PVector steer = new PVector(0, 0, 0);
		int count = 0;

		for (SpriteBase other : sprites) {

			double d = PVector.dist(location, other.location);

			if ((d > 0) && (d < desiredseparation)) {

				PVector diff = PVector.sub(location, other.location);
				diff.normalize();
				diff.div(d);

				steer.add(diff);

				count++;
			}
		}

		if (count > 0) {
			steer.div((double) count);
		}

		if (steer.mag() > 0) {

			steer.normalize();
			steer.mult(maxSpeed);
			steer.sub(velocity);
			steer.limit(maxForce);
		}

		return steer;
	}

	public PVector separate(PVector target) {

		double forceLimit = 3;
		double desiredseparation = 10;

		PVector steer = new PVector(0, 0, 0);
		int count = 0;

		double d = PVector.dist(location, target);

		if ((d > 0) && (d < desiredseparation)) {

			PVector diff = PVector.sub(location, target);
			diff.normalize();
			diff.div(d);

			steer.add(diff);

			count++;
		}

		if (count > 0) {
			steer.div((double) count);
		}

		if (steer.mag() > 0) {

			steer.normalize();
			steer.mult(maxSpeed);
			steer.sub(velocity);
			steer.limit(forceLimit);
		}

		return steer;
	}

	public PVector getLocation() {
		return location;
	}

	public PVector getVelocity() {
		return velocity;
	}

	public PVector getAcceleration() {
		return acceleration;
	}

	public double getMaxSpeed() {
		return maxSpeed;
	}

	public void setMaxSpeed(double maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

}
