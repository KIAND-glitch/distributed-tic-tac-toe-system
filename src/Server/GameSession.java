package Server;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Random;

public class GameSession {
    private HashMap<String, PlayerInfo> players = new HashMap<>();
    private char[][] board = new char[3][3];
    private String currentPlayer;
    private TicTacToeServer server;
    private Random random = new Random();

    public GameSession(String player1, PlayerInfo info1, String player2, PlayerInfo info2, TicTacToeServer server) {
        players.put(player1, info1);
        players.put(player2, info2);
        this.server = server;
        initBoard();
        currentPlayer = random.nextBoolean() ? player1 : player2;
    }

    private void initBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = ' ';
            }
        }
    }

    public void assignCharactersAndStart() throws RemoteException {
        players.get(currentPlayer).symbol = 'X';
        String otherPlayer = getOtherPlayer(currentPlayer);
        players.get(otherPlayer).symbol = 'O';

        notifyPlayers('O', 'X', currentPlayer, otherPlayer);
    }

    private void notifyPlayers(char currentChar, char otherChar, String currentPlayer, String otherPlayer) throws RemoteException {
        players.get(otherPlayer).client.assignCharacter(currentChar, currentPlayer, server.getPlayerRank(otherPlayer), server.getPlayerRank(currentPlayer));
        players.get(currentPlayer).client.assignCharacter(otherChar, otherPlayer, server.getPlayerRank(currentPlayer), server.getPlayerRank(otherPlayer));
        players.get(currentPlayer).client.notifyTurn();
    }

    public char makeMove(String playerName, int row, int col) throws RemoteException {
        updateBoard(playerName, row, col);

        char result = evaluateGame();
        handleGameEnd(result, playerName);

        return result;
    }

    private void updateBoard(String playerName, int row, int col) throws RemoteException {
        board[row][col] = players.get(playerName).symbol;
        players.get(playerName).client.displayBoard(board);

        String otherPlayer = getOtherPlayer(playerName);
        players.get(otherPlayer).client.displayBoard(board);
    }

    private void handleGameEnd(char result, String playerName) throws RemoteException {
        String otherPlayer = getOtherPlayer(playerName);

        if (result == 'N') {
            currentPlayer = otherPlayer;
            players.get(currentPlayer).client.notifyTurn();
        } else {
            if (result == 'D') {
                informPlayers("Match Drawn", playerName, otherPlayer);
                server.updatePointsAfterGame(playerName, 2);
                server.updatePointsAfterGame(otherPlayer, 2);
            } else {
                handlePlayerWin(result, playerName, otherPlayer);
            }
            askForRematch();
        }
    }

    private void handlePlayerWin(char result, String playerName, String otherPlayer) throws RemoteException {
        if (result == players.get(playerName).symbol) {
            informPlayers("Player " + playerName + " wins!", playerName, otherPlayer);
            server.updatePointsAfterGame(playerName, 5);
            server.updatePointsAfterGame(otherPlayer, -5);
        } else {
            informPlayers("Player " + otherPlayer + " wins!", playerName, otherPlayer);
            server.updatePointsAfterGame(playerName, -5);
            server.updatePointsAfterGame(otherPlayer, 5);
        }
    }

    private void informPlayers(String message, String playerName, String otherPlayer) throws RemoteException {
        players.get(playerName).client.updateGameInfo(message);
        players.get(otherPlayer).client.updateGameInfo(message);
    }

    public char evaluateGame() {
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == board[i][1] && board[i][1] == board[i][2] && board[i][0] != ' ') return board[i][0];
            if (board[0][i] == board[1][i] && board[1][i] == board[2][i] && board[0][i] != ' ') return board[0][i];
        }
        if (board[0][0] == board[1][1] && board[1][1] == board[2][2] && board[0][0] != ' ') return board[0][0];
        if (board[0][2] == board[1][1] && board[1][1] == board[2][0] && board[0][2] != ' ') return board[0][2];

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == ' ') return 'N';
            }
        }
        return 'D';
    }

    public void sendMessage(String sender, String message) throws RemoteException {
        String receiver = getOtherPlayer(sender);
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

    public HashMap<String, PlayerInfo> getPlayers() {
        return players;
    }
}
