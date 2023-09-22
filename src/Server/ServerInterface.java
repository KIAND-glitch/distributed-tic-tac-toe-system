package Server;

import Client.ClientCallback;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {
    void registerPlayer(String playerName, ClientCallback client) throws RemoteException;
    boolean makeMove(String playerName, int row, int col) throws RemoteException;
    public Character evaluateGame() throws RemoteException;

}
