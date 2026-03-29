package dev.jcog.goombotio.util;

import dev.jcog.goombotio.api.ApiManager;
import dev.jcog.goombotio.database.DbManager;
import dev.jcog.goombotio.functions.DiscordBotController;

import java.util.concurrent.ScheduledExecutorService;

public record CommonUtils(
        TwitchApi twitchApi,
        DbManager dbManager,
        DiscordBotController discordBotController,
        ApiManager apiManager,
        ScheduledExecutorService scheduler
) {}
