package Server;

public class PlayerRanking {
    private String playerName;
    private int points;

    public PlayerRanking(String playerName) {
        this.playerName = playerName;
        this.points = 0;
    }

    public void addPoints(int pointsToAdd) {
        this.points += pointsToAdd;
    }

    public void subtractPoints(int pointsToSubtract) {
        this.points -= pointsToSubtract;
    }

    public int getPoints() {
        return this.points;
    }

}
