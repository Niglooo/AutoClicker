package nigloo.autoclicker.component;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import nigloo.autoclicker.ScreenCapture;
import nigloo.autoclicker.ScreenPosition;

public class ScreenCapturer {

	private enum State {CAPTURING, SELECT_POS, NONE}
	
	private final Robot robot;
	private final Stage stage;
	private final Pane content;
	
	private State state;
	private Point2D selectionP1;
	private Rectangle selection;
	
	public ScreenCapturer(Robot robot)
	{
		this.robot = robot;
		content = new Pane();
		content.setBackground(Background.EMPTY);
		
		stage = new Stage(StageStyle.TRANSPARENT);
		stage.setFullScreen(true);
		stage.setFullScreenExitHint("");
		stage.setFullScreenExitKeyCombination(KeyCodeCombination.NO_MATCH);
		stage.setScene(new Scene(content, Color.rgb(0, 0, 0, 0.01)));
		
		state = null;
		selectionP1 = null;
		selection = new Rectangle();
		selection.setFill(Color.TRANSPARENT);
		selection.setStroke(Color.GRAY);
		selection.getStrokeDashArray().setAll(5d, 5d);
		selection.setStrokeType(StrokeType.OUTSIDE);
		selection.setStrokeWidth(1);
		
		
		content.setOnMouseDragged(e ->
		{
			if (state == State.CAPTURING && selectionP1 != null)
			{
				updateSelection(e);
			}
		});
		content.setOnMousePressed(e ->
		{
			if (state == State.CAPTURING)
			{
				selectionP1 = new Point2D(e.getScreenX(), e.getScreenY());
				
				updateSelection(e);
				
				content.getChildren().add(selection);
			}
			else if (state == State.SELECT_POS)
			{
				selectionP1 = new Point2D(e.getScreenX(), e.getScreenY());
				stage.close();
			}
		});
		content.setOnMouseReleased(e ->
		{
			if (state == State.CAPTURING)
			{
				stage.close();
			}
		});
		
		reset();
	}

	public ScreenCapture capture()
	{
		reset();
		
		state = State.CAPTURING;
		stage.showAndWait();
		
		if (state != State.CAPTURING)
			return null;
			
		content.getChildren().remove(selection);
		
		double width = Math.floor(selection.getWidth());
		double height = Math.floor(selection.getHeight());
		
		if (width <= 0 || height <= 0)
			return null;
		
		Rectangle2D region = new Rectangle2D(selection.getX(), selection.getY(), width, height);
		Image image = robot.getScreenCapture(null, region);
		return new ScreenCapture(region, image);
	}
	
	public ScreenPosition selectScreenPosition()
	{
		reset();
		
		state = State.SELECT_POS;
		stage.showAndWait();
		
		if (state != State.SELECT_POS)
			return null;
		
		return ScreenPosition.fromFx(selectionP1);
	}
	
	
	private void reset()
	{
		state = State.NONE;
		content.getChildren().remove(selection);
	}
	
	
	private void updateSelection(MouseEvent e)
	{
		selection.setX(Math.min(selectionP1.getX(), e.getScreenX()));
		selection.setY(Math.min(selectionP1.getY(), e.getScreenY()));
		selection.setWidth(Math.abs(selectionP1.getX() - e.getScreenX()));
		selection.setHeight(Math.abs(selectionP1.getY() - e.getScreenY()));
	}
}
