package Client;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import Server.TicTacToeServer;
public class TicTacToeClient {
    private TicTacToeServer server;
    private String username;

    public TicTacToeClient(String serverIP, int serverPort, String username) {
        try {
            Registry registry = LocateRegistry.getRegistry(serverIP, serverPort);
            this.server = (TicTacToeServer) registry.lookup("TicTacToeServer");
            this.username = username;

            if (server.registerPlayer(username, this)) {
                System.out.println("Registered successfully.");
            } else {
                System.out.println("Username is already taken.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void makeMove(int row, int col) {
        try {
            server.makeMove(row, col, username);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void sendChatMessage(String message) {
        try {
            server.sendChatMessage(message, username);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String serverIP = "localhost";
        int serverPort = 1099;
        String username = "Player1";

        TicTacToeClient client = new TicTacToeClient(serverIP, serverPort, username);
    }

    public void receiveChatMessage(String s) {
    }
}

