package nigloo.autoclicker;

import org.apache.commons.lang3.StringUtils;

import javafx.geometry.Point2D;

public class ScreenPosition
{
	public static final double X_RATIO = 1919.0 / 1535.6;
	public static final double Y_RATIO = 1199.0 / 959.6;
	
	int x;
	int y;
	
	public ScreenPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	@Override
	public String toString() {
		return "["+StringUtils.leftPad(String.valueOf(x), 4)+", "+StringUtils.leftPad(String.valueOf(y), 4)+"]";
	}
	
	public Point2D toFx()
	{
		return new Point2D(x / X_RATIO, y / Y_RATIO);
	}
	
	static public ScreenPosition fromFx(Point2D p)
	{
		return new ScreenPosition((int)Math.round(p.getX() * X_RATIO), (int)Math.round(p.getY() * Y_RATIO));
	}
}
