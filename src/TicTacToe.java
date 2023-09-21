import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TicTacToe extends Remote {
    boolean registerPlayer(String username) throws RemoteException;
    boolean makeMove(int row, int col, String username) throws RemoteException;
    void sendChatMessage(String message, String username) throws RemoteException;
    void receiveChatMessage(String message) throws RemoteException;
    char[][] getGameBoard() throws RemoteException;
}

