import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Scanner;

public class TicTacToeClient {
    private TicTacToe server;
    private String username;

    public TicTacToeClient(String serverIP, int serverPort, String username) {
        try {
            this.server = (TicTacToe) Naming.lookup("rmi://" + serverIP + "/TicTacToeServer");
            this.username = username;

            // Register the player and get their symbol
            if (!(server.registerPlayer("player1") && server.registerPlayer("player2"))) {
                System.out.println("players not registered");
            }

            // Start the game
            String startingPlayer = server.startGame("player1", "player2");
            System.out.println("Game started!");

            System.out.println(startingPlayer + "'s turn.");

            String currentPlayer = startingPlayer;
            // Game loop
            Scanner scanner = new Scanner(System.in);

            boolean gameInProgress = true;

            while (gameInProgress) {
                System.out.println("Enter location (row and column): ");
                int row = scanner.nextInt();
                int col = scanner.nextInt();
                scanner.nextLine();

                boolean validMove =  server.makeMove(row, col, currentPlayer);
                while (!validMove) {
                    System.out.println("Invalid move: please try again");
                    System.out.println("Enter location (row and column): ");
                    row = scanner.nextInt();
                    col = scanner.nextInt();
                    validMove =  server.makeMove(row, col, currentPlayer);
                }

                printGameBoard();

                char state = server.evaluateGame();
                if (state == 'D') {
                    System.out.println("Game draw");
                    gameInProgress = false;
                } else if (state == 'X' | state == 'O') {
                    System.out.println(currentPlayer + " won");
                    gameInProgress = false;
                } else {
                    currentPlayer = currentPlayer.equals("player1") ? "player2" : "player1";
                    System.out.println(currentPlayer + "'s turn.");
                }
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
        String username = "Player1";

        TicTacToeClient client = new TicTacToeClient(serverIP, serverPort, username);
    }
}
