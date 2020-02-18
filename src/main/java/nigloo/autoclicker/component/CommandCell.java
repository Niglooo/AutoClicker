package nigloo.autoclicker.component;

import org.apache.commons.lang3.StringUtils;

import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.text.Font;
import nigloo.autoclicker.Command;

public class CommandCell extends ListCell<Command>
{
	public CommandCell()
	{
		setFont(Font.font("monospaced"));

		setOnDragDetected(event -> {
			if (getItem() == null) {
				return;
			}

			int index = getListView().getSelectionModel().getSelectedIndex();

			Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
			ClipboardContent content = new ClipboardContent();
			content.putString(Integer.toString(index));
			//dragboard.setDragView(birdImages.get(items.indexOf(getItem())));
			dragboard.setContent(content);

			event.consume();
		});

		setOnDragOver(event -> {
			if (event.getGestureSource() != CommandCell.this && event.getDragboard().hasString()) {
				event.acceptTransferModes(TransferMode.MOVE);
			}

			event.consume();
		});

		setOnDragEntered(event -> {
			if (event.getGestureSource() != CommandCell.this && event.getDragboard().hasString()) {
				setOpacity(0.3);
			}
		});

		setOnDragExited(event -> {
			if (event.getGestureSource() != CommandCell.this && event.getDragboard().hasString()) {
				setOpacity(1);
			}
		});

		setOnDragDropped(event -> {
			if (getItem() == null) {
				return;
			}

			Dragboard db = event.getDragboard();
			boolean success = false;

			if (db.hasString()) {
				ObservableList<Command> items = getListView().getItems();
				int draggedIdx = Integer.parseInt(db.getString());
				int thisIdx = getIndex();
				
				Command draggedCommand = items.get(draggedIdx);
				items.remove(draggedIdx);
				items.add(thisIdx, draggedCommand);
				
				getListView().refresh();
				
				getListView().getSelectionModel().select(thisIdx);

				success = true;
			}
			event.setDropCompleted(success);

			event.consume();
		});

		setOnDragDone(DragEvent::consume);
	}
	
	@Override 
	protected void updateItem(Command command, boolean empty)
	{
		super.updateItem(command, empty);
		
		if (empty)
			setText(null);
		else
		{
			StringBuilder sb = new StringBuilder();
			sb.append(StringUtils.rightPad(String.valueOf(command.getType()), 11));
			sb.append(" ");
			
			switch (command.getType())
			{
				case LEFT_CLICK:
				case RIGHT_CLICK:
					String strPos = command.getPosition() != null ? command.getPosition().toString() : "current";
					sb.append(StringUtils.leftPad(strPos, 12));
					sb.append(" ");
					sb.append(StringUtils.leftPad(String.valueOf(command.getDelay()), 6));
					sb.append("ms ");
					sb.append(StringUtils.leftPad(String.valueOf(command.getRepetition()), 4));
					break;
				
				case WAIT:
					sb.append(StringUtils.leftPad("", 12));
					sb.append(" ");
					sb.append(StringUtils.leftPad(String.valueOf(command.getDelay()*command.getRepetition()), 6));
					sb.append("ms");
					break;
					
				default:
					throw new AssertionError("Forgot to handle display of "+command.getType());
			}
			
			setText(sb.toString());
		}
	}
}
