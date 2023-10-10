package Server;

import Client.ClientCallback;

public class PlayerInfo {
    ClientCallback client;
    char symbol;
    public PlayerInfo(ClientCallback client, char symbol) {
        this.client = client;
        this.symbol = symbol;
    }

    public void setClient(ClientCallback client) {
        this.client = client;
    }
}
