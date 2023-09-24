package Client;

import Server.ServerInterface;

import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

public class TicTacToeClient extends UnicastRemoteObject implements ClientCallback {

    private ServerInterface server;
    private String playerName;
    private String opponentName;
    private Character playerSymbol;
    private Character opponentSymbol;
    private boolean isExported = false;
    private boolean myTurn = false;

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
            server.registerPlayer(playerName, clientStub);
            isExported = true;
            playGame(playerName);
        }
    }

    @Override
    public void notifyTurn() throws RemoteException {
        myTurn = true;
    }

    @Override
    public void displayMessage(String message) throws RemoteException {
        System.out.println(message);
    }

    @Override
    public void gameStarted(String opponentName) throws RemoteException {
        System.out.println("Game started with " + opponentName);
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
        Scanner scanner = new Scanner(System.in);

        while (true) {
            if (myTurn) {
                char state = 'I';  // Assume the move is invalid by default
                while (state == 'I') {
                    System.out.println("Enter location (row and column): ");
                    int row = scanner.nextInt();
                    int col = scanner.nextInt();
                    scanner.nextLine();

                    try {
                        state = server.makeMove(playerName, row, col);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    if (state == 'I') {
                        System.out.println("Invalid move: please try again");
                    }
                }

                if (state == 'D') {
                    System.out.println("Game draw");
                    break;
                } else if (state == 'W') {
                    System.out.println("You won!");
                    break;
                } else {
                    myTurn = false;  // state == 'N', so it's not the client's turn anymore
                }
            } else {
                System.out.println("Waiting for other player's move...");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    public void displayBoard(char[][] board ) throws RemoteException {
        // This is just a simple console print for the board. In a real application, you'd update the GUI.
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                System.out.print(board[i][j] + " ");
            }
            System.out.println();
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
