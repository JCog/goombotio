package util;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.events.ChannelChangeGameEvent;
import com.github.twitch4j.events.ChannelChangeTitleEvent;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.events.ChannelGoOfflineEvent;
import com.github.twitch4j.eventsub.domain.RedemptionStatus;
import com.github.twitch4j.helix.domain.*;
import com.github.twitch4j.pubsub.events.ChannelBitsEvent;
import com.github.twitch4j.pubsub.events.ChannelSubGiftEvent;
import com.github.twitch4j.pubsub.events.ChannelSubscribeEvent;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import listeners.TwitchEventListener;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static java.lang.System.out;

public class TwitchApi {
    private final String channelAuthToken;
    private final String botAuthToken;
    private final TwitchClient twitchClient;
    private final User streamerUser;
    private final User botUser;
    private final ChatLogger chatLogger;
    private final boolean silentChat;
    
    public TwitchApi(
            ChatLogger chatLogger,
            String streamerUsername,
            String botUsername,
            String channelAuthToken,
            String channelClientId,
            String botAuthToken,
            boolean silentChat
    ) {
        out.printf("Establishing Twitch connection (channel=%s, chat=%s)... ", streamerUsername, botUsername);
        this.chatLogger = chatLogger;
        this.channelAuthToken = channelAuthToken;
        this.botAuthToken = botAuthToken;
        this.silentChat = silentChat;
        
        OAuth2Credential oauth = new OAuth2Credential("twitch", channelAuthToken);
        twitchClient = TwitchClientBuilder.builder()
                .withClientId(channelClientId)
                .withDefaultAuthToken(oauth)
                .withChatAccount(new OAuth2Credential("twitch", botAuthToken))
                .withEnableTMI(true)
                .withEnableHelix(true)
                .withEnablePubSub(true)
                .withEnableChat(true)
                .build();
        twitchClient.getChat().joinChannel(streamerUsername);
        twitchClient.getClientHelper().enableStreamEventListener(streamerUsername);
        
        streamerUser = getUserByUsername(streamerUsername);
        botUser = getUserByUsername(botUsername);
        if (streamerUser == null) {
            out.println("Error retrieving streamer user");
            System.exit(1);
        }
        if (botUser == null) {
            out.println("Error retrieving bot user");
            System.exit(1);
        }
    
        // PubSub
        twitchClient.getPubSub().listenForCheerEvents(oauth, streamerUser.getId());
        twitchClient.getPubSub().listenForChannelPointsRedemptionEvents(oauth, streamerUser.getId());
        twitchClient.getPubSub().listenForSubscriptionEvents(oauth, streamerUser.getId());
        twitchClient.getPubSub().listenForChannelSubGiftsEvents(oauth, streamerUser.getId());
        out.println("success.");
    }
    
    public User getStreamerUser() {
        return streamerUser;
    }
    
    public User getBotUser() {
        return botUser;
    }
    
    public void registerEventListener(TwitchEventListener eventListener) {
        twitchClient.getEventManager().onEvent(ChannelMessageEvent.class, eventListener::onChannelMessage);
        twitchClient.getEventManager().onEvent(ChannelGoLiveEvent.class, eventListener::onGoLive);
        twitchClient.getEventManager().onEvent(ChannelGoOfflineEvent.class, eventListener::onGoOffline);
        twitchClient.getEventManager().onEvent(ChannelChangeGameEvent.class, eventListener::onGameChange);
        twitchClient.getEventManager().onEvent(ChannelChangeTitleEvent.class, eventListener::onChangeTitle);
        
        twitchClient.getEventManager().onEvent(ChannelBitsEvent.class, eventListener::onCheer);
        twitchClient.getEventManager().onEvent(RewardRedeemedEvent.class, eventListener::onChannelPointsRedemption);
        twitchClient.getEventManager().onEvent(ChannelSubscribeEvent.class, eventListener::onSub);
        twitchClient.getEventManager().onEvent(ChannelSubGiftEvent.class, eventListener::onSubGift);
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
        channelAnnouncement(message, AnnouncementColor.PRIMARY);
    }
    
    public void channelAnnouncement(String message, AnnouncementColor color) {
        String output = message.trim();
        if (output.isEmpty()) {
            return;
        }
        twitchClient.getHelix().sendChatAnnouncement(
                botAuthToken,
                streamerUser.getId(),
                botUser.getId(),
                output,
                AnnouncementColor.PRIMARY
        ).execute();
    }
    
    private void sendMessage(String message) {
        if (silentChat) {
            System.out.println("SILENT_CHAT: " + message);
        } else {
            twitchClient.getChat().sendMessage(streamerUser.getDisplayName(), message);
            chatLogger.logMessage(botUser, message);
        }
    }
    
    //////////////////////////////////////////////////////////////////////////
    
    public BannedUser getBannedUser(String userId) throws HystrixRuntimeException {
        List<BannedUser> bannedUsers = getBannedUsers(Collections.singletonList(userId));
        if (bannedUsers.isEmpty()) {
            return null;
        }
        return bannedUsers.get(0);
    }
    
    public List<BannedUser> getBannedUsers() throws HystrixRuntimeException {
        return getBannedUsers(null);
    }
    
    public List<BannedUser> getBannedUsers(List<String> userIds) throws HystrixRuntimeException {
        String cursor = null;
        List<BannedUser> bannedUserOutput = new ArrayList<>();
        do {
            BannedUserList bannedUserList = twitchClient.getHelix().getBannedUsers(
                    channelAuthToken,
                    streamerUser.getId(),
                    userIds,
                    cursor,
                    null,
                    100
            ).execute();
            cursor = bannedUserList.getPagination().getCursor();
            bannedUserOutput.addAll(bannedUserList.getResults());
        } while (cursor != null);
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
                id,
                null,
                null,
                1,
                null,
                null
        ).execute();
        if (clipList.getData().isEmpty()) {
            return null;
        }
        return clipList.getData().get(0);
    }

    @Nullable
    public Follow getFollow(String fromId, String toId) throws HystrixRuntimeException {
        FollowList followList = twitchClient.getHelix().getFollowers(
                channelAuthToken,
                fromId,
                toId,
                null,
                1
        ).execute();
        if (followList.getFollows().isEmpty()) {
            return null;
        }
        return followList.getFollows().get(0);
    }

    public int getFollowerCount(String userId) throws HystrixRuntimeException {
        FollowList followList = twitchClient.getHelix().getFollowers(
                channelAuthToken,
                null,
                userId,
                null,
                null
        ).execute();
        return followList.getTotal();
    }
    
    public InboundFollow getChannelFollower(String channelId, String followerId) throws HystrixRuntimeException {
        String cursor = null;
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
            followsOutput.addAll(followList.getFollows());
        } while (cursor != null);
        return followsOutput;
    }

    public List<OutboundFollow> getFollowedChannels(String userId) throws HystrixRuntimeException {
        String cursor = null;
        List<OutboundFollow> followsOutput = new ArrayList<>();

        do {
            OutboundFollowing followList = twitchClient.getHelix().getFollowedChannels(
                    channelAuthToken,
                    userId,
                    null,
                    100,
                    cursor
            ).execute();
            cursor = followList.getPagination().getCursor();
            followsOutput.addAll(followList.getFollows());
        } while (cursor != null);
        return followsOutput;
    }

    @Nullable
    public Game getGameById(String gameId) throws HystrixRuntimeException {
        GameList gameList = twitchClient.getHelix().getGames(
                channelAuthToken,
                Collections.singletonList(gameId),
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
                Collections.singletonList(name)
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
    public Stream getStream(String streamer) throws HystrixRuntimeException {
        StreamList streamList = twitchClient.getHelix().getStreams(
                channelAuthToken,
                "",
                "",
                1,
                null,
                null,
                null,
                Collections.singletonList(streamer)).execute();
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
                videoId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                1
        ).execute();
        if (videoList.getVideos().isEmpty()) {
            return null;
        }
        return videoList.getVideos().get(0);
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
