package Server;

import Client.TicTacToeClient;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;

public class TicTacToeServer extends UnicastRemoteObject implements TicTacToe {
    private Map<String, TicTacToeClient> players;
    private char[][] board;

    public TicTacToeServer() throws RemoteException {
        super();
        players = new HashMap<>();
        board = new char[3][3];

        try {
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("TicTacToeServer", this);
            System.out.println("Server is running...");
        } catch (Exception e) {
            System.err.println("TicTacToeServer exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean registerPlayer(String username, TicTacToeClient client) throws RemoteException {
        if (!players.containsKey(username)) {
            players.put(username, client);
            return true;
        }
        return false;
    }

    @Override
    public void makeMove(int row, int col, String username) throws RemoteException {
    }

    @Override
    public void sendChatMessage(String message, String username) throws RemoteException {
        for (TicTacToeClient client : players.values()) {
            client.receiveChatMessage(username + ": " + message);
        }
    }

    @Override
    public char[][] getGameBoard() throws RemoteException {
        return board;
    }
}

