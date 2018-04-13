package AgentsLineOfSight;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.layout.Pane;

public class Mover extends SpriteView {

	private double sweepAngleRad = Math.toRadians(90); 
	private double viewAngle = 0;
	private double targetAngle = 0;
	
	List<Line> scanLines = new ArrayList<>();
	List<PVector> intersectionPoints = new ArrayList<>();
	
	public Mover(Pane layer) {
		super(layer);
	}

	public void move() {
		
		super.move();
		
		updateViewAngle();

	}
	
	public PVector getAverageIntersection() {
		
		// calculatge sum
		PVector sum = new PVector(0, 0);
		for( PVector other: getIntersectionPoints()) {
			sum.add(other);
		}
		
		// average
		sum.div( getIntersectionPoints().size());
		
		return sum;
	}

	
	private void updateViewAngle() {

		// as long as we are moving, we get a viewing angle
		if( velocity.x != 0 || velocity.y != 0) {
			targetAngle = getAngleRad();
		}

		// check if the angle exceeds a given delta
		double diff = targetAngle - viewAngle;
		
		viewAngle += diff;
	
	}
	
	public double getViewAngleRad() {
		return viewAngle;
	}
	
	public double getSweepAngleRad() {
		return sweepAngleRad;
	}

	public void setSweepAngleRad(double sweepAngleRad) {
		this.sweepAngleRad = sweepAngleRad;
	}

	public List<Line> getScanLines() {
		return scanLines;
	}

	public void setScanLines(List<Line> scanLines) {
		this.scanLines = scanLines;
	}

	public List<PVector> getIntersectionPoints() {
		return intersectionPoints;
	}

	public void setIntersectionPoints(List<PVector> intersectionPoints) {
		this.intersectionPoints = intersectionPoints;
	}
	
}
