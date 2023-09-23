package Client;


import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientCallback extends Remote {
    void displayMessage(String message) throws RemoteException;
    public void gameStarted(String opponentName) throws RemoteException;
    public void assignCharacter(Character character, String startMessage) throws RemoteException;
    public void notifyTurn() throws RemoteException;
}
