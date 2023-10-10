package Client;

import Server.ServerInterface;

import javax.swing.*;
import java.awt.*;
import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.awt.Point;
import java.util.Timer;
import java.util.TimerTask;

public class TicTacToeClient extends UnicastRemoteObject implements ClientCallback {

    private ServerInterface server;
    private String playerName;
    private Character playerChar;
    private int playerRank = -1;
    private String opponentName;
    private Character opponentChar;
    private int opponentRank;
    private List<String> chatMessages = new ArrayList<>();
    private boolean isExported = false;
    private boolean myTurn = false;
    private JFrame frame;
    private JButton[][] buttons = new JButton[3][3];
    private JTextArea chatArea;
    private JTextField chatInput;
    private JButton sendButton;
    private int timeLeft = 20;
    private Timer countdownTimer;
    private JLabel timerLabel;
    private JLabel gameInfo;
    private Timer pauseTimer;

    public TicTacToeClient(String playerName) throws Exception {
        this.playerName = playerName;
        server = (ServerInterface) Naming.lookup("rmi://localhost/TicTacToeServer");

        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (NoSuchObjectException e) {
            e.printStackTrace();
        }

        if (!isExported) {
            ClientCallback clientStub = (ClientCallback) UnicastRemoteObject.exportObject(this, 0);
            createAndShowGUI();
            isExported = true;
            server.registerPlayer(playerName, clientStub, false);
            startHeartbeat();
            playGame();
        }
    }

    private void startHeartbeat() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    server.sendHeartbeat(playerName);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame, "Server unavailable", "Error", JOptionPane.ERROR_MESSAGE);
                        System.exit(0);
                    });
                }
            }
        }, 0, 2000);
    }

    private void createAndShowGUI() {
        frame = new JFrame("Tic Tac Toe: " + playerName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        // Timer and Quit Button Panel
        JPanel timerQuitPanel = new JPanel();
        timerQuitPanel.setLayout(new BoxLayout(timerQuitPanel, BoxLayout.Y_AXIS));

        JLabel timerText = new JLabel("Timer");
        timerText.setAlignmentX(Component.CENTER_ALIGNMENT);
        timerQuitPanel.add(timerText);

        timerLabel = new JLabel("Timer: 20");
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timerQuitPanel.add(timerLabel);

        // Spacer to push the quit button to the bottom
        timerQuitPanel.add(Box.createVerticalGlue());

        JButton quitButton = new JButton("Quit");
        quitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        timerQuitPanel.add(quitButton);

        quitButton.setPreferredSize(new Dimension(100, 30));
        quitButton.setMaximumSize(new Dimension(100, 30));

        quitButton.addActionListener(e -> {
            try {
                server.quitGame(playerName, false);
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
            System.exit(0);
        });

        timerQuitPanel.setPreferredSize(new Dimension(frame.getWidth() * 2 / 12, frame.getHeight()));

        // Player's turn label above the board
        gameInfo = new JLabel("Finding Player");
        gameInfo.setHorizontalAlignment(JLabel.CENTER);

        // Tic Tac Toe Board
        JPanel boardPanel = new JPanel();
        boardPanel.setLayout(new BorderLayout());
        // boardPanel.setPreferredSize(new Dimension(frame.getWidth() * 7 / 12, frame.getHeight()));
        boardPanel.setPreferredSize(new Dimension(400, 400));


        JPanel gridPanel = new JPanel(new GridLayout(3, 3, 5, 5));
        gridPanel.setBackground(Color.BLACK);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j] = new JButton(" ");
                buttons[i][j].setFont(new Font("Arial", Font.BOLD, 60));
                buttons[i][j].setBackground(Color.WHITE);

                int finalI = i;
                int finalJ = j;

                buttons[i][j].addActionListener(e -> {
                    if (myTurn && buttons[finalI][finalJ].getText().equals(" ")) {
                        System.out.println("button clicked" + finalI + finalJ);
                        try {
                            stopCountdownTimer();
                            char result = server.makeMove(playerName, finalI, finalJ);
//                            handleGameResult(result);
                            myTurn = false;
                            gameInfo.setText("Rank#" + opponentRank + " " +opponentName + "'s turn (" + opponentChar + ")");

                        } catch (RemoteException remoteException) {
                            remoteException.printStackTrace();
                        }
                    }
                });

                gridPanel.add(buttons[i][j]);
            }
        }

        boardPanel.add(gameInfo, BorderLayout.NORTH);
        boardPanel.add(gridPanel, BorderLayout.CENTER);

        // Chat Area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);

        // Chat input field and send button
        chatInput = new JTextField(15);
        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());

        JPanel chatBottomPanel = new JPanel(new BorderLayout());
        chatBottomPanel.add(chatInput, BorderLayout.CENTER);
        chatBottomPanel.add(sendButton, BorderLayout.EAST);

        JPanel chatPanel = new JPanel(new BorderLayout());
        // chatPanel.setPreferredSize(new Dimension(frame.getWidth() * 3 / 12, frame.getHeight()));
        chatPanel.setPreferredSize(new Dimension(250, frame.getHeight()));


        JLabel chatTitle = new JLabel("Player Chat");
        chatTitle.setHorizontalAlignment(JLabel.CENTER);
        chatPanel.add(chatTitle, BorderLayout.NORTH);

        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatPanel.add(chatBottomPanel, BorderLayout.SOUTH);

        frame.getContentPane().add(timerQuitPanel, BorderLayout.WEST);
        frame.getContentPane().add(boardPanel, BorderLayout.CENTER);
        frame.getContentPane().add(chatPanel, BorderLayout.EAST);

        frame.pack();
        frame.setVisible(true);
    }

    private void setupCountdownTimer() {
        stopCountdownTimer();
        countdownTimer = new Timer();

        timeLeft = 20;
        timerLabel.setText("Timer: " + timeLeft);
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                timeLeft--;
                timerLabel.setText("Timer: " + timeLeft);

                if (timeLeft <= 0) {
                    makeRandomMove();
                    countdownTimer.cancel();
                }
            }
        }, 0, 1000);
    }

    private void stopCountdownTimer() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
    }

    private void makeRandomMove() {
        List<Point> emptyCells = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (buttons[i][j].getText().equals(" ")) {
                    emptyCells.add(new Point(i, j));
                }
            }
        }
        if (!emptyCells.isEmpty()) {
            Point randomMove = emptyCells.get(new Random().nextInt(emptyCells.size()));
            try {
                char result = server.makeMove(playerName, randomMove.x, randomMove.y);
//                handleGameResult(result);
                myTurn = false;

            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void sendMessage() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            chatArea.append("Rank#" + playerRank + " " +playerName + ": " + message + "\n");
            chatInput.setText("");
            try {
                server.sendMessageToOpponent(playerRank, playerName, message);
            } catch (RemoteException e) {
                e.printStackTrace();
                chatArea.append("Failed to send message.\n");
            }
        }
    }

    public void updateGameInfo(String updatedGameInfo) throws RemoteException {
        gameInfo.setText(updatedGameInfo);
    }

    @Override
    public void notifyTurn() throws RemoteException {
        if(frame == null) {
            System.out.println("Frame has not been initialized!");
            return;
        }

        gameInfo.setText("Rank#" + playerRank + " " +playerName + "'s turn (" + playerChar + ")");
        myTurn = true;

        setupCountdownTimer();
    }

    @Override
    public void displayBoard(char[][] board) throws RemoteException {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText(String.valueOf(board[i][j]));
            }
        }
    }

    @Override
    public void displayMessage(String message) throws RemoteException {
        chatArea.append(message + "\n");
    }

    @Override
    public void assignCharacter(Character character, String opponentName, int rank, int opponentRank) throws RemoteException {
        this.playerChar = character;
        this.opponentName = opponentName;
        this.opponentChar = character == 'X'? '0':'X';
        this.playerRank = rank;
        this.opponentRank = opponentRank;

        gameInfo.setText("Rank#" + opponentRank + " " +opponentName + "'s turn (" + opponentChar + ")");
    }

    private void playGame() throws RemoteException {
        while (true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void askToPlayAgain() throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            int response = JOptionPane.showConfirmDialog(null,
                    "Game Over! Find a new match?",
                    "Tic Tac Toe",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if(response == JOptionPane.YES_OPTION){
                try {
                    // Reset game state
                    myTurn = false;  // This line ensures players can't make moves until it's their turn

                    for (int i = 0; i < 3; i++) {
                        for (int j = 0; j < 3; j++) {
                            buttons[i][j].setText(" ");
                        }
                    }
                    gameInfo.setText("Finding Player");
                    server.registerPlayer(playerName, this, true);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            else {
                try {
                    server.quitGame(playerName, true);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
                System.exit(0);
            }
        });
    }

    public void handlePause() {
        SwingUtilities.invokeLater(() -> {
            gameInfo.setText("Game paused, Timer: 30");


            if (pauseTimer != null) {
                pauseTimer.cancel();
            }
            pauseTimer = new Timer();
            pauseTimer.scheduleAtFixedRate(new TimerTask() {
                int countdown = 30;

                @Override
                public void run() {
                    countdown--;
                    gameInfo.setText("Game paused, Timer: " + countdown);
                    if (countdown <= 0) {
//                        chatArea.append("Game ended in a draw due to player disconnection\n");
                        try {
                            server.drawGameDisconnection(playerName, opponentName);
                        } catch (RemoteException e) {
                            throw new RuntimeException(e);
                        }
                        gameInfo.setText("Game ended in a draw");
                        pauseTimer.cancel();
                    }
                }
            }, 0, 1000);
        });
    }

    public void resumeGame() throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            if (pauseTimer != null) {
                pauseTimer.cancel();
            }
            gameInfo.setText("Game resumed");
            try {
                // Fetch the current state of the game from the server
                char[][] board = server.getCurrentBoardState(playerName);
                displayBoard(board);

                // Decide and Notify whose turn it is
                if (myTurn) {
                    notifyTurn();
                } else {
                    gameInfo.setText("Rank#" + opponentRank + " " +opponentName + "'s turn (" + opponentChar + ")");
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java TicTacToeClient <username>");
            return;
        }
        String playerName = args[0];
        try {
            new TicTacToeClient(playerName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
