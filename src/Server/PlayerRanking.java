package Server;

public class PlayerRanking {
    private int points;

    public PlayerRanking() {
        this.points = 0;
    }

    public void addPoints(int pointsToAdd) {
        this.points += pointsToAdd;
    }

    public void subtractPoints(int pointsToSubtract) {
        this.points += pointsToSubtract;
    }

    public int getPoints() {
        return this.points;
    }

}
