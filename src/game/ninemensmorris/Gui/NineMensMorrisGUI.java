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
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.Timer;

import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
    String difficulty = "Easy";
    private Timer player1Timer;
    private Timer player2Timer;
    private int currentPlayerTime;
    private boolean gameEnded = false; // Track if the game has ended

    private JToggleButton modeToggle;

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

            if (pvpMode && gameEnded) {
                return;
            }
            
            currentGame.makeMove(move);
            boardPanel.repaint();

        
            if (currentGame.hasCurrentPlayerLost()) {
                if (currentPlayer == 1) {
                    statusLabel.setText("Player 2 won!");
                } else {
                    statusLabel.setText("Player 1 won!");
                }
            } else {
                // Toggle between players

                
                currentPlayer = 1 - currentPlayer;
                if (pvpMode) {
                    currentPlayerTime = 30;
                    gameEnded = false;
                    initializeTimers(); // Ensure timers are initialized
                    if (currentPlayer == 0) {
                        player1Timer.start();
                    } else {
                        player2Timer.start();
                    }
                }
                
                statusLabel.setText("Player " + (currentPlayer + 1) + "'s move");
                boardPanel.makeMove();  
                // Allow the CPU to make a move in PvCPU mode
                if (currentPlayer == 1 && !pvpMode) { 
                    statusLabel.setText("Making A Move...");
                    int maxDepth = 5;
                    int maxTime = 5 * 1000;
                    switch (difficulty) {
                        case "Easy":
                            maxDepth = 5; 
                            maxTime = 5000;
                            break;
                        case "Medium":
                            maxDepth = 10; 
                            maxTime = 10000; 
                            break;
                        case "Hard":
                            maxDepth = 15;
                            maxTime = 15000;
                            break;
                        default:
                            maxDepth = 5; 
                            maxTime = 5000;
                            break;
                    }
                    
        
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

    private void customButtonp(JRadioButton button) {
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(100,  30)); // Set preferred size
        button.setBackground(Color.LIGHT_GRAY); // Set background color
        button.setBorder(new EmptyBorder(5, 10, 5, 10)); 
    }

    private synchronized void endGame(String winner) {
        // Stop both timers
        player1Timer.stop();
        player2Timer.stop();
        gameEnded = true; // Set gameEnded flag to true
        statusLabel.setText(winner + " won!");

        // Add any other end game logic here
    }

    private synchronized void updateStatusLabel() {
        statusLabel.setText("Player " + (currentPlayer + 1) + "'s move | Time left: " + currentPlayerTime + " seconds");
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
                NineMensMorrisBoard.lastremoved = null;
                startNewGame();
                if (pvpMode) {
                    gameEnded = false;
                    initializeTimers();
                }
                else {
                    if (player1Timer != null) {
                        player1Timer.stop();
                        player1Timer = null;
                    }
                    if (player2Timer != null) {
                        player2Timer.stop();
                        player2Timer = null;
                    }
                }

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
                
                // Panel for difficulty selection
                JPanel settingsPanel = new JPanel(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.gridwidth = 2;
                gbc.anchor = GridBagConstraints.WEST;
                settingsPanel.add(new JLabel("Select Difficulty:"), gbc);
        
                gbc.gridwidth = 1;
                gbc.gridy++;
                JRadioButton easyButton = new JRadioButton("Easy");
                customButtonp(easyButton);
                settingsPanel.add(easyButton, gbc);
        
                gbc.gridy++;
                JRadioButton mediumButton = new JRadioButton("Medium");
                customButtonp(mediumButton);
                settingsPanel.add(mediumButton, gbc);
        
                gbc.gridy++;
                JRadioButton hardButton = new JRadioButton("Hard");
                customButtonp(hardButton);
                settingsPanel.add(hardButton, gbc);
        
                ButtonGroup difficultyGroup = new ButtonGroup();
                difficultyGroup.add(easyButton);
                difficultyGroup.add(mediumButton);
                difficultyGroup.add(hardButton);


                if (difficulty == "Easy") {
                    easyButton.setSelected(true);

                    
                } else if (difficulty == "Medium") {
                    mediumButton.setSelected(true);

                }
                else {
                    hardButton.setSelected(true);

                }
                
                // Save button
                JButton saveButton = new JButton("Save");
                customButton(saveButton);
                saveButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // Update the difficulty based on user selection
                        if (easyButton.isSelected()) {
                            // Set the difficulty to Easy
                            difficulty = "Easy";
                        } else if (mediumButton.isSelected()) {
                            // Set the difficulty to Medium
                            difficulty = "Medium";
                        } else if (hardButton.isSelected()) {
                            // Set the difficulty to Hard
                            difficulty = "Hard";
                        }
                        settingsPopup.dispose(); // Close the settings pop-up window
                    }
                });
                gbc.gridy++;
                gbc.gridwidth = 2;
                gbc.anchor = GridBagConstraints.CENTER;
                settingsPanel.add(saveButton, gbc);
                
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
                try {
                    // Open the GitHub repository link in the default browser
                    Desktop.getDesktop().browse(new URI("https://github.com/777leed/nine-Mens-Morris"));
                } catch (IOException | URISyntaxException ex) {
                    ex.printStackTrace();
                    // Handle the exception if the link cannot be opened
                }
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
                JLabel creditsLabel = new JLabel("<html><center>This game was created by @Adnane.<br><br>©Copyright2024©</center></html>");
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
        if (pvpMode) {
            initializeTimers();
        }
        startNewGame();

    }

    private void initializeTimers() {
        // Dispose of existing timers, if any
        if (player1Timer != null) {
            player1Timer.stop();
            player1Timer = null;
        }
        if (player2Timer != null) {
            player2Timer.stop();
            player2Timer = null;
        }
    
        // Initialize player timers
        player1Timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentPlayerTime--;
                updateStatusLabel();
                if (currentPlayerTime <= 0) {
                    // Player 1 ran out of time
                    player1Timer.stop();
                    endGame("Player 2");
                }
            }
        });
    
        player2Timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentPlayerTime--;
                updateStatusLabel();
                if (currentPlayerTime <= 0) {
                    // Player 2 ran out of time
                    player2Timer.stop();
                    endGame("Player 1");
                }
            }
        });
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
