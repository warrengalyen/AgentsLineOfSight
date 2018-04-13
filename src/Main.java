package AgentsLineOfSight;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;

/**
 * Brute Force Line of Sight Algorithm: Use ScanLines to detect the visible area from a given position.
 * Regarding the movement of the agents, please read The Nature of Code: http://natureofcode.com/
 */
public class Main extends Application {

	Random rnd = new Random();

	Canvas backgroundCanvas;
	GraphicsContext backgroundGraphicsContext;

	Canvas foregroundCanvas;
	GraphicsContext foregroundGraphicsContext;

	/**
	 * Container for canvas and other nodes like attractors and repellers
	 */
	Pane layerPane;

	AnimationTimer animationLoop;

	Scene scene;

	List<Line> sceneLines;

	/**
	 * Current mouse location
	 */
	MouseStatus mouseStatus = new MouseStatus();

	Level levelGenerator;

	Algorithm algorithm = new Algorithm();

	List<Mover> players;
	List<Mover> enemies;

	@Override
	public void start(Stage primaryStage) {

		BorderPane root = new BorderPane();

		// canvas
		backgroundCanvas = new Canvas(Settings.get().getCanvasWidth(), Settings.get().getCanvasHeight());
		backgroundGraphicsContext = backgroundCanvas.getGraphicsContext2D();

		foregroundCanvas = new Canvas(Settings.get().getCanvasWidth(), Settings.get().getCanvasHeight());
		foregroundGraphicsContext = foregroundCanvas.getGraphicsContext2D();

		// layers
		layerPane = new Pane();
		layerPane.getChildren().addAll(backgroundCanvas, foregroundCanvas);

		backgroundCanvas.widthProperty().bind(layerPane.widthProperty());
		foregroundCanvas.widthProperty().bind(layerPane.widthProperty());

		root.setCenter(layerPane);

		// toolbar
		Node toolbar = Settings.get().createToolbar();
		root.setRight(toolbar);

		scene = new Scene(root, Settings.get().getSceneWidth(), Settings.get().getSceneHeight(), Settings.get().getSceneColor());

		primaryStage.setScene(scene);
		primaryStage.setTitle("Demo");

		// primaryStage.setFullScreen(true);
		// primaryStage.setFullScreenExitHint("");
		primaryStage.show();

		// add content
		createObjects();
		createPlayers();
		createEnemies();

		// listeners for settings
		addSettingsListeners();

		// add mouse location listener
		addInputListeners();

		// add context menus
		addCanvasContextMenu(backgroundCanvas);

		// run animation loop
		startAnimation();

	}

	private void createObjects() {

		levelGenerator = new Level();

		sceneLines = levelGenerator.getLines();

	}

	private void createPlayers() {

		players = new ArrayList<>();

		if (!Settings.hasPlayer)
			return;

		players.add(createPlayer());

	}

	private void createEnemies() {

		enemies = new ArrayList<>();
		for (int i = 0; i < Settings.ENEMY_COUNT; i++) {

			Mover mover = createAgent();
			enemies.add(mover);

		}

	}

	private Mover createPlayer() {

		double x = mouseStatus.x;
		double y = mouseStatus.y;

		Mover agent = new Mover(layerPane);
		agent.setLocation(x, y);

		// player is bound to the mouse location => limitation doesn't make
		// sense
		agent.setMaxSpeed(1000);

		return agent;

	}

	private Mover createAgent() {

		double x = levelGenerator.minX + rnd.nextDouble() * levelGenerator.width;
		double y = levelGenerator.minY + rnd.nextDouble() * levelGenerator.height;

		Mover agent = new Mover(layerPane);
		agent.setLocation(x, y);

		return agent;
	}

	private void startAnimation() {

		// start game
		animationLoop = new AnimationTimer() {

			FpsCounter fpsCounter = new FpsCounter();

			@Override
			public void handle(long now) {

				// update fps
				// ----------------------------
				fpsCounter.update(now);

				// ai: create scanlines & points
				// ----------------------------
				applyAlgorithm(players);
				applyAlgorithm(enemies);

				// player ai
				// ----------------------------
				for (Mover player : players) {
					PVector newLocation = new PVector(mouseStatus.x, mouseStatus.y);
					PVector distance = PVector.sub(newLocation, player.getLocation());
					PVector forceWithResetVelocity = PVector.sub(distance, player.getVelocity());
					player.applyForce(forceWithResetVelocity);
				}

				// enemy ai
				// ----------------------------
				for (Mover mover : enemies) {

					// move towards average intersection
					PVector avg = mover.getAverageIntersection();
					PVector seek = mover.seek(avg);
					mover.applyForce(seek);

					// separate from walls
					PVector separateFromWalls = mover.separate(avg);
					mover.applyForce(separateFromWalls);

					// separate from other movers
					PVector separateFromMovers = mover.separate(enemies);
					mover.applyForce(separateFromMovers);

				}

				// move
				// ----------------------------
				players.forEach(Mover::move);
				enemies.forEach(Mover::move);

				// update ui
				// ----------------------------
				players.forEach(Mover::updateUI);
				enemies.forEach(Mover::updateUI);

				// paint background canvas
				// ----------------------------
				// clear canvas. we don't use clearRect because we want a black
				// background
				backgroundGraphicsContext.setFill(Settings.get().getBackgroundColor());
				backgroundGraphicsContext.fillRect(0, 0, backgroundCanvas.getWidth(), backgroundCanvas.getHeight());

				// background
				paintGrid(Settings.get().getGridColor());

				// paint foreground canvas
				// ----------------------------
				// draw depending on mouse button down
				paintOnCanvas();

				// update overlays (statistics)
				// ----------------------------

				// show fps and other debug info
				backgroundGraphicsContext.setFill(Color.BLACK);
				backgroundGraphicsContext.fillText("Fps: " + fpsCounter.getFrameRate(), 1, 10);

			}
		};

		animationLoop.start();

	}

	private void applyAlgorithm(List<Mover> movers) {

		for (Mover mover : movers) {

			// get scanlines
			List<Line> scanLines = algorithm.createScanLines(mover);
			mover.setScanLines(scanLines);

			// get intersection points
			List<PVector> points = algorithm.getIntersectionPoints(scanLines, sceneLines);
			mover.setIntersectionPoints(points);

		}

	}

	private void clearCanvas() {

		GraphicsContext gc = foregroundGraphicsContext;
		gc.clearRect(0, 0, foregroundCanvas.getWidth(), foregroundCanvas.getHeight());

	}

	private void paintScanLines(List<Mover> movers) {

		if (!Settings.get().isDrawScanLines())
			return;

		GraphicsContext gc = foregroundGraphicsContext;

		gc.setStroke(Color.BLUE.deriveColor(1, 1, 1, 0.3));
		gc.setFill(Color.BLUE);

		for (Mover mover : movers) {
			for (Line line : mover.getScanLines()) {
				drawLine(line);
			}
		}

	}

	private void paintScanShape(List<Mover> movers) {

		if (!Settings.get().isDrawShape())
			return;

		GraphicsContext gc = foregroundGraphicsContext;

		for (Mover mover : movers) {

			List<PVector> shapePoints = new ArrayList<>();
			shapePoints.addAll(mover.getIntersectionPoints());

			// if we don't have a full circle, we must start and close the shape
			// at the user's position
			if (mover.getSweepAngleRad() != Math.PI * 2) {
				shapePoints.add(0, mover.getLocation());
				shapePoints.add(mover.getLocation());
			}

			gc.setStroke(Color.GREEN);

			if (Settings.get().isGradientShapeFill()) {

				Color LIGHT_GRADIENT_START = Color.YELLOW.deriveColor(1, 1, 1, 0.5);
				Color LIGHT_GRADIENT_END = Color.TRANSPARENT;

				// TODO: don't use the center of the shape; instead calculate
				// the center depending on the user position
				RadialGradient gradient = new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE, new Stop(0, LIGHT_GRADIENT_START), new Stop(1, LIGHT_GRADIENT_END));
				gc.setFill(gradient);

				gc.setFill(gradient);

			} else {

				gc.setFill(Color.GREEN.deriveColor(1, 1, 1, 0.2));

			}

			int count = 0;
			gc.beginPath();
			for (PVector point : shapePoints) {
				if (count == 0) {
					gc.moveTo(point.x, point.y);
				} else {
					gc.lineTo(point.x, point.y);
				}
				count++;
			}
			gc.closePath();

			// stroke
			if (Settings.get().isShapeBorderVisible()) {
				gc.stroke();
			}

			// fill
			gc.fill();

		}

	}

	private void paintIntersectionPoints(List<Mover> movers) {

		if (!Settings.get().isDrawPoints())
			return;

		GraphicsContext gc = foregroundGraphicsContext;

		for (Mover mover : movers) {

			gc.setStroke(Color.RED);
			gc.setFill(Color.RED.deriveColor(1, 1, 1, 0.5));

			double w = 2;
			double h = w;
			for (PVector point : mover.getIntersectionPoints()) {
				gc.strokeOval(point.x - w / 2, point.y - h / 2, w, h);
				gc.fillOval(point.x - w / 2, point.y - h / 2, w, h);
			}

		}
	}

	private void paintEnvironment() {

		if (!Settings.get().isEnvironmentVisible())
			return;

		GraphicsContext gc = foregroundGraphicsContext;

		// room floor
		gc.setFill(Color.LIGHTGREY.deriveColor(1, 1, 1, 0.3));
		for (Bounds bounds : levelGenerator.getRoomDimensions()) {
			gc.fillRect(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
		}

		// scene lines
		gc.setStroke(Color.BLACK);
		gc.setFill(Color.BLACK);

		for (Line line : sceneLines) {
			drawLine(line);
		}

	}

	private void paintOnCanvas() {

		// clear canvas
		clearCanvas();

		paintEnvironment();

		paintScanLines(players);
		paintScanLines(enemies);

		// draw intersection shape
		paintScanShape(players);
		paintScanShape(enemies);

		// draw intersection points
		paintIntersectionPoints(players);
		paintIntersectionPoints(enemies);

	}

	private void drawLine(Line line) {

		GraphicsContext gc = foregroundGraphicsContext;

		gc.strokeLine(line.getStart().x, line.getStart().y, line.getEnd().x, line.getEnd().y);

	}

	/**
	 * Listeners for keyboard, mouse
	 */
	private void addInputListeners() {

		// capture mouse position
		scene.addEventFilter(MouseEvent.ANY, e -> {

			mouseStatus.setX(e.getX());
			mouseStatus.setY(e.getY());
			mouseStatus.setPrimaryButtonDown(e.isPrimaryButtonDown());
			mouseStatus.setSecondaryButtonDown(e.isSecondaryButtonDown());

		});

	}

	private void paintGrid(Color color) {

		double width = backgroundCanvas.getWidth();
		double height = backgroundCanvas.getHeight();

		double horizontalCellCount = Settings.get().getHorizontalCellCount();
		double cellSize = width / horizontalCellCount;
		double verticalCellCount = height / cellSize;

		backgroundGraphicsContext.setStroke(color);
		backgroundGraphicsContext.setLineWidth(1);

		// horizontal grid lines
		for (double row = 0; row < height; row += cellSize) {

			double y = (int) row + 0.5;
			backgroundGraphicsContext.strokeLine(0, y, width, y);

		}

		// vertical grid lines
		for (double col = 0; col < width; col += cellSize) {

			double x = (int) col + 0.5;
			backgroundGraphicsContext.strokeLine(x, 0, x, height);

		}

		// highlight cell in which the mouse cursor resides
		if (Settings.get().isHighlightGridCell()) {

			Color highlightColor = Color.LIGHTBLUE;

			int col = (int) (horizontalCellCount / width * mouseStatus.getX());
			int row = (int) (verticalCellCount / height * mouseStatus.getY());

			backgroundGraphicsContext.setFill(highlightColor);
			backgroundGraphicsContext.fillRect(col * cellSize, row * cellSize, cellSize, cellSize);

		}

	}

	/**
	 * Listeners for settings changes
	 */
	private void addSettingsListeners() {

		// particle size
		Settings.get().horizontalCellCountProperty().addListener((ChangeListener<Number>) (observable, oldValue, newValue) -> System.out.println("Horizontal cell count: " + newValue));

		Settings.get().lineCountProperty().addListener((ChangeListener<Number>) (observable, oldValue, newValue) -> createObjects());
		Settings.get().roomIterationsProperty().addListener((ChangeListener<Number>) (observable, oldValue, newValue) -> createObjects());
	}

	/**
	 * Context menu for the canvas
	 * 
	 * @param node
	 */
	public void addCanvasContextMenu(Node node) {

		MenuItem menuItem;

		// create context menu
		ContextMenu contextMenu = new ContextMenu();

		// add custom menu item
		menuItem = new MenuItem("Menu Item");
		menuItem.setOnAction(e -> System.out.println("Clicked"));
		contextMenu.getItems().add(menuItem);

		// context menu listener
		node.setOnMousePressed(event -> {
			if (event.isSecondaryButtonDown()) {
				contextMenu.show(node, event.getScreenX(), event.getScreenY());
			}
		});
	}

	public static void main(String[] args) {
		launch(args);
	}

}
