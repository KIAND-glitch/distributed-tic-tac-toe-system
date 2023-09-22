package Client;


import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientCallback extends Remote {
    void gameReady(String opponentName) throws RemoteException;
    void updateBoard(int row, int col, char symbol) throws RemoteException;
    void displayMessage(String message) throws RemoteException;
}
