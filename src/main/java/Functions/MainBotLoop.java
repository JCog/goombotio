package Functions;

import Util.Database.SpeedySpinLeaderboard;
import com.gikk.twirk.Twirk;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class MainBotLoop {
    private static MainBotLoop instance = null;
    private static Twirk twirk;
    
    private MainBotLoop(Twirk twirk){
        this.twirk = twirk;
    }
    
    public static MainBotLoop getInstance(Twirk twirk) {
        if (instance == null) {
            instance = new MainBotLoop(twirk);
        }
        return instance;
    }
    
    public void run() {
        Scanner scanner = new Scanner(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String line;
        while( !(line = scanner.nextLine()).matches(".quit") ) {
            if(line.equals(".lb")) { //TODO: this is pretty hacky, should improve
                SpeedySpinLeaderboard lb = new SpeedySpinLeaderboard();
                lb.logPreviousTopMonthlyScorers();
            }
            else {
                twirk.channelMessage(line);
            }
        }
        scanner.close();
    }
}
