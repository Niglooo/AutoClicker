package nigloo.autoclicker;

import java.util.List;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.Robot;
import nigloo.autoclicker.Command.Type;

public class CommandsExecution implements Runnable
{
	private final Robot robot;
	private final List<Command> commands;
	private int indexCommand;
	
	private boolean stop;

	public CommandsExecution(Robot robot, List<Command> commands) {
		this.robot = robot;
		this.commands = commands;
		this.indexCommand = 0;
		this.stop = false;
	}

	@Override
	public void run() {
		
		stop = false;
		
		while (!stop)
		{
			Command command = commands.get(indexCommand);
			
			boolean imageMatch = true;
			
			for (int i = 0 ; (i < command.getRepetition() && imageMatch) && !stop ; i++)
			{
				wait(command.getDelay());
				if (stop)
					return;
				
				if (command.getPosition() != null)
					robot.mouseMove(command.getPosition().toFx());
				
				MouseButton mouseButton = null;
				
				if (command.getType() == Type.LEFT_CLICK)
					mouseButton = MouseButton.PRIMARY;
				else if (command.getType() == Type.RIGHT_CLICK)
					mouseButton = MouseButton.SECONDARY;
				
				if (mouseButton !=null)
					robot.mouseClick(mouseButton);
				
				if (command.getScreenCapture() != null)
				{
					//long begin = System.currentTimeMillis();
					Image currentCapture = robot.getScreenCapture(null, command.getScreenCapture().getRegion());
					
					double match = compare(currentCapture, command.getScreenCapture().getFxImage());
					
					//long time = System.currentTimeMillis() - begin;
					//System.out.printf("Match: %.2f%% in %dms\n", match*100, time);
					
					imageMatch = match > (command.getMatchPercent() / 100d);
				}
			}
			
			indexCommand = (indexCommand+1) % commands.size();
		}
	}
	
	public void requestStop() {
		stop = true;
	}
	
	private void wait(int delay)
	{
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {}
	}
	
	private double compare(Image img1, Image img2)
	{
		if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight())
			throw new IllegalArgumentException("different sizes");
		
		int width = (int) img1.getWidth();
		int height = (int) img1.getHeight();
		
		byte[] pixels1 = new byte[width * height * 4];
		byte[] pixels2 = new byte[width * height * 4];
		int scanlineStride = width * 4;
		
		img1.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getByteBgraInstance(), pixels1, 0, scanlineStride);
		img2.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getByteBgraInstance(), pixels2, 0, scanlineStride);
		
		long sqDist = 0;
		
		for (int i = 0 ; i < width * height * 4 ; i += 4)
		{
			for (int channel = 0 ; channel < 3 ; channel++)// ignore alpha (channel==3)
			{
				int valuePx1 = pixels1[i+channel] & 0xff;
				int valuePx2 = pixels2[i+channel] & 0xff;
				
				int diff = valuePx1 - valuePx2;
				
				sqDist += diff * diff;
			}
		}

		double dist = Math.sqrt((double) sqDist);
		double maxDist = Math.sqrt((double) (255l*255 * width * height * 3));
		
		return 1 - (dist / maxDist);
	}
}
