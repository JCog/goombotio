package util;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.api.IEventManager;
import com.github.philippheuer.events4j.core.EventManager;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.ITwitchChat;
import com.github.twitch4j.chat.TwitchChatBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageActionEvent;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.chat.events.channel.ModAnnouncementEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.github.twitch4j.eventsub.domain.RedemptionStatus;
import com.github.twitch4j.eventsub.events.*;
import com.github.twitch4j.eventsub.socket.IEventSubSocket;
import com.github.twitch4j.eventsub.subscriptions.SubscriptionTypes;
import com.github.twitch4j.helix.domain.*;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import listeners.TwitchEventListener;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static java.lang.System.out;

public class TwitchApi {
    private final String channelAuthToken;
    private final String botAuthToken;
    private final TwitchClient twitchClient;
    private final ITwitchChat chatClient;
    private final User streamerUser;
    private final User botUser;
    
    private boolean silentChat;
    
    public TwitchApi(
            String streamerUsername,
            String botUsername,
            String channelAuthToken,
            String channelClientId,
            String botAuthToken,
            boolean silentChat
    ) {
        out.printf("Establishing Twitch connection (channel=%s, chat=%s)... ", streamerUsername, botUsername);
        this.channelAuthToken = channelAuthToken;
        this.botAuthToken = botAuthToken;
        this.silentChat = silentChat;
        
        OAuth2Credential oauth = new OAuth2Credential("twitch", channelAuthToken);
        twitchClient = TwitchClientBuilder.builder()
                .withClientId(channelClientId)
                .withDefaultAuthToken(oauth)
                .withEnableHelix(true)
                .withEnableEventSocket(true)
                .build();
        
        // creating this separately is the only way to call withAutoJoinOwnChannel(false)
        chatClient = TwitchChatBuilder.builder()
                .withChatAccount(new OAuth2Credential("twitch", botAuthToken))
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
            out.println("Error retrieving streamer user");
            System.exit(1);
        }
        if (botUser == null) {
            out.println("Error retrieving bot user");
            System.exit(1);
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
        eventSocket.register(SubscriptionTypes.CHANNEL_CHEER.prepareSubscription(
                b -> b.broadcasterUserId(streamerUser.getId()).build(), null
        ));
        eventSocket.register(SubscriptionTypes.CHANNEL_POINTS_CUSTOM_REWARD_REDEMPTION_ADD.prepareSubscription(
                b -> b.broadcasterUserId(streamerUser.getId()).build(), null
        ));
        // readd for EventSub
//        eventSocket.register(SubscriptionTypes.CHANNEL_SUBSCRIBE.prepareSubscription(
//                b -> b.broadcasterUserId(streamerUser.getId()).build(), null
//        ));
//        eventSocket.register(SubscriptionTypes.CHANNEL_SUBSCRIPTION_MESSAGE.prepareSubscription(
//                b -> b.broadcasterUserId(streamerUser.getId()).build(), null
//        ));
        eventSocket.register(SubscriptionTypes.CHANNEL_SUBSCRIPTION_GIFT.prepareSubscription(
                b -> b.broadcasterUserId(streamerUser.getId()).build(), null
        ));
        out.println("success.");
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
        IEventManager eventSubEvents = twitchClient.getEventSocket().getEventManager();
        eventSubEvents.onEvent(ChannelAdBreakBeginEvent.class, eventListener::onAdBegin);
        eventSubEvents.onEvent(ChannelRaidEvent.class, eventListener::onRaid);
        eventSubEvents.onEvent(StreamOnlineEvent.class, eventListener::onGoLive);
        eventSubEvents.onEvent(StreamOfflineEvent.class, eventListener::onGoOffline);
        eventSubEvents.onEvent(ChannelUpdateV2Event.class, eventListener::onChannelUpdate);
        eventSubEvents.onEvent(CustomRewardRedemptionAddEvent.class, eventListener::onChannelPointsRedemption);
        eventSubEvents.onEvent(ChannelCheerEvent.class, eventListener::onCheer);
        // redo sub events with EventSub if Twitch ever decides to make them work in a sensible way
//        eventSubEvents.onEvent(ChannelSubscribeEvent.class, eventListener::onSubscribe);
//        eventSubEvents.onEvent(ChannelSubscriptionMessageEvent.class, eventListener::onResubscribe);
        eventSubEvents.onEvent(ChannelSubscriptionGiftEvent.class, eventListener::onSubGift);
        eventSubEvents.onEvent(HypeTrainBeginEvent.class, eventListener::onHypeTrainBegin);
        
        EventManager chatEvents = chatClient.getEventManager();
        chatEvents.onEvent(ModAnnouncementEvent.class, eventListener::onAnnouncement);
        chatEvents.onEvent(ChannelMessageActionEvent.class, eventListener::onChannelMessageAction);
        chatEvents.onEvent(ChannelMessageEvent.class, eventListener::onChannelMessage);
        // remove when switching back to EventSub
        chatEvents.onEvent(SubscriptionEvent.class, eventListener::onSubscribe);
    }
    
    public void toggleSlientChat() {
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
            System.out.printf("Illegal command usage \"%s\"%n", firstWord);
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
        channelAnnouncement(message, com.github.twitch4j.common.enums.AnnouncementColor.PRIMARY);
    }
    
    public void channelAnnouncement(String message, com.github.twitch4j.common.enums.AnnouncementColor color) {
        channelMessage(message);
        // TODO: actually use announcements once twitch decides to make them work on mobile
//        String output = message.trim();
//        if (output.isEmpty()) {
//            return;
//        }
//        if (silentChat) {
//            out.println("SILENT_CHAT: /announce " + message);
//        } else {
//            twitchClient.getHelix().sendChatAnnouncement(
//                    botAuthToken,
//                    streamerUser.getId(),
//                    botUser.getId(),
//                    output,
//                    com.github.twitch4j.common.enums.AnnouncementColor.PRIMARY
//            ).execute();
//        }
    }
    
    private void sendMessage(String message) {
        if (silentChat) {
            System.out.println("SILENT_CHAT: " + message);
        } else {
            chatClient.sendMessage(streamerUser.getDisplayName(), message);
            ChatLogger.logMessage(botUser, message);
        }
    }
    
    public void shoutout(String userId) {
        try {
            twitchClient.getHelix().sendShoutout(botAuthToken, streamerUser.getId(), userId, botUser.getId()).execute();
        } catch (HystrixRuntimeException e) {
            e.printStackTrace();
        }
    }
    
    //////////////////////////////////////////////////////////////////////////
    
    public List<AdSchedule> getAdSchedule() {
        try {
            return twitchClient.getHelix().getAdSchedule(channelAuthToken, streamerUser.getId()).execute().getData();
        } catch (HystrixRuntimeException e) {
            return new ArrayList<>();
        }
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
                    channelAuthToken,
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
                        channelAuthToken,
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
                    channelAuthToken,
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
                channelAuthToken,
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
                channelAuthToken,
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
                    channelAuthToken,
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
                channelAuthToken,
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
                    channelAuthToken,
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
                channelAuthToken,
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
                    channelAuthToken,
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
                channelAuthToken,
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
                channelAuthToken,
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
                    channelAuthToken,
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
                channelAuthToken,
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
                channelAuthToken,
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
                    channelAuthToken,
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
                channelAuthToken,
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
                channelAuthToken,
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
                channelAuthToken,
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
                channelAuthToken,
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
                    channelAuthToken,
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
                    channelAuthToken,
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
                channelAuthToken,
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
        twitchClient.getHelix().sendWhisper(botAuthToken, botUser.getId(), toId, message).execute();
    }
    
    public void vipAdd(String userId) throws HystrixRuntimeException {
        twitchClient.getHelix().addChannelVip(channelAuthToken, streamerUser.getId(), userId).execute();
    }
    
    public void vipRemove(String userId) throws HystrixRuntimeException {
        twitchClient.getHelix().removeChannelVip(channelAuthToken, streamerUser.getId(), userId).execute();
    }
    
    ///////////////////////////// Channel Points /////////////////////////////
    
    public CustomReward createCustomReward(String broadcasterId, CustomReward customReward) throws  HystrixRuntimeException {
        CustomRewardList customRewardList = twitchClient.getHelix().createCustomReward(channelAuthToken, broadcasterId, customReward).execute();
        return customRewardList.getRewards().get(0);
    }
    
    public void deleteCustomReward(String broadcasterId, String rewardId) throws HystrixRuntimeException {
        twitchClient.getHelix().deleteCustomReward(channelAuthToken, broadcasterId, rewardId).execute();
    }
    
    public List<CustomReward> getCustomRewards(String broadcasterId, Collection<String> rewardIds, Boolean onlyManageableRewards) throws HystrixRuntimeException {
        return twitchClient.getHelix().getCustomRewards(channelAuthToken, broadcasterId, rewardIds, onlyManageableRewards).execute().getRewards();
    }
    
    public CustomReward updateCustomReward(String broadcasterId, String rewardId, CustomReward updatedReward) throws  HystrixRuntimeException {
        CustomRewardList customRewardList = twitchClient.getHelix().updateCustomReward(channelAuthToken, broadcasterId, rewardId, updatedReward).execute();
        return customRewardList.getRewards().get(0);
    }
    
    public List<CustomRewardRedemption> getCustomRewardRedemptions(String broadcasterId, String rewardId, Collection<String> redemptionIds) throws HystrixRuntimeException {
        String cursor = null;
        List<CustomRewardRedemption> redemptionsOutput = new ArrayList<>();
    
        do {
            CustomRewardRedemptionList redemptionList = twitchClient.getHelix().getCustomRewardRedemption(
                    channelAuthToken,
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
    
    public void updateRedemptionStatus(String broadcasterId, String rewardId, Collection<String> redemptionIds, RedemptionStatus newStatus) throws HystrixRuntimeException {
        CustomRewardRedemptionList redemptionList = twitchClient.getHelix().updateRedemptionStatus(channelAuthToken, broadcasterId, rewardId, redemptionIds, newStatus).execute();
        for (CustomRewardRedemption redemption : redemptionList.getRedemptions()) {
            out.printf("Custom reward redemption \"%s\" has been %s for %s%n",
                    redemption.getReward().getTitle(),
                    newStatus == RedemptionStatus.FULFILLED ? "fulfilled" : "canceled",
                    redemption.getUserName()
            );
        }
    }
}
