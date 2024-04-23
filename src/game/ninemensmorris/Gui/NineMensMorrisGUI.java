package game.ninemensmorris.Gui;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.SwingConstants;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;

import game.ninemensmorris.Algorithms.AlphaBetaPruning;
import game.ninemensmorris.Models.BoardState;
import game.ninemensmorris.Models.Move;

public class NineMensMorrisGUI extends JFrame {
    private static final long serialVersionUID = -514606427157467570L;
    private BoardState currentGame;
    private NineMensMorrisBoard boardPanel;
    private JPanel controls;
    private JButton newGameButton;
    private JButton quitButton;
    private JButton creditButton;
    private JButton githubButton;
    private JButton settingsButton;

    private JToggleButton modeToggle;

    private JTextField maxTimeTextField;
    private JTextField maxDepthTextField;
    private JLabel statusLabel;
    private AlphaBetaPruning solver;
    private volatile MoveExecutorCallback moveExecutor;
    private boolean pvpMode = true; // Default to PvP mode
    private int currentPlayer = 0; // 0 for player 1, 1 for player 2
    private int maxDepth = 30;
    private int maxTime = 15 * 1000;


    private class MoveExecutor implements MoveExecutorCallback {
        private boolean terminate = false;

        public synchronized void terminate() {
            this.terminate = true;
            solver.terminateSearch();
        }

        @Override
        public synchronized void makeMove(Move move) {
            if (terminate) {
                return;
            }

        
            currentGame.makeMove(move);
            boardPanel.repaint();
            System.out.println("not you");

        
            if (currentGame.hasCurrentPlayerLost()) {
                if (currentPlayer == 1) {
                    statusLabel.setText("Player 2 won!");
                } else {
                    statusLabel.setText("Player 1 won!");
                }
            } else {
                // Toggle between players
                currentPlayer = 1 - currentPlayer;
                statusLabel.setText("Player " + (currentPlayer + 1) + "'s move");
                boardPanel.makeMove();  
                // Allow the CPU to make a move in PvCPU mode
                if (currentPlayer == 1 && !pvpMode) { 
                    statusLabel.setText("Making move...");
        
                    int maxDepth = Integer.parseInt(maxDepthTextField.getText());
                    int maxTime = Integer.parseInt(maxTimeTextField.getText()) * 1000;
        
                    solver.setMaxDepth(maxDepth);
                    solver.setMaxTime(maxTime);
        
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Move move = solver.searchForBestMove();
                            MoveExecutor.this.makeMove(move);
                        }
                    }).start();
                }
            }
        }
        
    }

    private void startNewGame() {
        if (moveExecutor != null) {
            moveExecutor.terminate();
        }
        currentGame = new BoardState();
        moveExecutor = new MoveExecutor();
        boardPanel.setBoard(currentGame, moveExecutor);
        currentPlayer = 0; // Reset currentPlayer to 0 (player 1's turn)
        statusLabel.setText("Player 1's move");

        solver = new AlphaBetaPruning(currentGame, maxDepth, maxTime);
        boardPanel.makeMove();
    }
    

    public NineMensMorrisGUI() {
        super("Nine Men's Morris");

        boardPanel = new NineMensMorrisBoard();

        add(boardPanel, BorderLayout.CENTER);

        controls = new JPanel();
        controls.setLayout(new BorderLayout());

        // Panel for buttons and labels
        JPanel buttonsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;

        newGameButton = new JButton("New game");
        customButton(newGameButton);
        newGameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startNewGame();

            }
        });
        buttonsPanel.add(newGameButton, gbc);




        // Add mode toggle
        modeToggle = new JToggleButton("PvP");
		modeToggle.setPreferredSize(new Dimension(100, 30)); // Set preferred size
        modeToggle.setBackground(Color.LIGHT_GRAY); // Set background color;
        modeToggle.setBorder(new EmptyBorder(5, 10, 5, 10)); // Add a margin of 5 pixels top and bottom, 10 pixels left and right

        modeToggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pvpMode = !pvpMode;
                modeToggle.setText(pvpMode ? "PvP" : "PvCPU");
                // Additional logic to handle mode change if needed
            }
        });
        gbc.gridy++;
        buttonsPanel.add(modeToggle, gbc);

        quitButton = new JButton("Quit");
        customButton(quitButton);
        quitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        gbc.gridy++;
        buttonsPanel.add(quitButton, gbc);

        controls.add(buttonsPanel, BorderLayout.EAST);

        // Panel for labels
        // Panel for buttons and labels
        JPanel leftSideButtonsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        gbc2.gridx = 0;
        gbc2.gridy = 0;
        gbc2.weightx = 1.0;

        settingsButton = new JButton("Settings");
        customButton(settingsButton);
        settingsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Create and configure the settings pop-up window
                JFrame settingsPopup = new JFrame("Settings");
                settingsPopup.setSize(300, 200); // Set the size of the pop-up window
                settingsPopup.setLocationRelativeTo(null); // Center the pop-up window on the screen
                
                // Panel for text fields and save button
                JPanel settingsPanel = new JPanel();
                settingsPanel.setLayout(new FlowLayout());
                JTextField maxTimeTextField = new JTextField(3);
                maxTimeTextField.setText(Integer.toString(15)); // Initialize with current value
                settingsPanel.add(new JLabel("Max move time:"));
                settingsPanel.add(maxTimeTextField);
                
                JTextField maxDepthTextField = new JTextField(3);
                maxDepthTextField.setText(Integer.toString(30)); // Initialize with current value
                settingsPanel.add(new JLabel("Max searching depth:"));
                settingsPanel.add(maxDepthTextField);
                
                // Save button
                JButton saveButton = new JButton("Save");
                saveButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // Update the values with the user input
                        maxTime = Integer.parseInt(maxTimeTextField.getText());
                        maxDepth = Integer.parseInt(maxDepthTextField.getText());
                        settingsPopup.dispose(); // Close the settings pop-up window
                    }
                });
                settingsPanel.add(saveButton);
                
                // Add the settings panel to the content pane of the settings pop-up window
                settingsPopup.getContentPane().add(settingsPanel, BorderLayout.CENTER);
                
                // Make the settings pop-up window visible
                settingsPopup.setVisible(true);
            }
        });
        leftSideButtonsPanel.add(settingsButton, gbc2);

        githubButton = new JButton("GitHub Rep");
        customButton(githubButton);
        githubButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        gbc2.gridy++;
        leftSideButtonsPanel.add(githubButton, gbc2);


        creditButton = new JButton("Credit");
        customButton(creditButton);
        creditButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Create and configure the pop-up window
                JFrame popup = new JFrame("Credits");
                popup.setSize(300, 200); // Set the size of the pop-up window
                popup.setLocationRelativeTo(null); // Center the pop-up window on the screen
                
                // Create a JLabel with the credits text
                JLabel creditsLabel = new JLabel("<html><center>This game was created by @Adnane.</center></html>");
                creditsLabel.setHorizontalAlignment(SwingConstants.CENTER); // Center align the text
                
                // Add the JLabel to the content pane of the pop-up window
                popup.getContentPane().add(creditsLabel, BorderLayout.CENTER);
                
                // Make the pop-up window visible
                popup.setVisible(true);
            }
        });
        
        // Add action listener for credit button
        gbc2.gridy++;
        leftSideButtonsPanel.add(creditButton, gbc2);


        controls.add(leftSideButtonsPanel, BorderLayout.WEST);

        // Panel for text fields and status label
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new FlowLayout());
        settingsPanel.add(new JLabel("Status:"));
        statusLabel = new JLabel("Your move");
        settingsPanel.add(statusLabel);

        controls.add(settingsPanel, BorderLayout.CENTER);

        add(controls, BorderLayout.SOUTH);

        startNewGame();
    }

    /**
     * Customizes the appearance of the given button.
     */
    private void customButton(JButton button) {
        button.setPreferredSize(new Dimension(100, 30)); // Set preferred size
        button.setBackground(Color.LIGHT_GRAY); // Set background color
        button.setBorder(new EmptyBorder(5, 10, 5, 10)); // Add a margin of 5 pixels top and bottom, 10 pixels left and right
    }
    /**
     * @param args
     */
    public static void main(String[] args) {
        JFrame game = new NineMensMorrisGUI();

        game.setSize(600, 700); // Set the initial size
        game.setExtendedState(JFrame.MAXIMIZED_BOTH); // Maximize the window
        game.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        game.setVisible(true);
    }
}
