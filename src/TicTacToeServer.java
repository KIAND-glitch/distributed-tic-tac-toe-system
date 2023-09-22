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

    private Map<String, Character> gameChars;

    public TicTacToeServer() throws RemoteException {
        super();
        players = new HashMap<>();
        board = new char[3][3];
        // does this automatically make the chars of the board 0
        currentPlayer = null;
        gameChars = new HashMap<>();
    }

    @Override
    public boolean registerPlayer(String username) throws RemoteException {
        if (!players.containsKey(username)) {
            players.put(username, username);
            if (currentPlayer == null) {
                currentPlayer = username;
            }
            return true;
        }
        return false;
    }


    @Override
    public String makeMove(int row, int col, String username) throws RemoteException {
//        if (!username.equals(currentPlayer)) {
//            ''
//            return currentPlayer;
//        }

        if (row < 0 || row >= 3 || col < 0 || col >= 3) {
            System.out.println("here is the error");
            return currentPlayer;
        }

        if (board[row][col] == 0) {
            char symbol = gameChars.get(username);
            board[row][col] = symbol;

            // Switch to the other player's turn
            currentPlayer = (username.equals(players.keySet().toArray()[0])) ? players.keySet().toArray()[1].toString() : players.keySet().toArray()[0].toString();

            return currentPlayer;
        }
        System.out.println("last line");
        return currentPlayer;
    }


    @Override
    public String startGame(String player1, String player2) throws RemoteException {
        // Assign player 1 'X' and player 2 'O' randomly
        if (Math.random() < 0.5) {
            gameChars.put(player1, 'X');
            gameChars.put(player2, 'O');
            currentPlayer = player1;
        } else {
            gameChars.put(player1, 'O');
            gameChars.put(player2, 'X');
            currentPlayer = player2;
        }
        System.out.println("game chars" + gameChars);
        return currentPlayer;
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
}
