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

        // Tic Tac Toe Board
        JPanel boardPanel = new JPanel(new GridLayout(3, 3, 5, 5));
        boardPanel.setPreferredSize(new Dimension(400, 400));
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j] = new JButton(" ");
                buttons[i][j].setFont(new Font("Arial", Font.BOLD, 60));
                buttons[i][j].addActionListener(new ButtonListener(i, j));
                boardPanel.add(buttons[i][j]);
            }
        }

        // Chat Area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setPreferredSize(new Dimension(200, 400));

        frame.getContentPane().add(boardPanel, BorderLayout.WEST);
        frame.getContentPane().add(chatScrollPane, BorderLayout.EAST);

        frame.pack();
        frame.setVisible(true);
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
