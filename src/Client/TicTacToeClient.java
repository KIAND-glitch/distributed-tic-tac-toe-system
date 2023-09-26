package Client;

import Server.ServerInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class TicTacToeClient extends UnicastRemoteObject implements ClientCallback {

    private ServerInterface server;
    private String playerName;
    private String opponentName;
    private Character playerSymbol;
    private Character opponentSymbol;
    private boolean isExported = false;
    private boolean myTurn = false;

    private JFrame frame;
    private JButton[][] buttons = new JButton[3][3];
    private JTextArea chatArea;
    private JTextField chatInput;
    private JButton sendButton;

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
            server.registerPlayer(playerName, clientStub);
            playGame(playerName);
        }
    }

    private void createAndShowGUI() {
        frame = new JFrame("Tic Tac Toe ");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        // Timer and Quit Button Panel
        JPanel timerQuitPanel = new JPanel();
        timerQuitPanel.setLayout(new BoxLayout(timerQuitPanel, BoxLayout.Y_AXIS));

        JLabel timerText = new JLabel("Timer");
        timerText.setAlignmentX(Component.CENTER_ALIGNMENT);
        timerQuitPanel.add(timerText);

        JLabel timerLabel = new JLabel("00:00");
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timerQuitPanel.add(timerLabel);

        // Spacer to push the quit button to the bottom
        timerQuitPanel.add(Box.createVerticalGlue());

        JButton quitButton = new JButton("Quit");
        quitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        timerQuitPanel.add(quitButton);

        quitButton.setPreferredSize(new Dimension(100, 30));
        quitButton.setMaximumSize(new Dimension(100, 30));

        timerQuitPanel.setPreferredSize(new Dimension(frame.getWidth() * 2 / 12, frame.getHeight()));

        // Player's turn label above the board
        JLabel playerTurnLabel = new JLabel("Player's Turn: [Name]");
        playerTurnLabel.setHorizontalAlignment(JLabel.CENTER);

        // Tic Tac Toe Board
        JPanel boardPanel = new JPanel();
        boardPanel.setLayout(new BorderLayout());
        boardPanel.setPreferredSize(new Dimension(frame.getWidth() * 7 / 12, frame.getHeight()));

        JPanel gridPanel = new JPanel(new GridLayout(3, 3, 5, 5));
        gridPanel.setBackground(Color.BLACK);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j] = new JButton(" ");
                buttons[i][j].setFont(new Font("Arial", Font.BOLD, 60));
                buttons[i][j].setBackground(Color.WHITE);
                buttons[i][j].addActionListener(new ButtonListener(i, j));
                gridPanel.add(buttons[i][j]);
            }
        }

        boardPanel.add(playerTurnLabel, BorderLayout.NORTH);
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
        chatPanel.setPreferredSize(new Dimension(frame.getWidth() * 3 / 12, frame.getHeight()));

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



    private void sendMessage() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            chatArea.append("You: " + message + "\n");
            chatInput.setText("");

            try {
                server.sendMessageToOpponent(playerName, message);
            } catch (RemoteException e) {
                e.printStackTrace();
                chatArea.append("Failed to send message.\n");
            }
        }
    }

    private class ButtonListener implements ActionListener {
        int row;
        int col;

        public ButtonListener(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (myTurn && buttons[row][col].getText().equals(" ")) {
                try {
                    char result = server.makeMove(playerName, row, col);
                    handleGameResult(result);
                    myTurn = false; // It's no longer this client's turn
                    frame.setTitle("Waiting for Opponent");
                    // Disable board buttons
                    for(int i = 0; i < 3; i++) {
                        for(int j = 0; j < 3; j++) {
                            buttons[i][j].setEnabled(false);
                        }
                    }
                } catch (RemoteException remoteException) {
                    remoteException.printStackTrace();
                }
            }
        }

    }

    private void handleGameResult(char result) throws RemoteException {
        switch (result) {
            case 'D':
                chatArea.append("Game draw\n");
                break;
            case 'W':
                chatArea.append("You won!\n");
                break;
            case 'I':
                chatArea.append("Invalid move: please try again\n");
                break;
            default:
                break;
        }
    }

    @Override
    public void notifyTurn() throws RemoteException {
        if(frame == null) {
            System.out.println("Frame has not been initialized!");
            return;
        }

        myTurn = true;
        frame.setTitle("Your Turn");

        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 3; j++) {
                buttons[i][j].setEnabled(true);
            }
        }
    }

    @Override
    public void displayBoard(char[][] board ) throws RemoteException {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText(String.valueOf(board[i][j]));
                if(board[i][j] != ' ') {
                    buttons[i][j].setEnabled(false);
                }
            }
        }
    }


    @Override
    public void displayMessage(String message) throws RemoteException {
        chatArea.append(message + "\n");
    }

    @Override
    public void gameStarted(String opponentName) throws RemoteException {
        chatArea.append("Game started with " + opponentName + "\n");
        this.opponentName = opponentName;
        this.opponentSymbol = this.playerSymbol == 'X' ? 'O' : 'X';
    }

    @Override
    public void assignCharacter(Character character, String startMessage) throws RemoteException {
        System.out.println("You are assigned: " + character);
        System.out.println(startMessage);
        this.playerSymbol = character;
    }


    private void playGame(String playerName) throws RemoteException {
        while (true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
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
