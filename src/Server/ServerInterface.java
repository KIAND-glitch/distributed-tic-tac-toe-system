package Server;

import Client.ClientCallback;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {
    void registerPlayer(String playerName, ClientCallback client, Boolean newGame) throws RemoteException;
    Character makeMove(String playerName, int row, int col) throws RemoteException;
    void sendMessageToOpponent(String playerName, String message) throws RemoteException;
    void quitGame(String playerName, Boolean gameOver) throws RemoteException;
    void sendHeartbeat(String playerName) throws RemoteException;
    char[][] getCurrentBoardState(String playerName) throws RemoteException;
}
