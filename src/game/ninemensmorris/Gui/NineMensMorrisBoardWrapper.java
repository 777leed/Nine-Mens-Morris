package game.ninemensmorris.Gui;
import java.awt.BorderLayout;
import javax.swing.*;
import java.awt.Color;

import game.ninemensmorris.Models.BoardState;

public class NineMensMorrisBoardWrapper extends JPanel {
    private NineMensMorrisBoard board;

    public NineMensMorrisBoardWrapper() {
        setLayout(new BorderLayout());
        setBackground(new Color(0x26, 0x27, 0x28)); // Set background color to match the gray in the board

        // Create the NineMensMorrisBoard panel
        board = new NineMensMorrisBoard();

        // Add the NineMensMorrisBoard panel to a wrapper panel
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBackground(new Color(0x26, 0x27, 0x28)); // Set background color to match the gray in the board
        wrapperPanel.add(board, BorderLayout.CENTER);

        // Add the wrapper panel to this panel (NineMensMorrisBoardWrapper)
        add(wrapperPanel, BorderLayout.CENTER);
    }

    // Method to set the board state
    public void setBoardState(BoardState boardState, MoveExecutorCallback moveExecutor) {
        board.setBoard(boardState, moveExecutor);
    }

    // Method to make a move on the board
    public void makeMove() {
        board.makeMove();
    }
}
