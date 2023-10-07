package Server;

import Client.ClientCallback;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class TicTacToeServer extends UnicastRemoteObject implements ServerInterface {

    private HashMap<String, PlayerInfo> players = new HashMap<>();
    private Queue<String> waitingPlayers = new LinkedList<>();
    private HashMap<String, GameSession> activeGames = new HashMap<>();

    private HashMap<String, PlayerRanking> playerRankings = new HashMap<>();

    private HashMap<String, Long> lastHeartbeat = new HashMap<>();


    public TicTacToeServer() throws RemoteException {
        super();
        new Thread(() -> {
            while (true) {
                long now = System.currentTimeMillis();
                for (Map.Entry<String, Long> entry : lastHeartbeat.entrySet()) {
                    if (now - entry.getValue() > 5000) {
                        handleClientDisconnection(entry.getKey());
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    @Override
    public synchronized void sendHeartbeat(String playerName) throws RemoteException {
        System.out.println("Received heartbeat from: " + playerName);
        lastHeartbeat.put(playerName, System.currentTimeMillis());
    }

    private synchronized void handleClientDisconnection(String playerName) {
        System.out.println("Client " + playerName + " is disconnected.");
        lastHeartbeat.remove(playerName);
        // Handle the disconnection, e.g., end the game, notify the opponent, etc.
    }



    @Override
    public synchronized void registerPlayer(String playerName, ClientCallback client) throws RemoteException {
        PlayerInfo newPlayer = new PlayerInfo(client, '-');

        if (!playerRankings.containsKey(playerName)) {
            playerRankings.put(playerName, new PlayerRanking(playerName));
        }

        // If there's a player waiting, pair them up and start a game.
        if (!waitingPlayers.isEmpty()) {
            String opponentName = waitingPlayers.poll();
            GameSession newGame = new GameSession(playerName, newPlayer, opponentName, players.remove(opponentName), this);
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

    public void updatePointsAfterGame(String playerName, int points) {
        if (playerRankings.containsKey(playerName)) {
            if (points > 0) {
                playerRankings.get(playerName).addPoints(points);
            } else {
                playerRankings.get(playerName).subtractPoints(points);
            }
        }
    }

    public int getPlayerRank(String playerName) {
        List<PlayerRanking> sortedPlayers = new ArrayList<>(playerRankings.values());
        sortedPlayers.sort((player1, player2) -> Integer.compare(player2.getPoints(), player1.getPoints()));

        for (int i = 0; i < sortedPlayers.size(); i++) {
            if (sortedPlayers.get(i).getPoints() == playerRankings.get(playerName).getPoints()) {
                return i + 1;  // ranks start from 1
            }
        }
        return -1;  // if player not found (this shouldn't happen)
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
