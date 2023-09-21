import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class TicTacToeClient {
    private TicTacToe server;
    private String username;
    private char symbol;

    public TicTacToeClient(String serverIP, int serverPort, String username, char symbol) {
        try {
            Registry registry = LocateRegistry.getRegistry(serverIP, serverPort);
            this.server = (TicTacToe) Naming.lookup("rmi://localhost/TicTacToeServer");
            this.username = username;
            this.symbol = symbol;

            if (server.registerPlayer(username)) {
                System.out.println("Registered successfully.");
            } else {
                System.out.println("Username is already taken.");
                System.exit(1); // Exit if the username is taken.
            }

            // Start the game loop
            startGame();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startGame() throws RemoteException {
        Scanner scanner = new Scanner(System.in);
        boolean gameInProgress = true;

        System.out.println("Game started!");
        printGameBoard();

        while (gameInProgress) {
            System.out.println(username + "'s turn. Enter location (row and column): ");
            int row = scanner.nextInt();
            int col = scanner.nextInt();

            if (isValidMove(row, col)) {
                if (server.makeMove(row, col, username)) {
                    printGameBoard();

                    if (checkForWin()) {
                        System.out.println(username + " wins!");
                        gameInProgress = false;
                    } else if (checkForDraw()) {
                        System.out.println("It's a draw!");
                        gameInProgress = false;
                    }
                } else {
                    System.out.println("Invalid move. Try again.");
                }
            } else {
                System.out.println("Invalid input. Enter a valid move.");
            }
        }

        scanner.close();
    }

    private boolean isValidMove(int row, int col) {
        // Check if the move is within the valid range (0 to 2) and if the cell is empty.
        if (row >= 0 && row < 3 && col >= 0 && col < 3) {
            try {
                char[][] gameBoard = server.getGameBoard();
                return gameBoard[row][col] == 0;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void printGameBoard() {
        try {
            char[][] gameBoard = server.getGameBoard();
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    System.out.print(gameBoard[i][j] + " ");
                }
                System.out.println();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private boolean checkForWin() {
        // Implement logic to check if a player has won.
        // You can use the current game board and the symbol (X or O) of the current player.
        // Return true if the current player has won, false otherwise.
        return false;
    }

    private boolean checkForDraw() {
        // Implement logic to check if the game is a draw.
        // You can use the current game board.
        // Return true if it's a draw, false otherwise.
        return false;
    }

    public static void main(String[] args) {
        String serverIP = "localhost";
        int serverPort = 1099;

        // Create Player 1 (X)
        String username1 = "Player1";
        char symbol1 = 'X';
        TicTacToeClient client1 = new TicTacToeClient(serverIP, serverPort, username1, symbol1);

        // Create Player 2 (O)
        String username2 = "Player2";
        char symbol2 = 'O';
        TicTacToeClient client2 = new TicTacToeClient(serverIP, serverPort, username2, symbol2);
    }
}
