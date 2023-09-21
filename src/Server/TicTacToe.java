package Server;

import Client.TicTacToeClient;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TicTacToe extends Remote {
    boolean registerPlayer(String username, TicTacToeClient client) throws RemoteException;
    void makeMove(int row, int col, String username) throws RemoteException;
    void sendChatMessage(String message, String username) throws RemoteException;
    char[][] getGameBoard() throws RemoteException;
}

