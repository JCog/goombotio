package util;

import api.ApiManager;
import database.DbManager;
import functions.DiscordBotController;

import java.util.concurrent.ScheduledExecutorService;

public class CommonUtils {
    private final TwitchApi twitchApi;
    private final DbManager dbManager;
    private final DiscordBotController discordBotController;
    private final ApiManager apiManager;
    private final ScheduledExecutorService scheduler;
    
    public CommonUtils(
            TwitchApi twitchApi,
            DbManager dbManager,
            DiscordBotController discordBotController,
            ApiManager apiManager,
            ScheduledExecutorService scheduler
    ) {
        this.twitchApi = twitchApi;
        this.dbManager = dbManager;
        this.discordBotController = discordBotController;
        this.apiManager = apiManager;
        this.scheduler = scheduler;
    }
    
    public TwitchApi getTwitchApi() {
        return twitchApi;
    }
    
    public DbManager getDbManager() {
        return dbManager;
    }
    
    public DiscordBotController getDiscordBotController() {
        return discordBotController;
    }
    
    public ApiManager getApiManager() {
        return apiManager;
    }
    
    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }
}
