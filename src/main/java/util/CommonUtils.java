package util;

import api.ApiManager;
import database.DbManager;
import functions.DiscordBotController;

import java.util.concurrent.ScheduledExecutorService;

public record CommonUtils(
        TwitchApi twitchApi,
        DbManager dbManager,
        DiscordBotController discordBotController,
        ApiManager apiManager,
        ScheduledExecutorService scheduler
) {}
