import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TicTacToe extends Remote {

    // game admin
    boolean registerPlayer(String username) throws RemoteException;

    // game functionality
    String startGame(String player1, String player2) throws RemoteException;
    String makeMove(int row, int col, String username) throws RemoteException;
    void sendChatMessage(String message, String username) throws RemoteException;
    void receiveChatMessage(String message) throws RemoteException;
    char[][] getGameBoard() throws RemoteException;
}

