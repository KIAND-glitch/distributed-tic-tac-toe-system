package Server;

import Client.ClientCallback;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class TicTacToeServer extends UnicastRemoteObject implements ServerInterface {

    private class PlayerInfo {
        ClientCallback client;
        char symbol;

        PlayerInfo(ClientCallback client, char symbol) {
            this.client = client;
            this.symbol = symbol;
        }
    }

    private class GameSession {
        private HashMap<String, PlayerInfo> players = new HashMap<>();
        private char[][] board = new char[3][3];
        private String currentPlayer;

        private String[] playerChat;

        public GameSession(String player1, PlayerInfo info1, String player2, PlayerInfo info2) {
            players.put(player1, info1);
            players.put(player2, info2);

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

            players.get(currentPlayer).client.assignCharacter('X', "Game started. You play first!");
            players.get(currentPlayer).client.notifyTurn();
            players.get(otherPlayer).client.assignCharacter('O', "Game started. Waiting for opponent's move.");
        }

        public char makeMove(String playerName, int row, int col) throws RemoteException {
            if (row < 0 || row >= 3 || col < 0 || col >= 3 || board[row][col] != ' ') {
                return 'I';  // Invalid move
            }

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
                String winningMessage = "Game Over! The winner is ";
                String youWon = winningMessage + "You!";
                String opponentWon = winningMessage + "Your opponent!";

                if (result == 'D') {
                    players.get(playerName).client.displayMessage("Game Over! It's a draw!");
                    players.get(otherPlayer).client.displayMessage("Game Over! It's a draw!");
                } else {
                    players.get(playerName).client.displayMessage(result == players.get(playerName).symbol ? youWon : opponentWon);
                    players.get(otherPlayer).client.displayMessage(result == players.get(otherPlayer).symbol ? youWon : opponentWon);
                }
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
    }

    private HashMap<String, PlayerInfo> players = new HashMap<>();
    private Queue<String> waitingPlayers = new LinkedList<>();
    private HashMap<String, GameSession> activeGames = new HashMap<>();

    public TicTacToeServer() throws RemoteException {
        super();
    }

    @Override
    public synchronized void registerPlayer(String playerName, ClientCallback client) throws RemoteException {
        PlayerInfo newPlayer = new PlayerInfo(client, '-');

        // If there's a player waiting, pair them up and start a game.
        if (!waitingPlayers.isEmpty()) {
            String opponentName = waitingPlayers.poll();
            GameSession newGame = new GameSession(playerName, newPlayer, opponentName, players.remove(opponentName));
            activeGames.put(playerName, newGame);
            activeGames.put(opponentName, newGame);
            newGame.assignCharactersAndStart();
        } else {
            waitingPlayers.add(playerName);
            players.put(playerName, newPlayer);
        }
    }

    @Override
    public synchronized Character makeMove(String playerName, int row, int col) throws RemoteException {
        GameSession game = activeGames.get(playerName);
        if (game != null) {
            return game.makeMove(playerName, row, col);
        }
        return 'I';  // If there's no such game, consider it an invalid move
    }


    public static void main(String[] args) {
        try {
            TicTacToeServer server = new TicTacToeServer();
            Naming.rebind("rmi://localhost/TicTacToeServer", server);
            System.out.println("TicTacToe Server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
