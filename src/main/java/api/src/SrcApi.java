package api.src;

import api.src.games.CategoryData;
import api.src.games.GameData;
import api.src.games.Games;
import api.src.games.GamesInterface;
import api.src.leaderboard.Leaderboard;
import api.src.leaderboard.LeaderboardInterface;
import api.src.leaderboard.Run;
import api.src.leaderboard.VariablesInput;
import jakarta.ws.rs.ClientErrorException;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SrcApi {
    private static final String BASE_URI = "https://www.speedrun.com/api/v1/";
    
    private final LeaderboardInterface leaderboardProxy;
    private final GamesInterface gamesProxy;
    
    
    public SrcApi(ResteasyClient client) {
        ResteasyWebTarget target = client.target(BASE_URI);
        leaderboardProxy = target.proxy(LeaderboardInterface.class);
        gamesProxy = target.proxy(GamesInterface.class);
    }
    
    public String getWr(SrcEnums.Category category) {
        Leaderboard leaderboard;
        Map<String, String> variables = new HashMap<>();
        for (SrcEnums.Variable variable : category.getVariables()) {
            variables.put(variable.getVarId(), variable.getValueId());
        }
        try {
            leaderboard = leaderboardProxy.getWr(
                    category.getGame().getId(),
                    category.getId(),
                    1,
                    "players",
                    new VariablesInput(variables)
            );
        } catch (ClientErrorException e) {
            System.out.println("Error getting SRC category:\n" + e.getMessage());
            return "";
        }
        List<Run> runList = leaderboard.getLeaderboardData().getRunList();
        if (runList.isEmpty()) {
            return String.format("%s - %s has no WR.", category.getGame(), category);
        }
        
        Run wrRun = runList.get(0);
        String username = leaderboard.getLeaderboardData().getPlayers().getPlayerData().get(0).getNames()
                .getInternational();
        BigDecimal rawTime = wrRun.getRunData().getTimes().getPrimaryTime();
        String timeString = getTimeString(rawTime);
        return String.format("The %s - %s WR is %s by %s", category.getGame(), category, timeString, username);
    }
    
    public String getWr(String gameName) {
        Games games;
        try {
            games = gamesProxy.getGameByName(gameName, "categories");
        } catch (ClientErrorException e) {
            System.out.println("Error getting SRC game:\n" + e.getMessage());
            return "";
        }
        if (games.getGameData().isEmpty() || games.getGameData().get(0).getCategories().getCategoryData().isEmpty()) {
            return "Unknown WR for " + gameName;
        }
        
        GameData gameData = games.getGameData().get(0);
        String gameId = gameData.getId();
        String outputName = gameData.getNames().getInternational();
        CategoryData categoryData = null;
        for (CategoryData category : gameData.getCategories().getCategoryData()) {
            if (category.getType().equals("per-game")) {
                categoryData = category;
                break;
            }
        }
        if (categoryData == null) {
            return "Unknown WR for " + outputName;
        }
        
        String categoryId = categoryData.getId();
        String categoryName = categoryData.getName();
        
        Leaderboard leaderboard;
        try {
            leaderboard = leaderboardProxy.getWr(gameId, categoryId, 1, "players", null);
        } catch (ClientErrorException e) {
            System.out.println("Error getting SRC category:\n" + e.getMessage());
            return "";
        }
        List<Run> runList = leaderboard.getLeaderboardData().getRunList();
        if (runList.isEmpty()) {
            return String.format("%s - %s has no WR.", outputName, categoryName);
        }
    
        Run wrRun = runList.get(0);
        String username = leaderboard.getLeaderboardData().getPlayers().getPlayerData().get(0).getNames()
                .getInternational();
        BigDecimal rawTime = wrRun.getRunData().getTimes().getPrimaryTime();
        String timeString = getTimeString(rawTime);
        return String.format("The %s - %s WR is %s by %s", outputName, categoryName, timeString, username);
    }
    
    private static String getTimeString(BigDecimal rawTime) {
        long ms = rawTime.multiply(new BigDecimal(1000)).longValue();
    
        long hours = TimeUnit.MILLISECONDS.toHours(ms);
        ms -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        ms -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms);
        ms -= TimeUnit.SECONDS.toMillis(seconds);
        if (hours > 0) {
            if (ms > 0) {
                return String.format("%d:%02d:%02d.%03d", hours, minutes, seconds, ms);
            }
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            if (ms > 0) {
                return String.format("%d:%02d.%03d", minutes, seconds, ms);
            }
            return String.format("%d:%02d", minutes, seconds);
        }
    }
}
