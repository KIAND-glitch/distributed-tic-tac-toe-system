package Server;

import Client.ClientCallback;
import Client.TicTacToeClient;

import java.rmi.ConnectException;
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
    private ConcurrentHashMap<String, Timer> disconnectionTimers = new ConcurrentHashMap<>();

    public TicTacToeServer() throws RemoteException {
        super();
        new Thread(() -> {
            while (true) {
                long now = System.currentTimeMillis();
                lastHeartbeat.forEach((player, timestamp) -> {
                    if (now - timestamp > 3000) {
                        try {
                            handleClientDisconnection(player);
                        } catch (RemoteException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        }).start();

        // Periodically print the leaderboard
        new Thread(() -> {
            while (true) {
                printLeaderboard();
                try {
                    Thread.sleep(10000); // 10 seconds interval
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public synchronized void registerPlayer(String playerName, ClientCallback client, Boolean newGame) throws RemoteException {
        PlayerInfo newPlayer = new PlayerInfo(client, '-');

        if (!newGame) {
            // Check if the player was in an active game before disconnection
            GameSession previousGame = activeGames.get(playerName);
            if (previousGame != null) {
                newPlayer = previousGame.getPlayers().get(playerName);
                String opponent = previousGame.getOtherPlayer(playerName);
                if (activeGames.get(opponent) != previousGame) {
                    activeGames.remove(playerName);
                    matchPlayers(playerName, newPlayer);
                }
                newPlayer.setClient(client);
                previousGame.reconnection(playerName, newPlayer);
                String otherPlayer = previousGame.getOtherPlayer(playerName);
                try {
                    previousGame.getPlayers().get(playerName).client.resumeGame();
                    previousGame.getPlayers().get(otherPlayer).client.resumeGame();
                    Timer disconnectionTimer = disconnectionTimers.get(playerName);
                    if (disconnectionTimer != null) {
                        disconnectionTimer.cancel();
                        disconnectionTimers.remove(playerName);
                    }
                    return;
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        initializePlayerRanking(playerName);

        matchPlayers(playerName, newPlayer);
    }

    private void initializePlayerRanking(String playerName) {
        if (!playerRankings.containsKey(playerName)) {
            playerRankings.put(playerName, new PlayerRanking());
        }
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
    public synchronized char makeMove(String playerName, int row, int col) throws RemoteException {
        GameSession game = activeGames.get(playerName);
        if (game != null) {
            return game.makeMove(playerName, row, col);
        }
        return 0;
    }

    @Override
    public synchronized void quitGame(String playerName, Boolean gameOver) throws RemoteException {
        GameSession gameSession = activeGames.get(playerName);
        if (gameSession != null) {
            String otherPlayer = gameSession.getOtherPlayer(playerName);
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
                return i + 1;
            }
        }
        return -1;
    }

    public char[][] getCurrentBoardState(String playerName) throws RemoteException {
        return activeGames.get(playerName).getBoard();
    }

    @Override
    public synchronized void sendHeartbeat(String playerName) throws RemoteException {
        lastHeartbeat.put(playerName, System.currentTimeMillis());
    }

    private boolean invokeHandlePause(ClientCallback client) {
        try {
            client.handlePause();
            return true;
        } catch (ConnectException e) {
            // Client is not available.
            return false;
        } catch (RemoteException e) {
            // Other RMI errors. Handle or log this as per your needs.
            e.printStackTrace();
            return false;
        }
    }


    private void handleClientDisconnection(String playerName) throws RemoteException {
        GameSession gameSession = activeGames.get(playerName);

        if (gameSession != null) {
            String otherPlayer = gameSession.getOtherPlayer(playerName);

            // Only proceed if the other player's client is still connected.
            if (invokeHandlePause(gameSession.getPlayers().get(otherPlayer).client)) {
                Timer disconnectionTimer = new Timer();
                disconnectionTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        terminateGameDueToDisconnection(playerName, otherPlayer);
                    }
                }, 30000);

                disconnectionTimers.put(playerName, disconnectionTimer);
            } else {
                // Handle scenario where both players are disconnected.
                activeGames.remove(playerName);
                activeGames.remove(otherPlayer);
                drawGameDisconnection(playerName, otherPlayer);
                return;
            }
        }
        lastHeartbeat.remove(playerName);
    }


    private synchronized void terminateGameDueToDisconnection(String playerName, String otherPlayer) {
        GameSession gameSession = activeGames.get(playerName);
        if (gameSession != null) {
            try {
                gameSession.getPlayers().get(otherPlayer).client.updateGameInfo(playerName + " did not reconnect in time. Game drawn.");
                gameSession.getPlayers().get(otherPlayer).client.refreshBoard();
                activeGames.remove(playerName);
                activeGames.remove(otherPlayer);
                drawGameDisconnection(playerName, otherPlayer);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        disconnectionTimers.remove(playerName);
    }

    private void printLeaderboard() {
        List<Map.Entry<String, PlayerRanking>> sortedRankings = new ArrayList<>(playerRankings.entrySet());

        sortedRankings.sort((entry1, entry2) -> Integer.compare(entry2.getValue().getPoints(), entry1.getValue().getPoints()));

        System.out.println("------ Leaderboard ------");
        for (int i = 0; i < sortedRankings.size(); i++) {
            String player = sortedRankings.get(i).getKey();
            int points = sortedRankings.get(i).getValue().getPoints();
            System.out.println((i + 1) + ". " + player + " - " + points + " points");
        }
        System.out.println("-------------------------");
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
