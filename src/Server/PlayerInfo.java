package Server;

import Client.ClientCallback;

import java.rmi.RemoteException;

public class PlayerInfo {
    ClientCallback client;
    char symbol;

    public PlayerInfo(ClientCallback client, char symbol) {
        this.client = client;
        this.symbol = symbol;
    }

    public void wantsToPlayAgain() throws RemoteException {
        client.askToPlayAgain();
    }


}
