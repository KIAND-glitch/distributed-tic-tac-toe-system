package Server;

import Client.ClientCallback;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class TicTacToeServer extends UnicastRemoteObject implements ServerInterface {

    private HashMap<String, PlayerInfo> players = new HashMap<>();
    private Queue<String> waitingPlayers = new LinkedList<>();
    private HashMap<String, GameSession> activeGames = new HashMap<>();

    public TicTacToeServer() throws RemoteException {
        super();
    }

    @Override
    public synchronized void registerPlayer(String playerName, ClientCallback client) throws RemoteException {
        PlayerInfo newPlayer = new PlayerInfo(client, '-');

        // If there's a player waiting, pair them up and start a game.
        if (!waitingPlayers.isEmpty()) {
            String opponentName = waitingPlayers.poll();
            GameSession newGame = new GameSession(playerName, newPlayer, opponentName, players.remove(opponentName));
            activeGames.put(playerName, newGame);
            activeGames.put(opponentName, newGame);
            newGame.assignCharactersAndStart();
        } else {
            waitingPlayers.add(playerName);
            players.put(playerName, newPlayer);
        }
    }

    @Override
    public void sendMessageToOpponent(String playerName, String message) throws RemoteException {
        if (activeGames.containsKey(playerName)) {
            activeGames.get(playerName).sendMessage(playerName, message);
        }
    }

    @Override
    public synchronized Character makeMove(String playerName, int row, int col) throws RemoteException {
        GameSession game = activeGames.get(playerName);
        if (game != null) {
            return game.makeMove(playerName, row, col);
        }
        return 'I';
    }

    @Override
    public synchronized void quitGame(String playerName) throws RemoteException {
        GameSession gameSession = activeGames.get(playerName);
        if (gameSession != null) {
            String otherPlayer = gameSession.getPlayers().keySet().stream().filter(p -> !p.equals(playerName)).findFirst().get();
            gameSession.getPlayers().get(otherPlayer).client.displayMessage(playerName + " has quit the game! You are the winner!");

            // Cleanup resources
            activeGames.remove(playerName);
            activeGames.remove(otherPlayer);
        }
    }

    public static void main(String[] args) {
        try {
            TicTacToeServer server = new TicTacToeServer();
            Naming.rebind("rmi://localhost/TicTacToeServer", server);
            System.out.println("TicTacToe Server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
