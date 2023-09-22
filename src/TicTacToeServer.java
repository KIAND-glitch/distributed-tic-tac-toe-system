import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
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
    public String startGame(String player1, String player2) throws RemoteException {
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
    public boolean makeMove(int row, int col, String username) throws RemoteException {

        if (row < 0 || row >= 3 || col < 0 || col >= 3) {
            System.out.println("here is the error");
            return false;
        }

        if (board[row][col] == 0) {
            char symbol = gameChars.get(username);
            board[row][col] = symbol;
            return true;
        }
        System.out.println("last line");
        return false;
    }

    @Override
    public Character evaluateGame() throws RemoteException {
        for (int i = 0; i < 3; i++) {
            // row win
            if (board[i][0] == board[i][1] && board[i][1] == board[i][2] && board[i][0] != 0) {
                return board[i][0];
            }
            // column win
            if (board[0][i] == board[1][i] && board[1][i] == board[2][i] && board[0][i] != 0) {
                return board[0][i];
            }
        }

        // diagonal win
        if (board[0][0] == board[1][1] && board[1][1] == board[2][2] && board[0][0] != 0) {
            return board[0][0];
        }
        if (board[0][2] == board[1][1] && board[1][1] == board[2][0] && board[0][2] != 0) {
            return board[0][2];
        }

        // Check for a draw
        boolean isBoardFull = true;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == 0) {
                    isBoardFull = false;
                    break;
                }
            }
        }
        if (isBoardFull) {
            return 'D';
        }

        // Game is still ongoing
        return 'C';
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
