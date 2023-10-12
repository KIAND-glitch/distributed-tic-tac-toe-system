package Client;

import Server.ServerInterface;
import javax.swing.*;
import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.Timer;

public class TicTacToeClient extends UnicastRemoteObject implements ClientCallback {

    private ServerInterface server;
    private String playerName;
    private Character playerChar;
    private int playerRank = -1;
    private String opponentName;
    private Character opponentChar;
    private int opponentRank;
    private boolean isExported = false;
    private boolean myTurn = false;
    private TicTacToeGUI gui;

    public TicTacToeClient(String playerName) throws Exception {
        this.playerName = playerName;

        try {
            server = (ServerInterface) Naming.lookup("rmi://localhost/TicTacToeServer");

            try {
                UnicastRemoteObject.unexportObject(this, true);
            } catch (NoSuchObjectException e) {
                e.printStackTrace();
            }

            if (!isExported) {
                ClientCallback clientStub = (ClientCallback) UnicastRemoteObject.exportObject(this, 0);
                gui = new TicTacToeGUI(this);
                isExported = true;
                server.registerPlayer(playerName, clientStub, false);
                startHeartbeat();
            }
        } catch (RemoteException | NotBoundException e) {
            SwingUtilities.invokeLater(() -> {
                final JOptionPane optionPane = new JOptionPane("Server unavailable", JOptionPane.ERROR_MESSAGE);
                final JDialog dialog = optionPane.createDialog("Error");

                java.util.Timer timer = new java.util.Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        dialog.dispose();
                        System.exit(0);
                    }
                }, 5000); // 5 seconds

                dialog.setVisible(true);
            });

        }
    }
    private void startHeartbeat() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    server.sendHeartbeat(playerName);
                } catch (RemoteException e) {
                    SwingUtilities.invokeLater(() -> {
                        final JOptionPane optionPane = new JOptionPane("Server unavailable", JOptionPane.ERROR_MESSAGE);
                        final JDialog dialog = optionPane.createDialog("Error");

                        java.util.Timer timer = new java.util.Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                dialog.dispose();
                                System.exit(0);
                            }
                        }, 5000);

                        dialog.setVisible(true);
                    });

                }
            }
        }, 0, 2000);
    }

    @Override
    public void updateGameInfo(String updatedGameInfo) throws RemoteException {
        gui.updateGameInfo(updatedGameInfo);
    }

    @Override
    public void notifyTurn() throws RemoteException {
        if(gui == null) {
            System.out.println("GUI has not been initialized!");
            return;
        }

        String info = "Rank#" + playerRank + " " + playerName + "'s turn (" + playerChar + ")";

        gui.updateGameInfo(info);
        myTurn = true;
        gui.setupCountdownTimer();
    }

    @Override
    public void displayBoard(char[][] board) throws RemoteException {
        if(gui == null) {
            System.out.println("GUI has not been initialized!");
            return;
        }
        gui.updateButtonsText(board);
    }

    @Override
    public void displayMessage(String message) throws RemoteException {
        gui.displayMessage(message);
    }

    @Override
    public void assignCharacter(Character character, String opponentName, int rank, int opponentRank) throws RemoteException {
        this.playerChar = character;
        this.opponentName = opponentName;
        this.opponentChar = character == 'X'? '0':'X';
        this.playerRank = rank;
        this.opponentRank = opponentRank;

        gui.updateGameInfo("Rank#" + opponentRank + " " +opponentName + "'s turn (" + opponentChar + ")");
    }

    @Override
    public void resumeGame() throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            gui.resumeGameGui();

            try {
                char[][] board = server.getCurrentBoardState(playerName);
                gui.updateButtonsText(board);

                if (myTurn) {
                    notifyTurn();
                } else {
                    gui.updateGameInfo("Rank#" + opponentRank + " " +opponentName + "'s turn (" + opponentChar + ")");
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void handlePause() throws RemoteException{
        SwingUtilities.invokeLater(() -> {
            gui.handleGamePause();
        });
    }

    @Override
    public void askToPlayAgain() throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            if (gui.showPlayAgainDialog()) {
                gui.resetGameGui();
                try {
                    server.registerPlayer(playerName, this, true);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    server.quitGame(playerName, true);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
                System.exit(0);
            }
        });
    }

    @Override
    public void refreshBoard() throws RemoteException{
        gui.refreshBoard();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java TicTacToeClient <username>");
            return;
        }
        String playerName = args[0];
        try {
            new TicTacToeClient(playerName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // getters and setters

    public ServerInterface getServer() {
        return server;
    }

    public boolean isMyTurn() {
        return myTurn;
    }

    public void setMyTurn(boolean myTurn) {
        this.myTurn = myTurn;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getOpponentName(){
        return opponentName;
    }

    public Character getPlayerChar() {
        return playerChar;
    }

    public Character getOpponentChar() {
        return opponentChar;
    }

    public int getPlayerRank() {
        return playerRank;
    }

    public int getOpponentRank() {
        return opponentRank;
    }
}
