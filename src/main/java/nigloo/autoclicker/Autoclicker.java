package nigloo.autoclicker;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.jnativehook.mouse.NativeMouseEvent;
import org.jnativehook.mouse.NativeMouseListener;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import nigloo.autoclicker.Command.Type;
import nigloo.autoclicker.component.CommandCell;
import nigloo.autoclicker.component.EditableIntegerSpinner;
import nigloo.autoclicker.component.ScreenCapturer;

public class Autoclicker extends Application
{
	private final Robot robot = new Robot();
	
	private boolean logClick;
	private NativeMouseListener logClickListener;
	
	private ListView<Command> commandsView;
	
	private Button selectStartStopKeyButton;
	private boolean selectingHotkey;
	
	private Button startStopButton;
	private int startStopKeyCode;
	
	private CommandsExecution commandsExecution = null;
	
	public static void main(String[] args)
	{
		Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
		logger.setLevel(Level.WARNING);

		// Don't forget to disable the parent handlers.
		logger.setUseParentHandlers(false);
		
		try {
			GlobalScreen.registerNativeHook();
		}
		catch (NativeHookException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		launch(args);
	}
	
	@Override
	public void start(Stage mainWindow) throws Exception
	{
		selectingHotkey = false;
		startStopKeyCode = NativeKeyEvent.VC_2;
		
		initUI(mainWindow);
		initGlobalListeners();
		
		commandsView.getItems().addAll(getHentaiClickerCommandsList());
		commandsView.getSelectionModel().select(0);
		
		mainWindow.show();
	}
	
	@Override
	public void stop() throws Exception {
		GlobalScreen.unregisterNativeHook();
	}

	private void initUI(Stage mainWindow)
	{
		// ----------------------------------
		// ---- Instantiate all controls ----
		// ----------------------------------
		
		commandsView = new ListView<>();
		
		Button addCommandButton = new Button();
		Button removeCommandButton = new Button();
		
		
		ComboBox<Type> commandTypeInput = new ComboBox<>(FXCollections.observableArrayList(Type.values()));
		
		ToggleGroup positionToggleGroup = new ToggleGroup();
		RadioButton currentPosition = new RadioButton();
		currentPosition.setToggleGroup(positionToggleGroup);
		RadioButton customPosition = new RadioButton();
		customPosition.setToggleGroup(positionToggleGroup);
		
		EditableIntegerSpinner positionInputX = new EditableIntegerSpinner();
		EditableIntegerSpinner positionInputY = new EditableIntegerSpinner();
		Button selectPositionButton = new Button();
		
		EditableIntegerSpinner delayInput = new EditableIntegerSpinner();
		
		EditableIntegerSpinner repetitionInput = new EditableIntegerSpinner();
		
		Button selectScreenAreaButton = new Button();
		ImageView selectedScreenAreaView = new ImageView();
		EditableIntegerSpinner matchPercentInput = new EditableIntegerSpinner();
		
		
		selectStartStopKeyButton = new Button();
		startStopButton = new Button();
		
		
		// ---------------------------------
		// ---- Initialize all controls ----
		// ---------------------------------
		
		// ---- Commands list view ----
		
		commandsView.setItems(FXCollections.observableArrayList());
		commandsView.setCellFactory(lv -> new CommandCell());
		commandsView.getSelectionModel().selectedItemProperty().addListener((observable, oldCommand, newCommand) ->
		{
			if (newCommand != null)
			{
				commandTypeInput.setValue(newCommand.getType());
				if (newCommand.getPosition() != null)
				{
					positionInputX.setValue(newCommand.getPosition().x);
					positionInputY.setValue(newCommand.getPosition().y);
					positionToggleGroup.selectToggle(customPosition);
				}
				else
				{
					positionToggleGroup.selectToggle(currentPosition);
				}
				delayInput.setValue(newCommand.getDelay());
				repetitionInput.setValue(newCommand.getRepetition());
				selectedScreenAreaView.setImage(newCommand.getScreenCapture()==null?null:newCommand.getScreenCapture().getFxImage());
				matchPercentInput.setValue(newCommand.getMatchPercent());
				
				removeCommandButton.setDisable(false);
			}
			else
			{
				commandTypeInput.setValue(Type.LEFT_CLICK);
				positionToggleGroup.selectToggle(currentPosition);
				positionInputX.setDisable(true);
				positionInputY.setDisable(true);
				delayInput.setValue(1000);
				repetitionInput.setValue(1);
				selectedScreenAreaView.setImage(null);
				matchPercentInput.setValue(90);
				
				removeCommandButton.setDisable(true);
			}
		});
		
		addCommandButton.setText("New command");
		addCommandButton.setOnAction(event ->
		{
			int index = commandsView.getItems().size();
			if (commandsView.getSelectionModel().isEmpty() == false)
				index = commandsView.getSelectionModel().getSelectedIndex()+1;
			
			Command newCommand = new Command(Type.LEFT_CLICK, null, 1000, 1);
			commandsView.getItems().add(index, newCommand);
			commandsView.getSelectionModel().select(index);
			commandsView.scrollTo(index);
		});
		
		removeCommandButton.setText("Remove command");
		removeCommandButton.setOnAction(event ->
		{
			int index = commandsView.getSelectionModel().getSelectedIndex();
			
			commandsView.getItems().remove(index);
			commandsView.refresh();
			commandsView.getSelectionModel().select(index);
		});
		
		
		// ---- Selected command edition inputs ----
		
		commandTypeInput.valueProperty().addListener((observable, oldValue, newValue) ->
		{
			Command selectedCommand = commandsView.getSelectionModel().getSelectedItem();
			if (selectedCommand == null)
				return;
			
			selectedCommand.setType(newValue);
			commandsView.refresh();
		});
		
		currentPosition.setToggleGroup(positionToggleGroup);
		customPosition.setToggleGroup(positionToggleGroup);
		positionToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) ->
		{
			Command selectedCommand = commandsView.getSelectionModel().getSelectedItem();
			if (selectedCommand == null)
				return;
			
			if (newValue == currentPosition)
			{
				selectedCommand.setPosition(null);
				positionInputX.setDisable(true);
				positionInputY.setDisable(true);
			}
			else
			{
				selectedCommand.setPosition(new ScreenPosition(positionInputX.getValue(), positionInputY.getValue()));
				positionInputX.setDisable(false);
				positionInputY.setDisable(false);
			}
			commandsView.refresh();
		});
		
		positionInputX.valueProperty().addListener((observable, oldValue, newValue) ->
		{
			Command selectedCommand = commandsView.getSelectionModel().getSelectedItem();
			if (selectedCommand == null)
				return;

			selectedCommand.getPosition().x = newValue;
			commandsView.refresh();
		});
		positionInputY.valueProperty().addListener((observable, oldValue, newValue) ->
		{
			Command selectedCommand = commandsView.getSelectionModel().getSelectedItem();
			if (selectedCommand == null)
				return;

			selectedCommand.getPosition().y = newValue;
			commandsView.refresh();
		});
		selectPositionButton.setText("⌖");
		selectPositionButton.setOnAction(e ->
		{
			ScreenPosition position = new ScreenCapturer(robot).selectScreenPosition();
			positionInputX.setValue(position.x);
			positionInputY.setValue(position.y);
		});
		
		delayInput.valueProperty().addListener((observable, oldValue, newValue) ->
		{
			Command selectedCommand = commandsView.getSelectionModel().getSelectedItem();
			if (selectedCommand == null)
				return;

			selectedCommand.setDelay(newValue);
			commandsView.refresh();
		});
		
		repetitionInput.valueProperty().addListener((observable, oldValue, newValue) ->
		{
			Command selectedCommand = commandsView.getSelectionModel().getSelectedItem();
			if (selectedCommand == null)
				return;

			selectedCommand.setRepetition(newValue);
			commandsView.refresh();
		});
		
		selectScreenAreaButton.setText("Select screen area");
		selectScreenAreaButton.setOnAction(e ->
		{
			Command selectedCommand = commandsView.getSelectionModel().getSelectedItem();
			if (selectedCommand == null)
				return;
			
			mainWindow.hide();
			ScreenCapture capture = new ScreenCapturer(robot).capture();
			mainWindow.show();
			
			if (capture == null) {
				selectedScreenAreaView.setImage(null);
				return;
			}
			
			selectedScreenAreaView.setImage(capture.getFxImage());
			selectedCommand.setScreenCapture(capture);
			
			/*
			try {
				Image img = capture.getFxImage();
				int width = (int) img.getWidth();
				int height = (int) img.getHeight();
				
				byte[] pixels = new byte[width * height * 4];
				int scanlineStride = width * 4;
				img.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getByteBgraInstance(), pixels, 0, scanlineStride);
			
			
				FileOutputStream fos = new FileOutputStream("C:\\Users\\Nigloo\\Desktop\\capture.txt");
				fos.write((capture.getRegion().getMinX()+"\n").getBytes(StandardCharsets.ISO_8859_1));
				fos.write((capture.getRegion().getMinY()+"\n").getBytes(StandardCharsets.ISO_8859_1));
				fos.write((capture.getRegion().getWidth()+"\n").getBytes(StandardCharsets.ISO_8859_1));
				fos.write((capture.getRegion().getHeight()+"\n").getBytes(StandardCharsets.ISO_8859_1));
				fos.write(Base64.getEncoder().encode(pixels));
				fos.close();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			*/
			
			/*
			try {
				ScreenPosition realSize = ScreenPosition.fromFx(new Point2D(capture.getFxImage().getWidth(), capture.getFxImage().getHeight()));
				BufferedImage scaledImage = new BufferedImage(realSize.x, realSize.y, BufferedImage.TYPE_INT_RGB);
				scaledImage.getGraphics().drawImage(SwingFXUtils.fromFXImage(capture.getFxImage(), null).getScaledInstance(realSize.x, realSize.y, java.awt.Image.SCALE_DEFAULT),
						0, 0, null);
				ImageIO.write(scaledImage, "png", new File("C:\\Users\\Nigloo\\Desktop\\capture.png")) ;
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			//*/
		});
		
		matchPercentInput.setMin(0);
		matchPercentInput.setMax(100);
		matchPercentInput.valueProperty().addListener((observable, oldValue, newValue) ->
		{
			Command selectedCommand = commandsView.getSelectionModel().getSelectedItem();
			if (selectedCommand == null)
				return;

			selectedCommand.setMatchPercent(newValue);
			//commandsView.refresh();
		});
		
		
		// ---- Start/stop Button ----
		
		selectStartStopKeyButton.setText("Select hotkey");
		selectStartStopKeyButton.setOnAction(e -> {
			selectingHotkey = true;
			selectStartStopKeyButton.setText("Press a key...");
			selectStartStopKeyButton.setDisable(true);
		});
		
		startStopButton.setOnAction(e -> startStopCommands());
		updateStartStopButton();
		
		// -----------------------------
		// ---- Layout all controls ----
		// -----------------------------
		
		
		HBox buttonPane = new HBox();
		buttonPane.setSpacing(10);
		buttonPane.getChildren().add(addCommandButton);
		HBox.setHgrow(addCommandButton, Priority.ALWAYS);
		addCommandButton.setMaxWidth(Double.MAX_VALUE);
		buttonPane.getChildren().add(removeCommandButton);
		HBox.setHgrow(removeCommandButton, Priority.ALWAYS);
		removeCommandButton.setMaxWidth(Double.MAX_VALUE);
		VBox listViewPane = new VBox();
		listViewPane.setPadding(new Insets(10));
		listViewPane.setSpacing(10);
		commandsView.setMinWidth(320);
		listViewPane.getChildren().add(commandsView);
		listViewPane.getChildren().add(buttonPane);
		
		
		GridPane editCommandGridPane = new GridPane();
		editCommandGridPane.setHgap(10);
		editCommandGridPane.setVgap(5);
		editCommandGridPane.getColumnConstraints().add(new ColumnConstraints(Region.USE_COMPUTED_SIZE));
		editCommandGridPane.getColumnConstraints().add(new ColumnConstraints(Region.USE_COMPUTED_SIZE));
		editCommandGridPane.addColumn(0, new Label("Command"));
		editCommandGridPane.addColumn(1, commandTypeInput);
		HBox positionInputPane = new HBox(10d);
		currentPosition.setText("Current position");
		positionInputPane.getChildren().add(currentPosition);
		positionInputPane.getChildren().add(customPosition);
		positionInputPane.getChildren().add(new Label("X"));
		positionInputX.setMaxWidth(70);
		positionInputX.getEditor().setAlignment(Pos.BASELINE_RIGHT);
		positionInputPane.getChildren().add(positionInputX);
		positionInputPane.getChildren().add(new Label("Y"));
		positionInputY.setMaxWidth(70);
		positionInputY.getEditor().setAlignment(Pos.BASELINE_RIGHT);
		positionInputPane.getChildren().add(positionInputY);
		positionInputPane.setAlignment(Pos.BASELINE_LEFT);
		positionInputPane.getChildren().add(selectPositionButton);
		editCommandGridPane.addColumn(0, new Label("Position"));
		editCommandGridPane.addColumn(1, positionInputPane);
		editCommandGridPane.addColumn(0, new Label("Delay"));
		editCommandGridPane.addColumn(1, delayInput);
		editCommandGridPane.addColumn(0, new Label("Repetition"));
		editCommandGridPane.addColumn(1, repetitionInput);
		
		GridPane repeatCommandOnScreenScanPane = new GridPane();
		repeatCommandOnScreenScanPane.setHgap(10);
		repeatCommandOnScreenScanPane.setVgap(5);
		repeatCommandOnScreenScanPane.getColumnConstraints().add(new ColumnConstraints(Region.USE_COMPUTED_SIZE));
		repeatCommandOnScreenScanPane.getColumnConstraints().add(new ColumnConstraints(Region.USE_COMPUTED_SIZE));
		repeatCommandOnScreenScanPane.addColumn(0, new Label("Match"));
		repeatCommandOnScreenScanPane.addColumn(1, matchPercentInput);
		repeatCommandOnScreenScanPane.addColumn(0, selectScreenAreaButton);
		selectedScreenAreaView.setPreserveRatio(true);
		repeatCommandOnScreenScanPane.addColumn(1, selectedScreenAreaView);
		
		VBox editCommandPane = new VBox();
		editCommandPane.setPadding(new Insets(10));
		editCommandPane.setSpacing(10);
		editCommandPane.getChildren().add(editCommandGridPane);
		editCommandPane.getChildren().add(repeatCommandOnScreenScanPane);
		
		
		
		HBox bottomPane = new HBox();
		bottomPane.setPadding(new Insets(10));
		bottomPane.setSpacing(10);
		bottomPane.setPrefHeight(100);
		bottomPane.getChildren().add(selectStartStopKeyButton);
		HBox.setHgrow(selectStartStopKeyButton, Priority.ALWAYS);
		selectStartStopKeyButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		bottomPane.getChildren().add(startStopButton);
		HBox.setHgrow(startStopButton, Priority.ALWAYS);
		startStopButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		
		
		BorderPane borderPane = new BorderPane();
		borderPane.setLeft(listViewPane);
		borderPane.setCenter(editCommandPane);
		borderPane.setBottom(bottomPane);
		
		
		mainWindow.setScene(new Scene(borderPane));
	}
	
	private void initGlobalListeners()
	{
		logClick = false;
		logClickListener = new NativeMouseListener()
		{
			@Override public void nativeMouseReleased(NativeMouseEvent nativeEvent) {}
			@Override public void nativeMouseClicked(NativeMouseEvent nativeEvent) {}
			
			@Override
			public void nativeMousePressed(NativeMouseEvent nativeEvent)
			{
				if (nativeEvent.getButton() == NativeMouseEvent.BUTTON1)
					System.out.println("Click "+new ScreenPosition(nativeEvent.getX(), nativeEvent.getY()));
			}
		};
		
		GlobalScreen.addNativeKeyListener(new NativeKeyListener()
		{
			@Override public void nativeKeyTyped(NativeKeyEvent nativeEvent) {}
			@Override public void nativeKeyReleased(NativeKeyEvent nativeEvent) {}
			
			@Override
			public void nativeKeyPressed(NativeKeyEvent nativeEvent)
			{
				if (selectingHotkey)
				{
					startStopKeyCode = nativeEvent.getKeyCode();
					selectingHotkey = false;
					Platform.runLater(() ->
					{
						selectStartStopKeyButton.setText("Select hotkey");
						selectStartStopKeyButton.setDisable(false);
						updateStartStopButton();
					});
					
					return;
				}
				
				if (nativeEvent.getKeyCode() == NativeKeyEvent.VC_1)
				{
					if (logClick)
						GlobalScreen.removeNativeMouseListener(logClickListener);
					else
						GlobalScreen.addNativeMouseListener(logClickListener);
					
					logClick = !logClick;
				}
				
				if (nativeEvent.getKeyCode() == startStopKeyCode)
					startStopCommands();
				
				if (logClick)
					System.out.println(NativeKeyEvent.getKeyText(nativeEvent.getKeyCode()));
			}
		});
	}
	
	private void startStopCommands()
	{
		if (commandsExecution == null)
		{
			commandsExecution = new CommandsExecution(robot, commandsView.getItems());
			Platform.runLater(this::updateStartStopButton);
			Platform.runLater(commandsExecution);
		}
		else
		{
			commandsExecution.requestStop();
			commandsExecution = null;
			Platform.runLater(this::updateStartStopButton);
		}
	}
	
	private void updateStartStopButton()
	{
		if (commandsExecution == null)
		{
			startStopButton.setText("Start ("+NativeKeyEvent.getKeyText(startStopKeyCode)+")");
			startStopButton.setDisable(false);
		}
		else
		{
			startStopButton.setText("Stop ("+NativeKeyEvent.getKeyText(startStopKeyCode)+")");
			startStopButton.setDisable(true);
		}
		
	}
	
	private static List<Command> getFapCEOCommandsList()
	{
		Command commandMaxGirlButton = new Command(Command.Type.LEFT_CLICK, new ScreenPosition(1654, 1137), 500, 1);
		
		List<Command> commands = new ArrayList<>();
		// Sell compagny
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition( 975,   41), 500, 1));
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition(1141,  733), 500, 1));
		
		// Wait saving
		try {
			byte[] buff = new byte[1024];
			int read;
			ByteArrayOutputStream base64Pixels = new ByteArrayOutputStream();
			InputStream is = Autoclicker.class.getResourceAsStream("img64.txt");
			while ((read = is.read(buff)) != -1) base64Pixels.write(buff, 0, read);
			
			byte[] pixels = Base64.getDecoder().decode(base64Pixels.toByteArray());
			double x = 556.8;
			double y = 378.4;
			int w = 131;
			int h = 200;
			int scanlineStride = w * 4;
			WritableImage image = new WritableImage(w, h);
			image.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getByteBgraInstance(), pixels, 0, scanlineStride);
			ScreenCapture capture = new ScreenCapture(new Rectangle2D(x, y, w, h), image);
			
			Command waitSavingCmd = new Command(Command.Type.WAIT, null, 200, 50);
			waitSavingCmd.setScreenCapture(capture);
			waitSavingCmd.setMatchPercent(80);
			commands.add(waitSavingCmd);
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Buy girls
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition(1013,  879), 500, 1));
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition(1449,   84), 500, 1));
		
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition(1013,  879), 500, 1));
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition(1449,   84), 500, 1));
		
		// Max girls Cloé (last girl, twice for security)
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition( 997,  853), 300, 1));
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition(1654, 1137), 300, 1));
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition( 997,  853), 300, 1));
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition(1654, 1137),  50, 200));
		
		// Level up other girls
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition(1245,  727), 300, 1));
		commands.add(commandMaxGirlButton);
		
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition(1474,  578), 300, 1));
		commands.add(commandMaxGirlButton);
		
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition( 804,  744), 300, 1));
		commands.add(commandMaxGirlButton);
		
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition(1032,  584), 300, 1));
		commands.add(commandMaxGirlButton);
		
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition(1271,  460), 300, 1));
		commands.add(commandMaxGirlButton);
		
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition( 602,  624), 300, 1));
		commands.add(commandMaxGirlButton);
		
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition( 824,  484), 300, 1));
		commands.add(commandMaxGirlButton);
		
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition(1070,  324), 300, 1));
		commands.add(commandMaxGirlButton);
		
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition( 401,  476), 300, 1));
		commands.add(commandMaxGirlButton);
		
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition( 608,  339), 300, 1));
		commands.add(commandMaxGirlButton);
		
		// Chest
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition(1664,  805), 50, 40));
		
		return commands;
	}
	
	
	private static List<Command> getHentaiClickerCommandsList()
	{
		List<Command> commands = new ArrayList<>();
		
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition(1060,  980),  50, 10));
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition( 730,  700), 100,  1));
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition(1060,  980),  50, 10));
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition( 730,  830), 100,  1));
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition(1060,  980),  50, 10));
		commands.add(new Command(Command.Type.LEFT_CLICK, new ScreenPosition( 730,  970), 100,  1));
		
		return commands;
	}
}
