package Server;

import Client.ClientCallback;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TicTacToeServer extends UnicastRemoteObject implements ServerInterface {

    private class PlayerInfo {
        ClientCallback stub;
        char character;
        PlayerInfo(ClientCallback stub, char character) {
            this.stub = stub;
            this.character = character;
        }
    }

    private Map<String, PlayerInfo> players = new HashMap<>();
    private char[][] board = new char[3][3];
    private String currentPlayer = null;

    protected TicTacToeServer() throws RemoteException {
        super();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = '-';
            }
        }
    }

    private void notifyNextPlayer() throws RemoteException {
        for (String player : players.keySet()) {
            if (!player.equals(currentPlayer)) {
                players.get(player).stub.notifyTurn();
                currentPlayer = player;
                break;
            }
        }
    }

    public synchronized void registerPlayer(String playerName, ClientCallback clientStub) throws RemoteException {
        if (players.size() >= 2) {
            throw new RemoteException("Room is already full!");
        }

        if (!players.containsKey(playerName)) {
            players.put(playerName, new PlayerInfo(clientStub, '-'));
        }

        if (players.size() == 2) {
            assignCharactersAndStart();
        } else {
            currentPlayer = playerName;
        }
    }

    private void assignCharactersAndStart() {
        Random random = new Random();
        boolean isFirstPlayerStarts = random.nextBoolean();

        char player1Char = random.nextBoolean() ? 'X' : 'O';
        char player2Char = player1Char == 'X' ? 'O' : 'X';

        String[] playerNames = players.keySet().toArray(new String[0]);

        players.get(playerNames[0]).character = player1Char;
        players.get(playerNames[1]).character = player2Char;

        try {
            players.get(playerNames[0]).stub.assignCharacter(player1Char, isFirstPlayerStarts ? "You start first!" : "Wait for Player 2 to start.");
            players.get(playerNames[1]).stub.assignCharacter(player2Char, isFirstPlayerStarts ? "Wait for Player 1 to start." : "You start first!");

            if (isFirstPlayerStarts) {
                currentPlayer = playerNames[0];
            } else {
                currentPlayer = playerNames[1];
            }
            notifyNextPlayer();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized boolean makeMove(String playerName, int row, int col) throws RemoteException {
        if (row < 0 || row >= 3 || col < 0 || col >= 3) {
            return false;
        }

        if (board[row][col] == '-' && players.containsKey(playerName)) {
            board[row][col] = players.get(playerName).character;

            for (String player : players.keySet()) {
                if (!player.equals(playerName)) {
                    board[row][col] = players.get(player).character;
                    break;
                }
            }
            printGameBoard();
            notifyNextPlayer();
            return true;
        }
        return false;
    }

    private void printGameBoard() throws RemoteException {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                System.out.print(board[i][j] + " ");
            }
            System.out.println();
        }
    }

    @Override
    public Character evaluateGame() throws RemoteException {
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == board[i][1] && board[i][1] == board[i][2] && board[i][0] != '-') {
                return board[i][0];
            }
            if (board[0][i] == board[1][i] && board[1][i] == board[2][i] && board[0][i] != '-') {
                return board[0][i];
            }
        }
        if (board[0][0] == board[1][1] && board[1][1] == board[2][2] && board[0][0] != '-') {
            return board[0][0];
        }
        if (board[0][2] == board[1][1] && board[1][1] == board[2][0] && board[0][2] != '-') {
            return board[0][2];
        }
        boolean isBoardFull = true;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == '-') {
                    isBoardFull = false;
                    break;
                }
            }
        }
        if (isBoardFull) {
            return 'D';
        }
        return 'C';
    }

    public synchronized char[][] getGameBoard() throws RemoteException {
        return board;
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
