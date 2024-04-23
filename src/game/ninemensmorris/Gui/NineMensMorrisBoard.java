package game.ninemensmorris.Gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

import game.ninemensmorris.Models.BoardState;
import game.ninemensmorris.Models.Move;

public class NineMensMorrisBoard extends JPanel {
	private static final long serialVersionUID = 2961261317989680041L;

	private BoardState board;
	private int positionSelected;
	private boolean millFormed;
	private Move move;
	private MoveExecutorCallback moveExecutor;
	private boolean doMakeMove;
	private boolean showIllustration; // Flag to determine whether to show the illustration or not
	int ii = 1;

	
	public NineMensMorrisBoard() {
		addMouseListener(new Controller());
		addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateIllustrationVisibility();
            }
        });

	}

	    // Method to update the visibility of the illustration based on the component's size
		private void updateIllustrationVisibility() {
			int minWidthForIllustration = 800; // Adjust this value as needed
			showIllustration = getWidth() >= minWidthForIllustration;
			repaint();
		}
	
	public void setBoard(BoardState board, MoveExecutorCallback moveExecutor) {
		this.board = board;
		this.positionSelected = -1;
		this.millFormed = false;
		this.move = null;
		this.moveExecutor = moveExecutor;
		this.doMakeMove = false;

		repaint();
	}
	
	public void makeMove() {
		this.doMakeMove = true;
		System.out.println("Inside the make move for the " + ii++ + "time");
		System.out.println(doMakeMove);
	}
	
	Point getPositionCoords(int position) {
		Point result = new Point();

		int margin = 120;
		int width = getSize().width - 2 * margin;
		int height = getSize().height - 2 * margin;
		int metric = Math.min(width, height);
		int positionSpace = metric / 6;
		
		int row = position / 3;
		if (row < 3) {
			result.x = row * positionSpace + (position % 3) * (metric - 2 * row * positionSpace) / 2;
			result.y = row * positionSpace;
		} else if (row == 3) {
			result.x = (position % 3) * positionSpace;
			result.y = row * positionSpace;
		} else {
			Point point = getPositionCoords(23 - position);
			point.x -= margin;
			point.y -= margin;
			result.x = metric - point.x;
			result.y = metric - point.y;
		}
		
		result.x += margin;
		result.y += margin;
		
		return result;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;

    // Set the color for the background rectangle
	g2.setColor(new Color(0x26, 0x27, 0x28)); // Color #DAA06D
	
	g2.fillRect(0, 0, getWidth(), getHeight());

		g2.setStroke(new BasicStroke(10));
		g2.setColor(Color.white); // Set the color for the grid lines
		
		
		for (int i = 0; i < 24; i++) {
			for (int j : BoardState.POSITION_TO_NEIGHBOURS.get(i)) {
				Point start = getPositionCoords(i);
				Point end = getPositionCoords(j);
				g2.drawLine(start.x, start.y, end.x, end.y);

			}
		}

		g2.setStroke(new BasicStroke(1)); // Adjust the thickness of the ovals as needed
		AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f); // Adjust the opacity (0.5f) as needed

		g2.setComposite(alphaComposite);
		for (int i = 0; i < 24; i++) {
			Point coords = getPositionCoords(i);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g2.setColor(Color.BLACK); // Set the color for the ovals
			g2.fillOval(coords.x - 5, coords.y - 5, 10, 10); // Adjust the size of the ovals as needed
			
		
		}

		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));


		for (int i = 0; i < 24; i++) {
			if (move != null && i == move.getFromPosition()) {
				continue;
			}

			if (board.getPositionState(i) != 0 || (move != null && move.getToPosition() == i)) {
				if (positionSelected == i) {
					g.setColor(Color.RED);
				} else if (board.getPositionState(i) == 1
						|| (move != null && move.getToPosition() == i && board.getCurrentPlayer() == 0)) {
					g.setColor(Color.WHITE);
				} else {
					g.setColor(Color.BLACK);
				}
				
				Point coords = getPositionCoords(i);
				g.fillOval(coords.x - 20, coords.y - 20, 40, 40);
				
				g.setColor(Color.BLACK);
				g.drawOval(coords.x - 20, coords.y - 20, 40, 40);
			}
		}

		if (showIllustration) {
            // Call the method to draw the illustration
            drawIllustration(g2);
        }
	}

	private void drawIllustration(Graphics2D g2) {
		// Load the image from file
		Image illustrationImage = Toolkit.getDefaultToolkit().getImage("src/game/ninemensmorris/gui/title3d2.png");
		// Calculate the position and size of the illustration
		int illustrationWidth = getWidth() / 3; // Adjust the width of the illustration as needed
		int illustrationHeight = getHeight()/2; // Use the full height of the panel
		int illustrationX = (int) ((getWidth() - illustrationWidth)* 0.8); // Center the illustration horizontally
		int illustrationY = (int) ((getHeight() - illustrationHeight) * 0.5);
	
		// Draw the image
		g2.drawImage(illustrationImage, illustrationX, illustrationY, illustrationWidth, illustrationHeight, this);
	}
	
	private class Controller extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (!doMakeMove || board.hasCurrentPlayerLost()) {
				System.out.println("Lost Or False");

				return;
			}
			
			int x = e.getX();
			int y = e.getY();


			
			for (int i = 0; i < 24; i++) {
				Point coords = getPositionCoords(i);
				
				if (coords.x - 20 <= x && x <= coords.x + 20
						&& coords.y - 20 <= y && y <= coords.y + 20) {
					if (millFormed) {
						if (board.getPositionState(i) == board.getOtherPlayer() + 1) {
							boolean areAllOtherPlayerPiecesFromMill = board.areAllPiecesFromMill(board.getOtherPlayer());

							if (areAllOtherPlayerPiecesFromMill || !board.doesPieceCompleteMill(-1, i, board.getOtherPlayer())) {
								move = new Move(move.getFromPosition(), move.getToPosition(), i);
								if (board.isMoveValid(move)) {
									moveExecutor.makeMove(move);
									move = null;
									millFormed = false;
									doMakeMove = true;
								}									
							}
						}
					} else {
						if (board.getPositionState(i) == 0) {
							if (positionSelected == -1) {
								move = new Move(i);
							} else {
								move = new Move(positionSelected, i);
							}
						} else if (board.getPositionState(i) == board.getCurrentPlayer() + 1) {
							if (positionSelected == -1) {
								positionSelected = i;
							} else if (positionSelected == i) {
								positionSelected = -1;
							} else {
								positionSelected = i;
							}
						}
						
						if (move != null) {
							if (board.isMoveValid(move)) {
								positionSelected = -1;
								if (board.doesPieceCompleteMill(move.getFromPosition(), move.getToPosition(), board.getCurrentPlayer())) {
									millFormed = true;
								} else {
									moveExecutor.makeMove(move);
									move = null;
									doMakeMove = true;
								}
							} else {
								move = null;
							}
						}
					}

					repaint();
					
					break;
				}
			}
		}
	}


}
