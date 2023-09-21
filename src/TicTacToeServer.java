import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;

public class TicTacToeServer extends UnicastRemoteObject implements TicTacToe {
    private Map<String, String> players;
    private char[][] board;
    private String currentPlayer;

    public TicTacToeServer() throws RemoteException {
        super();
        players = new HashMap<>();
        board = new char[3][3];
        currentPlayer = null;
    }

    public static void main(String[] args) {
        try {
            java.rmi.registry.LocateRegistry.createRegistry(1099);
            TicTacToeServer obj = new TicTacToeServer();
            java.rmi.Naming.rebind("TicTacToeServer", obj);
            System.out.println("Server is running...");
        } catch (Exception e) {
            System.err.println("TicTacToeServer exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean registerPlayer(String username) throws RemoteException {
        if (!players.containsKey(username)) {
            players.put(username, username);
            players.put("second player", "second player");
            if (currentPlayer == null) {
                currentPlayer = username;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean makeMove(int row, int col, String username) throws RemoteException {
        if (!username.equals(currentPlayer)) {
            return false;
        }

        if (row < 0 || row >= 3 || col < 0 || col >= 3) {
            return false;
        }

        if (board[row][col] == 0) {
            char symbol = (currentPlayer.equals(players.keySet().toArray()[0])) ? 'X' : 'O';
            board[row][col] = symbol;
            currentPlayer = (String) ((currentPlayer.equals(players.keySet().toArray()[0])) ? players.keySet().toArray()[1] : players.keySet().toArray()[0]);
            return true;
        }
        return false;
    }



    @Override
    public void sendChatMessage(String message, String username) throws RemoteException {

    }

    @Override
    public void receiveChatMessage(String message) throws RemoteException {

    }

    @Override
    public char[][] getGameBoard() throws RemoteException {
        return board;
    }
}
