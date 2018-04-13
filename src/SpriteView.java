package AgentsLineOfSight;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class SpriteView extends SpriteBase {

	Node view = null;
	Pane layer = null;
	
	double width = 10;
	double height = 10;
	double centerX = width / 2;
	double centerY = height / 2;
	
	public SpriteView( Pane layer) {
	
		this.layer = layer;
		
		createView();
		addViewToLayer();
		
	}
	
	public void createView() {
		
		Rectangle rectangle = new Rectangle( 0,0,width,height);
		rectangle.setStroke(Color.BLACK);
		rectangle.setFill(Color.LIGHTSLATEGRAY.deriveColor(1, 1, 1, 0.3));

		this.view = rectangle;
		
	}
	
	public void addViewToLayer() {
		
		layer.getChildren().add( view);
		
	}

	public void removeViewFromLayer() {
		
		layer.getChildren().remove( view);
		
	}

	public Node getView() {
		return view;
	}
	
	public void updateUI() {
		view.relocate(location.x - centerX, location.y - centerY);
	}
}
