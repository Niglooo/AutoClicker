package nigloo.autoclicker;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;

public class ScreenCapture {
	
	private Rectangle2D fxRegion;
	private Image fxImage;
	
	public ScreenCapture(Rectangle2D fxRegion , Image fxImage) {
		this.fxRegion = fxRegion;
		this.fxImage = fxImage;
	}

	public Image getFxImage() {
		return fxImage;
	}
	
	public Rectangle2D getRegion()
	{
		return fxRegion;
	}
}
