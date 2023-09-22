package Server;

import Client.ClientCallback;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {
    void registerPlayer(String playerName, ClientCallback client) throws RemoteException;
    void makeMove(String playerName, int row, int col) throws RemoteException;
    // Additional methods as required
}
