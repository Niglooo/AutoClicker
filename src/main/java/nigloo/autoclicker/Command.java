package nigloo.autoclicker;

public class Command {

	public enum Type {LEFT_CLICK, RIGHT_CLICK, WAIT}
	
	private Type type;
	private ScreenPosition position;
	private int delay;
	private int repetition;
	
	private ScreenCapture screenCapture;
	private int matchPercent;
	
	
	public Command(Type type, ScreenPosition position, int delay, int repetition) {
		this.type = type;
		this.position = position;
		this.delay = delay;
		this.repetition = repetition;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public ScreenPosition getPosition() {
		return position;
	}

	public void setPosition(ScreenPosition position) {
		this.position = position;
	}

	public int getDelay() {
		return delay;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

	public int getRepetition() {
		return repetition;
	}

	public void setRepetition(int repetition) {
		this.repetition = repetition;
	}

	public ScreenCapture getScreenCapture() {
		return screenCapture;
	}

	public void setScreenCapture(ScreenCapture screenCapture) {
		this.screenCapture = screenCapture;
	}

	public int getMatchPercent() {
		return matchPercent;
	}

	public void setMatchPercent(int matchPercent) {
		this.matchPercent = matchPercent;
	}
}
