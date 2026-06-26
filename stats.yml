package studio.spark.duels.scoreboard;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

/** A single player's flicker-free sidebar. */
public class Board {

    private final Player player;
    private Scoreboard scoreboard;
    private Objective objective;
    private final List<Team> teams = new ArrayList<>();
    private String currentKey = "";
    private int lineCount = -1;

    public Board(Player player) { this.player = player; }

    public void show(String key, Component title, List<Component> lines) {
        if (lines.size() > 15) lines = lines.subList(0, 15);
        if (scoreboard == null || !key.equals(currentKey) || lines.size() != lineCount) {
            rebuild(title, lines);
            currentKey = key;
            lineCount = lines.size();
        } else {
            objective.displayName(title);
            for (int i = 0; i < lines.size(); i++) teams.get(i).prefix(lines.get(i));
        }
    }

    private void rebuild(Component title, List<Component> lines) {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        teams.clear();
        objective = scoreboard.registerNewObjective("spark", Criteria.DUMMY, title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (int i = 0; i < lines.size(); i++) {
            String entry = ChatColor.values()[i % ChatColor.values().length].toString();
            Team team = scoreboard.registerNewTeam("l" + i);
            team.addEntry(entry);
            team.prefix(lines.get(i));
            objective.getScore(entry).setScore(lines.size() - i);
            teams.add(team);
        }
        player.setScoreboard(scoreboard);
    }

    public void clear() {
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        scoreboard = null;
        currentKey = "";
        lineCount = -1;
    }
}
