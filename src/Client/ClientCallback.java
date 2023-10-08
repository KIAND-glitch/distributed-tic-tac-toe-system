package Client;


import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientCallback extends Remote {
    void displayMessage(String message) throws RemoteException;
    void assignCharacter(Character character, String startMessage, int rank) throws RemoteException;
    void notifyTurn() throws RemoteException;
    void displayBoard(char[][] board) throws RemoteException;
    void askToPlayAgain() throws RemoteException;
    void resumeGame() throws RemoteException;
    void handlePause() throws RemoteException;
}
