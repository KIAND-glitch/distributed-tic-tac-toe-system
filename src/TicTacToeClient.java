import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Scanner;

public class TicTacToeClient {
    private TicTacToe server;
    private String username;
    private char playerSymbol;

    public TicTacToeClient(String serverIP, int serverPort, String username) {
        try {
            this.server = (TicTacToe) Naming.lookup("rmi://" + serverIP + "/TicTacToeServer");
            this.username = username;

            // Register the player and get their symbol
            if (! (server.registerPlayer("player1") && server.registerPlayer("player2"))) {
                System.out.println("players not registered");
            }


            // Start the game
            String startingPlayer = server.startGame("player1", "player2");
//            System.out.println("Game started!");
//            System.out.println("Player 1: X");
//            System.out.println("Player 2: O");
            System.out.println(startingPlayer + "'s turn.");

            // Game loop
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("Enter location (row and column): ");
                int row = scanner.nextInt();
                int col = scanner.nextInt();
                scanner.nextLine();  // Consume newline character

                System.out.println("row is " + row);
                System.out.println("col is " + col);

                String nextPlayer = server.makeMove(row, col, startingPlayer);
                System.out.println(nextPlayer);
//                if (!nextPlayer.equals(username)) {
//                    System.out.println("Invalid move. Try again.");
//                } else {
                    printGameBoard();
                    System.out.println(nextPlayer + "'s turn.");
//                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printGameBoard() throws RemoteException {
        char[][] gameBoard = server.getGameBoard();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                System.out.print(gameBoard[i][j] + " ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        String serverIP = "localhost";
        int serverPort = 1099;
        String username = "Player1"; // You can set the username for Player2 accordingly.

        TicTacToeClient client = new TicTacToeClient(serverIP, serverPort, username);
    }
}
