package api.src;

import api.src.category.Category;
import api.src.category.CategoryInterface;
import api.src.category.Run;
import api.src.user.User;
import api.src.user.UserInterface;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class SrcApi {
    private static final String BASE_URI = "https://www.speedrun.com/api/v1/";
    
    private final CategoryInterface categoryProxy;
    private final UserInterface userProxy;
    
    
    public SrcApi() {
        ResteasyClient client = (ResteasyClient) ClientBuilder.newClient();
        ResteasyWebTarget target = client.target(BASE_URI);
        categoryProxy = target.proxy(CategoryInterface.class);
        userProxy = target.proxy(UserInterface.class);
    }
    
    public String getWr(SrcEnums.Category category) {
        Category srcCategory;
        try {
            srcCategory = categoryProxy.getWr(
                    category.getGame().getId(),
                    category.getId(),
                    category.getVariables().length == 0 ? 1 : null
            );
        } catch (ClientErrorException e) {
            System.out.println("Error getting SRC category:\n" + e.getMessage());
            return "";
        }
        List<Run> runs = srcCategory.getCategoryDetails().getRuns();
        if (runs.isEmpty()) {
            return String.format("%s %s has no WR.", category.getGame(), category);
        }
        Run wrRun = null;
        if (category.getVariables().length == 0) {
            wrRun = runs.get(0);
        } else {
            for (Run run : runs) {
                boolean matches = true;
                for (SrcEnums.Variable var : category.getVariables()) {
                    String value = run.getRunDetails().getValues().get(var.getVarId());
                    if (!Objects.equals(value, var.getValueId())) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    wrRun = run;
                    break;
                }
            }
            if (wrRun == null) {
                return String.format("%s %s has no WR.", category.getGame(), category);
            }
        }
        
        String userId = wrRun.getRunDetails().getPlayers().get(0).getId();
        BigDecimal rawTime = wrRun.getRunDetails().getTimes().getPrimaryTime();
        String timeString = getTimeString(rawTime);
        
        User user;
        try {
            user = userProxy.getUserById(userId);
        } catch (ClientErrorException e) {
            System.out.println("Error getting SRC user:\n" + e.getMessage());
            return "";
        }
        String username = user.getUserDetails().getNames().getInternational();
        return String.format("The %s %s WR is %s by %s", category.getGame(), category, timeString, username);
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
