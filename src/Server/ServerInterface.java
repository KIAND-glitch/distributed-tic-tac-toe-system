package Server;

import Client.ClientCallback;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {
    void registerPlayer(String playerName, ClientCallback client, Boolean newGame) throws RemoteException;
    void makeMove(String playerName, int row, int col) throws RemoteException;
    void sendMessageToOpponent(int playerRank, String playerName, String message) throws RemoteException;
    void quitGame(String playerName, Boolean gameOver) throws RemoteException;
    void sendHeartbeat(String playerName) throws RemoteException;
    char[][] getCurrentBoardState(String playerName) throws RemoteException;
    void drawGameDisconnection(String playerName, String opponentName) throws RemoteException;
}
