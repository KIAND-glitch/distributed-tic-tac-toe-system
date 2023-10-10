package Server;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GameSession {
    private HashMap<String, PlayerInfo> players = new HashMap<>();
    private char[][] board = new char[3][3];
    private String currentPlayer;

    private String[] playerChat;

    private TicTacToeServer server;


    public HashMap<String, PlayerInfo> getPlayers() {
        return players;
    }

    public GameSession(String player1, PlayerInfo info1, String player2, PlayerInfo info2, TicTacToeServer server) {
        players.put(player1, info1);
        players.put(player2, info2);
        this.server = server;

        // Initialize the board with spaces
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = ' ';
            }
        }

        currentPlayer = player1; // Start with the first player
    }

    public void assignCharactersAndStart() throws RemoteException {
        players.get(currentPlayer).symbol = 'X';
        String otherPlayer = players.keySet().stream().filter(p -> !p.equals(currentPlayer)).findFirst().get();
        players.get(otherPlayer).symbol = 'O';

        players.get(otherPlayer).client.assignCharacter('O', currentPlayer, server.getPlayerRank(otherPlayer), server.getPlayerRank(currentPlayer));

        players.get(currentPlayer).client.assignCharacter('X', otherPlayer, server.getPlayerRank(currentPlayer), server.getPlayerRank(otherPlayer));
        players.get(currentPlayer).client.notifyTurn();

    }

    public char makeMove(String playerName, int row, int col) throws RemoteException {
        if (row < 0 || row >= 3 || col < 0 || col >= 3 || board[row][col] != ' ') {
            return 'I';  // Invalid move
        }

        System.out.println("made move" + playerName + row + col);

        board[row][col] = players.get(playerName).symbol;
        players.get(playerName).client.displayBoard(board);

        // Notify the other player about the board update
        String otherPlayer = players.keySet().stream().filter(p -> !p.equals(playerName)).findFirst().get();
        players.get(otherPlayer).client.displayBoard(board);

        // Check for game end conditions
        char result = evaluateGame();

        if (result == 'N') {
            // If the game isn't over, switch turns
            currentPlayer = otherPlayer;
            players.get(currentPlayer).client.notifyTurn();
        } else {
            // If the game is over
            if (result == 'D') {
                players.get(playerName).client.updateGameInfo("Match Drawn");
                players.get(otherPlayer).client.updateGameInfo("Match Drawn");
                server.updatePointsAfterGame(playerName, 2);
                server.updatePointsAfterGame(otherPlayer, 2);

            } else if (result == players.get(playerName).symbol) { // Current player wins
                System.out.println("player" + playerName );
                System.out.println("other player" + otherPlayer );
                players.get(playerName).client.updateGameInfo("Player "+ playerName +" wins!");
                players.get(otherPlayer).client.updateGameInfo("Player "+ playerName +" wins!");
                server.updatePointsAfterGame(playerName, 5);
                server.updatePointsAfterGame(otherPlayer, -5);
            } else { // Opponent wins
                System.out.println("player" + playerName );
                System.out.println("other player" + otherPlayer );
                players.get(playerName).client.updateGameInfo("Player " + otherPlayer + " wins!");
                players.get(otherPlayer).client.updateGameInfo("Player " + otherPlayer + " wins!");
                server.updatePointsAfterGame(playerName, -5);
                server.updatePointsAfterGame(otherPlayer, 5);
            }
            askForRematch();
        }

        return result;
    }


    public char evaluateGame() {
        // Simplified win checking, doesn't account for draws or determine if the game can continue
        // Loop through rows, columns, and diagonals to check for three of the same symbol
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == board[i][1] && board[i][1] == board[i][2] && board[i][0] != ' ') return board[i][0];
            if (board[0][i] == board[1][i] && board[1][i] == board[2][i] && board[0][i] != ' ') return board[0][i];
        }
        if (board[0][0] == board[1][1] && board[1][1] == board[2][2] && board[0][0] != ' ') return board[0][0];
        if (board[0][2] == board[1][1] && board[1][1] == board[2][0] && board[0][2] != ' ') return board[0][2];

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == ' ') return 'N'; // Game is not over yet
            }
        }
        return 'D'; // Game is a draw
    }

    public void sendMessage(String sender, String message) throws RemoteException {
        String receiver = players.keySet().stream().filter(p -> !p.equals(sender)).findFirst().get();
        players.get(receiver).client.displayMessage(sender + ": " + message);
    }

    public void askForRematch() throws RemoteException {
        for (String player : players.keySet()) {
            players.get(player).client.askToPlayAgain();
        }
    }

    public String getOtherPlayer(String playerName) {
        for (String name : players.keySet()) {
            if (!name.equals(playerName)) {
                return name;
            }
        }
        return null;
    }

    public char[][] getBoard() {
        return board;
    }

    public String getCurrentPlayer() {
        return currentPlayer;
    }
}
