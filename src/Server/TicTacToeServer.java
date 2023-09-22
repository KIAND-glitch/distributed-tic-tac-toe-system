package Server;

import Client.ClientCallback;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class TicTacToeServer extends UnicastRemoteObject implements ServerInterface {

    // Store players and their callbacks
    private Map<String, ClientCallback> players = new HashMap<>();

    // Some kind of simple pairing mechanism for simplicity
    private String waitingPlayer = null;

    protected TicTacToeServer() throws RemoteException {
        super();
    }

    @Override
    public synchronized void registerPlayer(String playerName, ClientCallback client) throws RemoteException {
        players.put(playerName, client);

        // Check if there's another player waiting
        if (waitingPlayer == null) {
            waitingPlayer = playerName;
            client.displayMessage("Waiting for another player...");
        } else {
            // Notify both players that their game is ready to start
            ClientCallback waitingPlayerCallback = players.get(waitingPlayer);
            waitingPlayerCallback.gameReady(playerName);
            client.gameReady(waitingPlayer);

            // Reset waiting player
            waitingPlayer = null;
        }
    }

    @Override
    public void makeMove(String playerName, int row, int col) throws RemoteException {
        // Handle game logic here

        // Update both players' boards with the move made
        // For simplicity, let's assume the other player is always the opponent for now.
        for (Map.Entry<String, ClientCallback> entry : players.entrySet()) {
            if (!entry.getKey().equals(playerName)) {
                entry.getValue().updateBoard(row, col, 'X'); // 'X' or 'O' based on player's symbol
            }
        }
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

