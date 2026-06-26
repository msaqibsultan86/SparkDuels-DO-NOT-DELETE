package studio.spark.duels.stats;

public class PlayerStats {
    public int wins, losses, kills, deaths, streak, bestStreak, elo, coins;
    public int ffaKills, ffaDeaths, ffaStreak, ffaBestStreak;

    public PlayerStats(int elo) { this.elo = elo; }

    public double kdr() { return deaths == 0 ? kills : (double) kills / deaths; }

    public int winrate() {
        int total = wins + losses;
        return total == 0 ? 0 : (int) Math.round(100.0 * wins / total);
    }

    public double ffaKdr() { return ffaDeaths == 0 ? ffaKills : (double) ffaKills / ffaDeaths; }
}
