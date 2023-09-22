package Client;

import Server.ServerInterface;

import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class TicTacToeClient extends UnicastRemoteObject implements ClientCallback {

    private ServerInterface server; // Assuming you've defined a ServerInterface for the server's RMI methods.
    private String playerName;
    private char playerSymbol;
    private boolean isExported = false;
    private char[][] board = new char[3][3];

    public TicTacToeClient(String playerName) throws Exception {
        this.playerName = playerName;

        // Connect to the server
        server = (ServerInterface) Naming.lookup("rmi://localhost/TicTacToeServer");

        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (NoSuchObjectException e) {
            // Handle exception or print stack trace
            e.printStackTrace();
        }

        if (!isExported) {
            // Register this client's callback with the server
            ClientCallback clientStub = (ClientCallback) UnicastRemoteObject.exportObject(this, 0);
            server.registerPlayer(playerName, clientStub);
            isExported = true;
        }
    }

    @Override
    public void gameReady(String opponentName) throws RemoteException {
        // Here you'd probably update the UI to reflect that the game has started.
        System.out.println("Matched with: " + opponentName);
        // Initialize or reset the game board
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = '-';
            }
        }
    }

    @Override
    public void updateBoard(int row, int col, char symbol) throws RemoteException {
        // Update the local game board and refresh the display/UI.
        board[row][col] = symbol;
        displayBoard();
    }

    @Override
    public void displayMessage(String message) throws RemoteException {
        System.out.println(message);
    }

    public void makeMove(int row, int col) throws RemoteException {
        // This method is called when the user wants to make a move.
        server.makeMove(playerName, row, col);
    }

    private void displayBoard() {
        // This is just a simple console print for the board. In a real application, you'd update the GUI.
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                System.out.print(board[i][j] + " ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        // This is a basic main method to run the client.
        try {
            new TicTacToeClient("PlayerName"); // You'd get the player's name dynamically, perhaps from command-line or a GUI input.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
