package dev.jcog.goombotio.util;

import com.github.philippheuer.credentialmanager.CredentialManager;
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.api.IEventManager;
import com.github.philippheuer.events4j.core.EventManager;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import com.github.twitch4j.chat.ITwitchChat;
import com.github.twitch4j.chat.TwitchChatBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageActionEvent;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.chat.events.channel.ModAnnouncementEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.github.twitch4j.common.enums.AnnouncementColor;
import com.github.twitch4j.eventsub.domain.RedemptionStatus;
import com.github.twitch4j.eventsub.events.*;
import com.github.twitch4j.eventsub.socket.IEventSubSocket;
import com.github.twitch4j.eventsub.subscriptions.SubscriptionTypes;
import com.github.twitch4j.helix.domain.*;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import dev.jcog.goombotio.database.misc.AuthDb;
import dev.jcog.goombotio.database.misc.AuthDb.AuthItem;
import dev.jcog.goombotio.listeners.TwitchEventListener;
import dev.jcog.goombotio.listeners.TwitchEventListener.EVENT_TYPE;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TwitchApi {
    private static final Logger log = LoggerFactory.getLogger(TwitchApi.class);

    private final AuthDb authDb;
    private final AuthItem streamerAuth;
    private final AuthItem botAuth;
    private final ScheduledExecutorService scheduler;
    private final TwitchClient twitchClient;
    private final ITwitchChat chatClient;
    private final User streamerUser;
    private final User botUser;

    private boolean silentChat;

    public TwitchApi(
            String streamerUsername,
            String botUsername,
            String clientId,
            String clientSecret,
            boolean silentChat,
            AuthDb authDb,
            ScheduledExecutorService scheduler
    ) {
        log.info("Establishing Twitch connection (channel={}, bot={})...", streamerUsername, botUsername);
        this.authDb = authDb;
        this.scheduler = scheduler;
        this.silentChat = silentChat;
        streamerAuth = authDb.getAuth(streamerUsername);
        if (streamerAuth == null) {
            throw new StartupException("missing streamer credentials");
        }
        botAuth = authDb.getAuth(botUsername);
        if (botAuth == null) {
            throw new StartupException("missing bot credentials");
        }

        TwitchIdentityProvider tip = new TwitchIdentityProvider(clientId, clientSecret, null);
        CredentialManager credentialManager = CredentialManagerBuilder.builder().build();
        credentialManager.registerIdentityProvider(tip);

        OAuth2Credential streamerOauth = setupCredentials(streamerAuth, tip);
        twitchClient = TwitchClientBuilder.builder()
                .withClientId(clientId)
                .withClientSecret(clientSecret)
                .withCredentialManager(credentialManager)
                .withDefaultAuthToken(streamerOauth)
                .withEnableHelix(true)
                .withEnableEventSocket(true)
                .build();
        
        // creating this separately is the only way to call withAutoJoinOwnChannel(false)
        OAuth2Credential botOauth = setupCredentials(botAuth, tip);
        chatClient = TwitchChatBuilder.builder()
                .withClientId(clientId)
                .withClientSecret(clientSecret)
                .withCredentialManager(credentialManager)
                .withChatAccount(botOauth)
                .withAutoJoinOwnChannel(false)
                .build();
        chatClient.joinChannel(streamerUsername);
        
        try {
            streamerUser = getUserByUsername(streamerUsername);
        } catch (HystrixRuntimeException e) {
            throw new StartupException("unable to authenticate with Twitch Helix API");
        }
        botUser = getUserByUsername(botUsername);
        if (streamerUser == null) {
            throw new StartupException("unable to retrieve streamer user");
        }
        if (botUser == null) {
            throw new StartupException("unable to retrieve bot user");
        }
        
        // EventSub registrations
        IEventSubSocket eventSocket = twitchClient.getEventSocket();
        // I swear there has to be a better way to do this lmao
        eventSocket.register(SubscriptionTypes.CHANNEL_AD_BREAK_BEGIN.prepareSubscription(
                b -> b.broadcasterUserId(streamerUser.getId()).build(), null
        ));
        eventSocket.register(SubscriptionTypes.CHANNEL_RAID.prepareSubscription(
                b -> b.toBroadcasterUserId(streamerUser.getId()).build(), null
        ));
        eventSocket.register(SubscriptionTypes.STREAM_ONLINE.prepareSubscription(
                b -> b.broadcasterUserId(streamerUser.getId()).build(), null
        ));
        eventSocket.register(SubscriptionTypes.STREAM_OFFLINE.prepareSubscription(
                b -> b.broadcasterUserId(streamerUser.getId()).build(), null
        ));
        eventSocket.register(SubscriptionTypes.CHANNEL_UPDATE_V2.prepareSubscription(
                b -> b.broadcasterUserId(streamerUser.getId()).build(), null
        ));
        eventSocket.register(SubscriptionTypes.CHANNEL_POINTS_CUSTOM_REWARD_REDEMPTION_ADD.prepareSubscription(
                b -> b.broadcasterUserId(streamerUser.getId()).build(), null
        ));
        eventSocket.register(SubscriptionTypes.CHANNEL_CHEER.prepareSubscription(
                b -> b.broadcasterUserId(streamerUser.getId()).build(), null
        ));
        eventSocket.register(SubscriptionTypes.CHANNEL_SUBSCRIPTION_GIFT.prepareSubscription(
                b -> b.broadcasterUserId(streamerUser.getId()).build(), null
        ));
        eventSocket.register(SubscriptionTypes.HYPE_TRAIN_BEGIN_V2.prepareSubscription(
                b -> b.broadcasterUserId(streamerUser.getId()).build(), null
        ));
        eventSocket.register(SubscriptionTypes.CHANNEL_SUSPICIOUS_USER_MESSAGE.prepareSubscription(
                b -> b.broadcasterUserId(streamerUser.getId()).moderatorUserId(streamerUser.getId()).build(), null
        ));
        // readd for EventSub
//        eventSocket.register(SubscriptionTypes.CHANNEL_SUBSCRIBE.prepareSubscription(
//                b -> b.broadcasterUserId(streamerUser.getId()).build(), null
//        ));
//        eventSocket.register(SubscriptionTypes.CHANNEL_SUBSCRIPTION_MESSAGE.prepareSubscription(
//                b -> b.broadcasterUserId(streamerUser.getId()).build(), null
//        ));
        log.info("Twitch connection established");
    }

    private OAuth2Credential setupCredentials(AuthItem authItem, TwitchIdentityProvider tip) {
        OAuth2Credential credential = new OAuth2Credential("twitch", authItem.authToken);
        credential.setRefreshToken(authItem.refreshToken);

        if (Settings.DEV_ENV || scheduler == null) {
            return credential;
        }

        scheduler.scheduleAtFixedRate(() -> {
            try {
                tip.getAdditionalCredentialInformation(credential).ifPresent(validCred -> {
                    log.info("\"{}\" token validated", authItem.id);
                    if (validCred.getExpiresIn() > Duration.ofHours(2).getSeconds()) {
                        return;
                    }
                    tip.refreshCredential(credential).ifPresent(newCred -> {
                        credential.updateCredential(newCred);
                        authItem.authToken = credential.getAccessToken();
                        authItem.refreshToken = credential.getRefreshToken();
                        authDb.setAuthToken(authItem);
                        log.info("\"{}\" token refreshed", authItem.id);
                    });
                });
            } catch (RuntimeException e) {
                log.error(e.getMessage());
            }
        }, 0, 1, TimeUnit.HOURS);

        return credential;
    }
    
    public void close() {
        chatClient.close();
        twitchClient.close();
    }
    
    public User getStreamerUser() {
        return streamerUser;
    }
    
    public User getBotUser() {
        return botUser;
    }

    public void registerEventListener(TwitchEventListener eventListener) {
        IEventManager iem = twitchClient.getEventSocket().getEventManager();
        EventManager chatEM = chatClient.getEventManager();
        for (EVENT_TYPE eventType : eventListener.getEventTypes()) {
            switch (eventType) {
                case AD_BEGIN -> iem.onEvent(ChannelAdBreakBeginEvent.class, eventListener::onAdBegin);
                case RAID -> iem.onEvent(ChannelRaidEvent.class, eventListener::onRaid);
                case GO_LIVE -> iem.onEvent(StreamOnlineEvent.class, eventListener::onGoLive);
                case GO_OFFLINE -> iem.onEvent(StreamOfflineEvent.class, eventListener::onGoOffline);
                case CHANNEL_UPDATE -> iem.onEvent(ChannelUpdateV2Event.class, eventListener::onChannelUpdate);
                case CHANNEL_POINTS_REDEMPTION ->
                        iem.onEvent(CustomRewardRedemptionAddEvent.class, eventListener::onChannelPointsRedemption);
                case CHEER -> iem.onEvent(ChannelCheerEvent.class, eventListener::onCheer);
                // redo sub events with EventSub if Twitch ever decides to make them work in a sensible way
//                case SUBSCRIBE -> iem.onEvent(ChannelSubscribeEvent.class, eventListener::onSubscribe);
//                case RESUBSCRIBE -> iem.onEvent(ChannelSubscriptionMessageEvent.class, eventListener::onResubscribe);
                case SUB_GIFT -> iem.onEvent(ChannelSubscriptionGiftEvent.class, eventListener::onSubGift);
                case HYPE_TRAIN_BEGIN -> iem.onEvent(HypeTrainBeginV2Event.class, eventListener::onHypeTrainBegin);
                case SUSPICIOUS_USER_MESSAGE ->
                        iem.onEvent(SuspiciousUserMessageEvent.class, eventListener::onSuspiciousUserMessage);

                case ANNOUNCEMENT -> chatEM.onEvent(ModAnnouncementEvent.class, eventListener::onAnnouncement);
                case CHANNEL_MESSAGE_ACTION ->
                        chatEM.onEvent(ChannelMessageActionEvent.class, eventListener::onChannelMessageAction);
                case CHANNEL_MESSAGE -> chatEM.onEvent(ChannelMessageEvent.class, eventListener::onChannelMessage);
                case SUBSCRIBE -> chatEM.onEvent(SubscriptionEvent.class, eventListener::onSubscribe);
            }
        }
    }
    
    public void toggleSilentChat() {
        if (silentChat) {
            silentChat = false;
            channelMessage("Goombotio has been unmuted.");
        } else {
            channelMessage("Goombotio has been muted");
            silentChat = true;
        }
    }
    
    //////////////////////////////////////////////////////////////////////////
    
    /**
     * Sends a message to twitch chat if the first non-whitespace character is not '/' or '.' to prevent commands
     *
     * @param line message to send
     */
    public void channelMessage(String line) {
        String output = line.trim();
        if (output.isEmpty()) {
            return;
        }
        String firstWord = output.split("\\s", 2)[0];
        if (firstWord.charAt(0) == '/' || firstWord.charAt(0) == '.') {
            log.error("Illegal command usage \"{}\"", firstWord);
        } else {
            sendMessage(output);
        }
    }
    
    /**
     * Sends a message to twitch chat with no restrictions on commands
     *
     * @param message message to send
     */
    public void channelAnnouncement(String message) {
        channelAnnouncement(message, AnnouncementColor.PRIMARY);
    }
    
    public void channelAnnouncement(String message, AnnouncementColor color) {
        String output = message.trim();
        if (output.isEmpty()) {
            return;
        }

        if (silentChat) {
            log.info("SILENT_CHAT: /announce {}", message);
        } else {
            twitchClient.getHelix().sendChatAnnouncement(
                    botAuth.authToken,
                    streamerUser.getId(),
                    botUser.getId(),
                    output,
                    color
            ).execute();
        }
    }
    
    private void sendMessage(String message) {
        if (silentChat) {
            log.info("SILENT_CHAT: {}", message);
        } else {
            chatClient.sendMessage(streamerUser.getDisplayName(), message);
            ChatLogger.logMessage(botUser, message);
        }
    }
    
    public void shoutout(String userId) {
        try {
            twitchClient.getHelix().sendShoutout(botAuth.authToken, streamerUser.getId(), userId, botUser.getId()).execute();
        } catch (HystrixRuntimeException e) {
            log.error("Error attempting to shoutout user with id {}: {}", userId, e.getMessage());
        }
    }
    
    //////////////////////////////////////////////////////////////////////////
    
    public AdSchedule getAdSchedule() throws HystrixRuntimeException {
        List<AdSchedule> adSchedules = twitchClient.getHelix()
                .getAdSchedule(streamerAuth.authToken, streamerUser.getId()).execute().getData();
        if (adSchedules.isEmpty()) {
            return null;
        }
        return adSchedules.get(0);

    }

    public boolean snoozeNextAd() {
        try {
            twitchClient.getHelix().snoozeNextAd(streamerAuth.authToken, streamerUser.getId()).execute();
        } catch (HystrixRuntimeException e) {
            log.error("Error attempting to snooze next ad: {}", e.getMessage());
            return false;
        }
        return true;
    }
    
    public BannedUser getBannedUser(String userId) throws HystrixRuntimeException {
        List<BannedUser> bannedUsers = getBannedUsers(Collections.singletonList(userId));
        if (bannedUsers.isEmpty()) {
            return null;
        }
        return bannedUsers.get(0);
    }
    
    public List<BannedUser> getBannedUsers() throws HystrixRuntimeException {
        String cursor = null;
        List<BannedUser> bannedUsers = new ArrayList<>();
        do {
            BannedUserList followList = twitchClient.getHelix().getBannedUsers(
                    streamerAuth.authToken,
                    streamerUser.getId(),
                    null,
                    cursor,
                    null,
                    100
            ).execute();
            cursor = followList.getPagination().getCursor();
            bannedUsers.addAll(followList.getResults());
        } while (cursor != null);
        return bannedUsers;
    }
    
    public List<BannedUser> getBannedUsers(List<String> userIds) throws HystrixRuntimeException {
        // for some reason this endpoint only accepts a max of 100 users, so can't just use a cursor and call it good
        List<String> idSubset = new ArrayList<>();
        List<BannedUser> bannedUserOutput = new ArrayList<>();
        Iterator<String> it = userIds.iterator();
        while (it.hasNext()) {
            idSubset.add(it.next());
            if (idSubset.size() == 100 || !it.hasNext()) {
                BannedUserList bannedUserList = twitchClient.getHelix().getBannedUsers(
                        streamerAuth.authToken,
                        streamerUser.getId(),
                        idSubset,
                        null,
                        null,
                        100
                ).execute();
                bannedUserOutput.addAll(bannedUserList.getResults());
                idSubset.clear();
            }
        }
        return bannedUserOutput;
    }
    
    public List<Chatter> getChatters() throws HystrixRuntimeException {
        String cursor = null;
        List<Chatter> chattersOutput = new ArrayList<>();
        
        do {
            ChattersList followList = twitchClient.getHelix().getChatters(
                    streamerAuth.authToken,
                    streamerUser.getId(),
                    streamerUser.getId(),
                    1000,
                    cursor
            ).execute();
            cursor = followList.getPagination().getCursor();
            chattersOutput.addAll(followList.getChatters());
        } while (cursor != null);
        return chattersOutput;
    }
    
    @Nullable
    public Clip getClipById(String id) throws HystrixRuntimeException {
        ClipList clipList = twitchClient.getHelix().getClips(
                streamerAuth.authToken,
                null,
                null,
                Collections.singletonList(id),
                null,
                null,
                1,
                null,
                null,
                null
        ).execute();
        if (clipList.getData().isEmpty()) {
            return null;
        }
        return clipList.getData().get(0);
    }
    
    // returns user that follows channel if it exists (must be a mod of the channel)
    public InboundFollow getChannelFollower(String channelId, String followerId) throws HystrixRuntimeException {
        InboundFollowers followList = twitchClient.getHelix().getChannelFollowers(
                streamerAuth.authToken,
                channelId,
                followerId,
                1,
                null
        ).execute();
        if (followList.getFollows() == null || followList.getFollows().isEmpty()) {
            return null;
        }
        return followList.getFollows().get(0);
    }
    
    // returns all users that follow a channel (must be a mod of the channel)
    public List<InboundFollow> getChannelFollowers(String channelId) throws HystrixRuntimeException {
        String cursor = null;
        List<InboundFollow> followsOutput = new ArrayList<>();
        
        do {
            InboundFollowers followList = twitchClient.getHelix().getChannelFollowers(
                    streamerAuth.authToken,
                    channelId,
                    null,
                    100,
                    cursor
            ).execute();
            cursor = followList.getPagination().getCursor();
            if (followList.getFollows() != null) {
                followsOutput.addAll(followList.getFollows());
            }
        } while (cursor != null);
        return followsOutput;
    }
    
    public int getChannelFollowersCount(String channelId) throws HystrixRuntimeException {
        InboundFollowers inboundFollowers = twitchClient.getHelix().getChannelFollowers(
                streamerAuth.authToken,
                channelId,
                null,
                null,
                null
        ).execute();
        return inboundFollowers.getTotal();
    }
    
    public List<ChannelVip> getChannelVips() throws HystrixRuntimeException {
        return getChannelVips(null);
    }
    
    public List<ChannelVip> getChannelVips(List<String> userIds) throws HystrixRuntimeException {
        String cursor = null;
        List<ChannelVip> vipListOutput = new ArrayList<>();
        do {
            ChannelVipList vipList = twitchClient.getHelix().getChannelVips(
                    streamerAuth.authToken,
                    streamerUser.getId(),
                    userIds,
                    100,
                    cursor
            ).execute();
            cursor = vipList.getPagination().getCursor();
            vipListOutput.addAll(vipList.getData());
        } while (cursor != null);
        return vipListOutput;
    }

    // returns followedId's follow data if followerId follows them (must be mod for follower)
    public OutboundFollow getFollowedChannel(String followerId, String followedId) throws HystrixRuntimeException {
        OutboundFollowing followList = twitchClient.getHelix().getFollowedChannels(
                streamerAuth.authToken,
                followerId,
                followedId,
                1,
                null
        ).execute();
        if (followList.getFollows() == null || followList.getFollows().isEmpty()) {
            return null;
        }
        return followList.getFollows().get(0);
    }
    
    // returns all the users that followerId follows (must be mod for follower)
    public List<OutboundFollow> getFollowedChannels(String followerId) throws HystrixRuntimeException {
        String cursor = null;
        List<OutboundFollow> followsOutput = new ArrayList<>();
        
        do {
            OutboundFollowing followList = twitchClient.getHelix().getFollowedChannels(
                    streamerAuth.authToken,
                    followerId,
                    null,
                    100,
                    cursor
            ).execute();
            cursor = followList.getPagination().getCursor();
            if (followList.getFollows() != null) {
                followsOutput.addAll(followList.getFollows());
            }
        } while (cursor != null);
        return followsOutput;
    }

    @Nullable
    public Game getGameById(String gameId) throws HystrixRuntimeException {
        GameList gameList = twitchClient.getHelix().getGames(
                streamerAuth.authToken,
                Collections.singletonList(gameId),
                null,
                null
        ).execute();
        if (gameList.getGames().isEmpty()) {
            return null;
        }
        return gameList.getGames().get(0);
    }
    
    @Nullable
    public Game getGameByName(String name) throws HystrixRuntimeException {
        GameList gameList = twitchClient.getHelix().getGames(
                streamerAuth.authToken,
                null,
                Collections.singletonList(name),
                null
        ).execute();
        if (gameList.getGames().isEmpty()) {
            return null;
        }
        return gameList.getGames().get(0);
    }

    public List<Moderator> getMods(String userId) throws HystrixRuntimeException {
        String cursor = null;
        List<Moderator> modsOutput = new ArrayList<>();

        do {
            ModeratorList moderatorList = twitchClient.getHelix().getModerators(
                    streamerAuth.authToken,
                    userId,
                    null,
                    cursor,
                    100
            ).execute();
            cursor = moderatorList.getPagination().getCursor();
            modsOutput.addAll(moderatorList.getModerators());
        } while (cursor != null);
        return modsOutput;
    }
    
    @Nullable
    public Stream getStreamByUserId(String userId) throws HystrixRuntimeException {
        StreamList streamList = twitchClient.getHelix().getStreams(
                streamerAuth.authToken,
                "",
                "",
                1,
                null,
                null,
                Collections.singletonList(userId),
                null
        ).execute();
        if (streamList.getStreams().isEmpty()) {
            return null;
        }
        return streamList.getStreams().get(0);
    }
    
    @Nullable
    public Stream getStreamByUsername(String username) throws HystrixRuntimeException {
        StreamList streamList = twitchClient.getHelix().getStreams(
                streamerAuth.authToken,
                "",
                "",
                1,
                null,
                null,
                null,
                Collections.singletonList(username)
        ).execute();
        if (streamList.getStreams().isEmpty()) {
            return null;
        }
        return streamList.getStreams().get(0);
    }

    public List<Subscription> getSubList(String userId) throws HystrixRuntimeException {
        String cursor = null;
        List<Subscription> subscriptionsOutput = new ArrayList<>();

        do {
            SubscriptionList subscriptionList = twitchClient.getHelix().getSubscriptions(
                    streamerAuth.authToken,
                    userId,
                    cursor,
                    null,
                    100
            ).execute();
            cursor = subscriptionList.getPagination().getCursor();
            subscriptionsOutput.addAll(subscriptionList.getSubscriptions());
        } while (cursor != null);
        return subscriptionsOutput;
    }
    
    public Subscription getSubByUser(String channelId, String userId) throws HystrixRuntimeException {
        SubscriptionList subList = twitchClient.getHelix().getSubscriptionsByUser(
                streamerAuth.authToken,
                channelId,
                Collections.singletonList(userId)
        ).execute();
        if (subList.getSubscriptions().isEmpty()) {
            return null;
        }
        return subList.getSubscriptions().get(0);
    }
    
    public int getSubPoints(String userId) throws HystrixRuntimeException {
        SubscriptionList subscriptionList = twitchClient.getHelix().getSubscriptions(
                streamerAuth.authToken,
                userId,
                null,
                null,
                1
        ).execute();
        return subscriptionList.getPoints();
    }

    @Nullable
    public User getUserById(String userId) throws HystrixRuntimeException {
        UserList userList = twitchClient.getHelix().getUsers(
                streamerAuth.authToken,
                Collections.singletonList(userId),
                null
        ).execute();
        if (userList.getUsers().isEmpty()) {
            return null;
        }
        return userList.getUsers().get(0);
    }

    @Nullable
    public User getUserByUsername(String username) throws HystrixRuntimeException {
        UserList userList = twitchClient.getHelix().getUsers(
                streamerAuth.authToken,
                null,
                Collections.singletonList(username)
        ).execute();
        if (userList.getUsers().isEmpty()) {
            return null;
        }
        return userList.getUsers().get(0);
    }

    public List<User> getUserListByIds(Collection<String> userIdList) throws HystrixRuntimeException {
        Iterator<String> iterator = userIdList.iterator();
        List<String> usersHundred = new ArrayList<>();
        List<User> output = new ArrayList<>();
        while (iterator.hasNext()) {
            while (usersHundred.size() < 100 && iterator.hasNext()) {
                usersHundred.add(iterator.next());
            }
            UserList resultList = twitchClient.getHelix().getUsers(
                    streamerAuth.authToken,
                    usersHundred,
                    null
            ).execute();
            output.addAll(resultList.getUsers());
            usersHundred.clear();
        }
        return output;
    }

    public List<User> getUserListByUsernames(Collection<String> usernameList) throws HystrixRuntimeException {
        Iterator<String> iterator = usernameList.iterator();
        List<String> usersHundred = new ArrayList<>();
        List<User> output = new ArrayList<>();
        while (iterator.hasNext()) {
            while (usersHundred.size() < 100 && iterator.hasNext()) {
                usersHundred.add(iterator.next());
            }
            UserList resultList = twitchClient.getHelix().getUsers(
                    streamerAuth.authToken,
                    null,
                    usersHundred
            ).execute();
            output.addAll(resultList.getUsers());
            usersHundred.clear();
        }
        return output;
    }

    @Nullable
    public Video getVideoById(String videoId) throws HystrixRuntimeException {
        VideoList videoList = twitchClient.getHelix().getVideos(
                streamerAuth.authToken,
                Collections.singletonList(videoId),
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                null,
                null
        ).execute();
        if (videoList.getVideos().isEmpty()) {
            return null;
        }
        return videoList.getVideos().get(0);
    }
    
    public void sendWhisper(String toId, String message) throws HystrixRuntimeException {
        twitchClient.getHelix().sendWhisper(botAuth.authToken, botUser.getId(), toId, message).execute();
    }
    
    public void vipAdd(String userId) throws HystrixRuntimeException {
        twitchClient.getHelix().addChannelVip(streamerAuth.authToken, streamerUser.getId(), userId).execute();
    }
    
    public void vipRemove(String userId) throws HystrixRuntimeException {
        twitchClient.getHelix().removeChannelVip(streamerAuth.authToken, streamerUser.getId(), userId).execute();
    }
    
    ///////////////////////////// Channel Points /////////////////////////////
    
    public CustomReward createCustomReward(
            String broadcasterId,
            CustomReward customReward
    ) throws HystrixRuntimeException {
        CustomRewardList customRewardList = twitchClient.getHelix()
                .createCustomReward(streamerAuth.authToken, broadcasterId, customReward).execute();
        return customRewardList.getRewards().get(0);
    }
    
    public void deleteCustomReward(String broadcasterId, String rewardId) throws HystrixRuntimeException {
        twitchClient.getHelix().deleteCustomReward(streamerAuth.authToken, broadcasterId, rewardId).execute();
    }

    public List<CustomReward> getCustomRewards(
            String broadcasterId,
            Collection<String> rewardIds,
            Boolean onlyManageableRewards
    ) throws HystrixRuntimeException {
        return twitchClient.getHelix()
                .getCustomRewards(streamerAuth.authToken, broadcasterId, rewardIds, onlyManageableRewards).execute()
                .getRewards();
    }

    public CustomReward updateCustomReward(
            String broadcasterId,
            String rewardId,
            CustomReward updatedReward
    ) throws  HystrixRuntimeException {
        CustomRewardList customRewardList = twitchClient.getHelix()
                .updateCustomReward(streamerAuth.authToken, broadcasterId, rewardId, updatedReward).execute();
        return customRewardList.getRewards().get(0);
    }

    public List<CustomRewardRedemption> getCustomRewardRedemptions(
            String broadcasterId,
            String rewardId,
            Collection<String> redemptionIds
    ) throws HystrixRuntimeException {
        String cursor = null;
        List<CustomRewardRedemption> redemptionsOutput = new ArrayList<>();
    
        do {
            CustomRewardRedemptionList redemptionList = twitchClient.getHelix().getCustomRewardRedemption(
                    streamerAuth.authToken,
                    broadcasterId,
                    rewardId,
                    redemptionIds,
                    null,
                    null,
                    cursor,
                    50
            ).execute();
            cursor = redemptionList.getPagination().getCursor();
            redemptionsOutput.addAll(redemptionList.getRedemptions());
        } while (cursor != null);
        return redemptionsOutput;
    }
    
    public void updateRedemptionStatus(
            String broadcasterId,
            String rewardId,
            Collection<String> redemptionIds,
            RedemptionStatus newStatus
    ) throws HystrixRuntimeException {
        CustomRewardRedemptionList redemptionList = twitchClient.getHelix().updateRedemptionStatus(
                streamerAuth.authToken,
                broadcasterId,
                rewardId,
                redemptionIds,
                newStatus
        ).execute();
        for (CustomRewardRedemption redemption : redemptionList.getRedemptions()) {
            log.info("Custom reward redemption \"{}\" has been {} for {}",
                    redemption.getReward().getTitle(),
                    newStatus == RedemptionStatus.FULFILLED ? "fulfilled" : "canceled",
                    redemption.getUserName()
            );
        }
    }
}
