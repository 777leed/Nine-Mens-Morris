package game.ninemensmorris.Gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.border.EmptyBorder;

import game.ninemensmorris.Algorithms.AlphaBetaPruning;
import game.ninemensmorris.Models.BoardState;
import game.ninemensmorris.Models.Move;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;


class RoundedButtonUI extends BasicButtonUI {
    private int radius;

    public RoundedButtonUI(int radius) {
        this.radius = radius;
    }

    @Override
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        if (b.isContentAreaFilled()) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(b.getBackground().darker());
            g2.fillRoundRect(0, 0, b.getWidth(), b.getHeight(), radius, radius);
        }
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        paintBackground(g, b, b.getModel().isPressed() ? b.getBackground().darker() : b.getBackground());
        super.paint(g, c);
    }

    private void paintBackground(Graphics g, JComponent c, Color color) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), radius, radius);
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        AbstractButton button = (AbstractButton) c;
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
    }
}


public class NineMensMorrisGUI extends JFrame {
    private static final long serialVersionUID = -514606427157467570L;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JPanel mainMenuPanel;
    private JPanel gamePanel;

    private BoardState currentGame;
    private NineMensMorrisBoardWrapper boardPanel;
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
        boardPanel.setBoardState(currentGame, moveExecutor);
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

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        createMainMenuPanel();
        createGamePanel();

        mainPanel.add(mainMenuPanel, "Main Menu");
        mainPanel.add(gamePanel, "Game");

        add(mainPanel);
        cardLayout.show(mainPanel, "Main Menu");
    }

    private void createMainMenuPanel() {
        mainMenuPanel = new JPanel(new GridBagLayout());
        mainMenuPanel.setBackground(Color.GRAY); // Set background color to gray
        GridBagConstraints gbc = new GridBagConstraints();
    
        // Load and scale the title image
        ImageIcon originalIcon = new ImageIcon("src/game/ninemensmorris/gui/title3d2.png");
        Image originalImage = originalIcon.getImage();
        int scaledWidth = 300; // desired width
        int scaledHeight = 200; // desired height
        Image scaledImage = originalImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);
        JLabel titleLabel = new JLabel(scaledIcon);
    
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 0, 20, 0); // Add some padding around the title
        gbc.anchor = GridBagConstraints.CENTER; // Center the title image
        mainMenuPanel.add(titleLabel, gbc);
    
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 0, 5, 0); // Reset insets
    
        // New Game Button
        newGameButton = new JButton("New game");
        customButtonBigger(newGameButton);
        newGameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NineMensMorrisBoard.lastremoved = null;
                startNewGame();
                cardLayout.show(mainPanel, "Game");
                if (pvpMode) {
                    gameEnded = false;
                    initializeTimers();
                } else {
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
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2; // Span both columns to center the button
        mainMenuPanel.add(newGameButton, gbc);
    
        // Quit Button
        quitButton = new JButton("Quit");
        customButtonBigger(quitButton);
        quitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        gbc.gridy++;
        mainMenuPanel.add(quitButton, gbc);
    
        // Credit Button
        creditButton = new JButton("Credit");
        customButtonBigger(creditButton);
        creditButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame popup = new JFrame("Credits");
                popup.setSize(300, 200);
                popup.setLocationRelativeTo(null);
    
                JLabel creditsLabel = new JLabel("<html><center>This game was created by @Adnane.<br><br>©Copyright2024©</center></html>");
                creditsLabel.setHorizontalAlignment(SwingConstants.CENTER);
                popup.getContentPane().add(creditsLabel, BorderLayout.CENTER);
                popup.setVisible(true);
            }
        });
        gbc.gridy++;
        mainMenuPanel.add(creditButton, gbc);
    
        // GitHub Button
        githubButton = new JButton("GitHub Rep");
        customButtonBigger(githubButton);
        githubButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://github.com/777leed/nine-Mens-Morris"));
                } catch (IOException | URISyntaxException ex) {
                    ex.printStackTrace();
                }
            }
        });
        gbc.gridy++;
        mainMenuPanel.add(githubButton, gbc);
    
        // Settings Button
        settingsButton = new JButton("Settings");
        customButtonBigger(settingsButton);
        settingsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame settingsPopup = new JFrame("Settings");
                settingsPopup.setSize(300, 200);
                settingsPopup.setLocationRelativeTo(null);
    
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
                } else {
                    hardButton.setSelected(true);
                }
    
                JButton saveButton = new JButton("Save");
                customButton(saveButton);
                saveButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (easyButton.isSelected()) {
                            difficulty = "Easy";
                        } else if (mediumButton.isSelected()) {
                            difficulty = "Medium";
                        } else if (hardButton.isSelected()) {
                            difficulty = "Hard";
                        }
                        settingsPopup.dispose();
                    }
                });
                gbc.gridy++;
                gbc.gridwidth = 2;
                gbc.anchor = GridBagConstraints.CENTER;
                settingsPanel.add(saveButton, gbc);
    
                settingsPopup.getContentPane().add(settingsPanel, BorderLayout.CENTER);
                settingsPopup.setVisible(true);
            }
        });
        gbc.gridy++;
        mainMenuPanel.add(settingsButton, gbc);
    }
    

    private void createGamePanel() {
        gamePanel = new JPanel(new BorderLayout());

        boardPanel = new NineMensMorrisBoardWrapper(); 
        gamePanel.add(boardPanel, BorderLayout.CENTER);

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

        // Inside the createGamePanel method
JButton backButton = new JButton("Menu");
customButton(backButton);
backButton.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
        // Stop the timers
        if (player1Timer != null) {
            player1Timer.stop();
            player1Timer = null;
        }
        if (player2Timer != null) {
            player2Timer.stop();
            player2Timer = null;
        }
        // Reset game-related variables
        currentGame = null;
        currentPlayer = 0;
        currentPlayerTime = 0;
        gameEnded = false;
        NineMensMorrisBoard.lastremoved = null;
        // Switch to the main menu panel
        cardLayout.show(mainPanel, "Main Menu");
    }
});
gbc.gridy++;
buttonsPanel.add(backButton, gbc);


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

        // Panel for text fields and status label
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new FlowLayout());
        settingsPanel.add(new JLabel("Status:"));
        statusLabel = new JLabel("Your move");
        settingsPanel.add(statusLabel);

        controls.add(settingsPanel, BorderLayout.CENTER);

        gamePanel.add(controls, BorderLayout.SOUTH);

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



private void customButtonBigger(JButton button) {
    button.setPreferredSize(new Dimension(200, 60)); // Set preferred size
    button.setBackground(Color.LIGHT_GRAY); // Set background color
    button.setBorder(new EmptyBorder(5, 10, 5, 10)); // Add a margin of 5 pixels top and bottom, 10 pixels left and right
    button.setUI(new RoundedButtonUI(20)); // Set custom UI with a border radius of 20
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
