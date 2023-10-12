package Client;

import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;
import java.util.Timer;

public class TicTacToeGUI {
    private TicTacToeClient client;
    private JFrame frame;
    private JButton[][] buttons = new JButton[3][3];
    private JTextArea chatArea;
    private JTextField chatInput;
    private JButton sendButton;
    private int timeLeft = 20;
    private java.util.Timer countdownTimer;
    private JLabel timerLabel;
    private JLabel gameInfo;
    private Timer pauseTimer;

    public TicTacToeGUI(TicTacToeClient client) {
        this.client = client;
        createAndShowGUI();
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
        frame = new JFrame("Tic Tac Toe: " + client.getPlayerName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
    }

    public void setupCountdownTimer() {
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
                client.getServer().quitGame(client.getPlayerName(), false);
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
                createGridButtons(i, j, gridPanel);
            }
        }

        return gridPanel;
    }

    private void createGridButtons(int i, int j, JPanel gridPanel) {
        buttons[i][j] = new JButton(" ");
        buttons[i][j].setFont(new Font("Arial", Font.BOLD, 60));
        buttons[i][j].setBackground(Color.WHITE);

        buttons[i][j].addActionListener(e -> {
            System.out.println("condition 1" + client.isMyTurn());
            System.out.println("condition 2" + buttons[i][j].getText().equals(" "));
            if (client.isMyTurn() && buttons[i][j].getText().equals(" ")) {
                System.out.println("Clicked" + i + j);
                try {
                    client.getServer().makeMove(client.getPlayerName(), i, j);
                    client.setMyTurn(false);
                    stopCountdownTimer();
                    gameInfo.setText("Rank#" + client.getOpponentRank() + " " + client.getOpponentName() + "'s turn (" + client.getOpponentChar() + ")");

                } catch (RemoteException remoteException) {
                    remoteException.printStackTrace();
                }
            }
        });

        gridPanel.add(buttons[i][j]);
    }

    private void makeRandomMove() {
        List<Point> emptyCells = new ArrayList<>();

        try {
            char[][] board = client.getServer().getCurrentBoardState(client.getPlayerName());

            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board[i].length; j++) {
                    if (board[i][j] == ' ') emptyCells.add(new Point(i, j));
                }
            }

            if (!emptyCells.isEmpty()) {
                Point randomMove = emptyCells.get(new Random().nextInt(emptyCells.size()));
                client.getServer().makeMove(client.getPlayerName(), randomMove.x, randomMove.y);
                client.setMyTurn(false);
                gameInfo.setText("Rank#" + client.getOpponentRank() + " " + client.getOpponentName() + "'s turn (" + client.getOpponentChar() + ")");                stopCountdownTimer();
            }

        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
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

    private void sendMessage() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            chatArea.append("Rank#" + client.getPlayerRank() + " " + client.getPlayerName() + ": " + message + "\n");
            chatInput.setText("");
            try {
                client.getServer().sendMessageToOpponent(client.getPlayerRank(), client.getPlayerName(), message);
            } catch (RemoteException e) {
                e.printStackTrace();
                chatArea.append("Failed to send message.\n");
            }
        }
    }

    public void stopCountdownTimer() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            timerLabel.setText("Timer: -");
            countdownTimer = null;
        }
    }

    public void refreshBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText(" ");
            }
        }
    }

    public void updateGameInfo(String info) {
        gameInfo.setText(info);
    }

    public void displayMessage(String message) {
        chatArea.append(message + "\n");
    }

    public void resumeGameGui() {
        if (pauseTimer != null) {
            pauseTimer.cancel();
        }
        sendButton.setEnabled(true);
        gameInfo.setText("Game resumed");
    }

    public void handleGamePause() {
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
                    pauseTimer.cancel();
                }
            }
        }, 0, 1000);
    }

    public boolean showPlayAgainDialog() {
        int response = JOptionPane.showConfirmDialog(null,
                "Game Over! Find a new match?",
                "Tic Tac Toe",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        return response == JOptionPane.YES_OPTION;
    }

    public void resetGameGui() {
        client.setMyTurn(false);
        stopCountdownTimer();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText(" ");
            }
        }
        chatArea.setText("");
        gameInfo.setText("Finding Player");
    }

    public void updateButtonsText(char[][] board) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText(String.valueOf(board[i][j]));
            }
        }
    }

}
