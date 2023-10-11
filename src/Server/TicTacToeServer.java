package Server;

import Client.ClientCallback;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TicTacToeServer extends UnicastRemoteObject implements ServerInterface {

    private HashMap<String, PlayerInfo> players = new HashMap<>();
    private Queue<String> waitingPlayers = new LinkedList<>();
    private HashMap<String, GameSession> activeGames = new HashMap<>();
    private HashMap<String, PlayerRanking> playerRankings = new HashMap<>();
    private ConcurrentHashMap<String, Long> lastHeartbeat = new ConcurrentHashMap<>();

    public TicTacToeServer() throws RemoteException {
        super();
        new Thread(() -> {
            while (true) {
                long now = System.currentTimeMillis();
                lastHeartbeat.forEach((player, timestamp) -> {
                    if (now - timestamp > 3000) {
                        handleClientDisconnection(player);
                    }
                });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public synchronized void registerPlayer(String playerName, ClientCallback client, Boolean newGame) throws RemoteException {
        PlayerInfo newPlayer = new PlayerInfo(client, '-');

        if (!newGame) {
            // Check if the player was in an active game before disconnection
            System.out.println("connection new connection attempt");
            GameSession previousGame = activeGames.get(playerName);
            System.out.println("previous game" + previousGame);
            if (previousGame != null) {
                newPlayer = previousGame.getPlayers().get(playerName);
                newPlayer.setClient(client);
                previousGame.reconnection(playerName, newPlayer);
                String otherPlayer = previousGame.getPlayers().keySet().stream().filter(p -> !p.equals(playerName)).findFirst().get();
                try {
                    previousGame.getPlayers().get(playerName).client.resumeGame();
                    previousGame.getPlayers().get(otherPlayer).client.resumeGame();
                    return;
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        if (!playerRankings.containsKey(playerName)) {
            playerRankings.put(playerName, new PlayerRanking());
        }

        matchPlayers(playerName, newPlayer);
    }

    @Override
    public void sendMessageToOpponent(int playerRank, String playerName, String message) throws RemoteException {
        if (activeGames.containsKey(playerName)) {
            activeGames.get(playerName).sendMessage(playerRank, playerName, message);
        }
    }

    private synchronized void matchPlayers(String playerName, PlayerInfo playerInfo) {
        if (!waitingPlayers.isEmpty()) {
            String opponentName = waitingPlayers.poll();
            GameSession newGame = new GameSession(playerName, playerInfo, opponentName, players.remove(opponentName), this);
            activeGames.put(playerName, newGame);
            activeGames.put(opponentName, newGame);
            try {
                newGame.assignCharactersAndStart();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            waitingPlayers.add(playerName);
            players.put(playerName, playerInfo);
        }
    }

    @Override
    public synchronized void makeMove(String playerName, int row, int col) throws RemoteException {
        GameSession game = activeGames.get(playerName);
        if (game != null) {
            game.makeMove(playerName, row, col);
        }
    }

    @Override
    public synchronized void quitGame(String playerName, Boolean gameOver) throws RemoteException {
        GameSession gameSession = activeGames.get(playerName);
        if (gameSession != null) {
            String otherPlayer = gameSession.getPlayers().keySet().stream().filter(p -> !p.equals(playerName)).findFirst().get();
            activeGames.remove(playerName);
            if (!gameOver) {
                gameSession.getPlayers().get(otherPlayer).client.updateGameInfo(playerName + " quit! Player '" + otherPlayer + "' wins");
                matchPlayers(otherPlayer, gameSession.getPlayers().get(otherPlayer));
                updatePointsAfterGame(otherPlayer, 5);
                gameSession.getPlayers().get(otherPlayer).client.refreshBoard();
                gameSession.getPlayers().get(otherPlayer).client.askToPlayAgain();
            }
        }
    }

    public void drawGameDisconnection(String playerName, String opponentName) throws RemoteException {
        updatePointsAfterGame(playerName, 2);
        updatePointsAfterGame(opponentName, 2);
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

    public char[][] getCurrentBoardState(String playerName) throws RemoteException {
        return activeGames.get(playerName).getBoard();
    }

    @Override
    public synchronized void sendHeartbeat(String playerName) throws RemoteException {
        System.out.println("heartbeat recieved" + playerName);
        lastHeartbeat.put(playerName, System.currentTimeMillis());
    }

    private void handleClientDisconnection(String playerName) {
        System.out.println("Client " + playerName + " is disconnected.");

        GameSession gameSession = activeGames.get(playerName);
        if (gameSession != null) {
            String otherPlayer = gameSession.getOtherPlayer(playerName);
            try {
                gameSession.getPlayers().get(otherPlayer).client.handlePause();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        // It's safe to remove directly from a ConcurrentHashMap
        lastHeartbeat.remove(playerName);
        // Handle the disconnection, e.g., end the game, notify the opponent, etc.
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
