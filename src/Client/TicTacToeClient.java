package Client;

import Server.ServerInterface;

import javax.swing.*;
import java.awt.*;
import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
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

        try {
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
            }
        } catch (RemoteException | NotBoundException e) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(frame, "Server unavailable", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            });
        }
    }


    private void startHeartbeat() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    server.sendHeartbeat(playerName);
                } catch (RemoteException e) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame, "Server unavailable", "Error", JOptionPane.ERROR_MESSAGE);
                        System.exit(0);
                    });
                }
            }
        }, 0, 2000);
    }

    private void createAndShowGUI() {
        initializeFrame();
        setupTimerQuitPanel();
        setupGameInfoLabel();
        setupBoardPanel();
        setupChatPanel();
        frame.pack();
        frame.setVisible(true);
    }

    private void initializeFrame() {
        frame = new JFrame("Tic Tac Toe: " + playerName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
    }

    private void setupTimerQuitPanel() {
        JPanel timerQuitPanel = new JPanel();
        timerQuitPanel.setLayout(new BoxLayout(timerQuitPanel, BoxLayout.Y_AXIS));

        JLabel timerText = new JLabel("Timer");
        timerText.setAlignmentX(Component.CENTER_ALIGNMENT);
        timerQuitPanel.add(timerText);

        timerLabel = new JLabel("Timer: 20");
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timerQuitPanel.add(timerLabel);

        timerQuitPanel.add(Box.createVerticalGlue());

        JButton quitButton = createQuitButton();
        timerQuitPanel.add(quitButton);

        timerQuitPanel.setPreferredSize(new Dimension(frame.getWidth() * 2 / 12, frame.getHeight()));

        frame.getContentPane().add(timerQuitPanel, BorderLayout.WEST);
    }

    private JButton createQuitButton() {
        JButton quitButton = new JButton("Quit");
        quitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
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

        return quitButton;
    }

    private void setupGameInfoLabel() {
        gameInfo = new JLabel("Finding Player");
        gameInfo.setHorizontalAlignment(JLabel.CENTER);
    }

    private void setupBoardPanel() {
        JPanel boardPanel = new JPanel();
        boardPanel.setLayout(new BorderLayout());
        boardPanel.setPreferredSize(new Dimension(400, 400));

        JPanel gridPanel = createGridPanel();

        boardPanel.add(gameInfo, BorderLayout.NORTH);
        boardPanel.add(gridPanel, BorderLayout.CENTER);

        frame.getContentPane().add(boardPanel, BorderLayout.CENTER);
    }

    private JPanel createGridPanel() {
        JPanel gridPanel = new JPanel(new GridLayout(3, 3, 5, 5));
        gridPanel.setBackground(Color.BLACK);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                JButton button = createGridButton(i, j);
                gridPanel.add(button);
            }
        }

        return gridPanel;
    }

    private JButton createGridButton(int i, int j) {
        buttons[i][j] = new JButton(" ");
        buttons[i][j].setFont(new Font("Arial", Font.BOLD, 60));
        buttons[i][j].setBackground(Color.WHITE);

        buttons[i][j].addActionListener(e -> {
            if (myTurn && buttons[i][j].getText().equals(" ")) {
                try {
                    stopCountdownTimer();
                    server.makeMove(playerName, i, j);
                    myTurn = false;
                    stopCountdownTimer();
                    gameInfo.setText("Rank#" + opponentRank + " " +opponentName + "'s turn (" + opponentChar + ")");

                } catch (RemoteException remoteException) {
                    remoteException.printStackTrace();
                }
            }
        });

        return buttons[i][j];
    }

    private void setupChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setPreferredSize(new Dimension(250, frame.getHeight()));

        JLabel chatTitle = new JLabel("Player Chat");
        chatTitle.setHorizontalAlignment(JLabel.CENTER);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);

        chatInput = new JTextField(15);
        sendButton = createSendButton();

        JPanel chatBottomPanel = new JPanel(new BorderLayout());
        chatBottomPanel.add(chatInput, BorderLayout.CENTER);
        chatBottomPanel.add(sendButton, BorderLayout.EAST);

        chatPanel.add(chatTitle, BorderLayout.NORTH);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatPanel.add(chatBottomPanel, BorderLayout.SOUTH);

        frame.getContentPane().add(chatPanel, BorderLayout.EAST);
    }

    private JButton createSendButton() {
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());
        return sendButton;
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
                    try {
                        countdownTimer.cancel();
                    } catch (NullPointerException e) {
                    }
                }
            }
        }, 0, 1000);
    }


    private void stopCountdownTimer() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            timerLabel.setText("Timer: -");
            countdownTimer = null;
        }
    }

    private void makeRandomMove() {
        List<Point> emptyCells = new ArrayList<>();

        try {
            char[][] board = server.getCurrentBoardState(playerName);

            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board[i].length; j++) {
                    if (board[i][j] == ' ') emptyCells.add(new Point(i, j));
                }
            }

            if (!emptyCells.isEmpty()) {
                Point randomMove = emptyCells.get(new Random().nextInt(emptyCells.size()));
                server.makeMove(playerName, randomMove.x, randomMove.y);
                myTurn = false;
                gameInfo.setText("Rank#" + opponentRank + " " + opponentName + "'s turn (" + opponentChar + ")");
                stopCountdownTimer();
            }

        } catch (RemoteException ex) {
            ex.printStackTrace();
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
                    myTurn = false;
                    stopCountdownTimer();
                    for (int i = 0; i < 3; i++) {
                        for (int j = 0; j < 3; j++) {
                            buttons[i][j].setText(" ");
                        }
                    }
                    chatArea.setText("");
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

            stopCountdownTimer();
            sendButton.setEnabled(false);

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
                        try {
                            server.drawGameDisconnection(playerName, opponentName);
                            askToPlayAgain();
                        } catch (RemoteException e) {
                            throw new RuntimeException(e);
                        }
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
            sendButton.setEnabled(true);
            gameInfo.setText("Game resumed");
            try {
                // Fetch the current state of the game from the server
                char[][] board = server.getCurrentBoardState(playerName);
                displayBoard(board);

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

    public void refreshBoard() throws RemoteException{
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText(" ");
            }
        }
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
