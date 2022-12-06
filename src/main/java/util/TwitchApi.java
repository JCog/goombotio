package util;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.eventsub.domain.RedemptionStatus;
import com.github.twitch4j.helix.domain.*;
import com.github.twitch4j.pubsub.TwitchPubSub;
import com.github.twitch4j.tmi.domain.Chatters;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import listeners.TwitchEventListener;
import listeners.commands.CommandBase;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TwitchApi {
    private final String authToken;
    private final TwitchClient twitchClient;
    private final User streamerUser;
    private final User botUser;
    private final ChatLogger chatLogger;
    private final boolean silentChat;
    private final Set<String> reservedCommands = new HashSet<>();
    
    public TwitchApi(ChatLogger chatLogger, String streamerUsername, String botUsername, String authToken, String clientId, boolean silentChat) {
        this.chatLogger = chatLogger;
        this.authToken = authToken;
        this.silentChat = silentChat;
        twitchClient = TwitchClientBuilder.builder()
                .withClientId(clientId)
                .withEnableChat(true)
                .withEnableTMI(true)
                .withEnableHelix(true)
                .withEnablePubSub(true)
                .build();
        twitchClient.getChat().joinChannel(streamerUsername);
    
        streamerUser = getUserByUsername(streamerUsername);
        botUser = getUserByUsername(botUsername);
    }
    
    public User getStreamerUser() {
        return streamerUser;
    }
    
    public User getBotUser() {
        return botUser;
    }
    
    public TwitchPubSub getPubSub() {
        return twitchClient.getPubSub();
    }
    
    public void registerEventListener(TwitchEventListener eventListener) {
        twitchClient.getEventManager().onEvent(ChannelMessageEvent.class, eventListener::onPrivMsg);
        twitchClient.getEventManager().onEvent(ChannelGoLiveEvent.class, eventListener::onGoLive);
    
        // not sure how I feel about storing all the reserved commands here, but I'm not sure where would fit better
        if (eventListener instanceof CommandBase) {
            String[] commands = ((CommandBase) eventListener).getCommandWords().split("\\|");
            reservedCommands.addAll(Arrays.asList(commands));
        }
    }
    
    public Set<String> getReservedCommands() {
        return reservedCommands;
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
        }
        else {
            sendMessage(output);
        }
    }
    
    /**
     * Sends a message to twitch chat with no restrictions on commands
     *
     * @param line message to send
     */
    public void channelCommand(String line) {
        String output = line.trim();
        if (output.isEmpty()) {
            return;
        }
        String firstWord = output.split("\\s", 2)[0];
        if (firstWord.charAt(0) == '/') {
            System.out.printf("command message sent \"%s\"%n", output);
        }
        sendMessage(output);
    }
    
    public void whisper(String username, String message) {
        twitchClient.getChat().sendPrivateMessage(username, message);
    }
    
    private void sendMessage(String message) {
        if (silentChat) {
            System.out.println("SILENT_CHAT: " + message);
        }
        else {
            twitchClient.getChat().sendMessage(streamerUser.getDisplayName(), message);
            chatLogger.logMessage(botUser, message);
        }
    }
    
    //////////////////////////////////////////////////////////////////////////
    
    public Chatters getChatters() throws HystrixRuntimeException {
        return twitchClient.getMessagingInterface().getChatters(streamerUser.getLogin()).execute();
    }
    
    //////////////////////////////////////////////////////////////////////////
    
    @Nullable
    public Clip getClipById(String id) throws HystrixRuntimeException {
        ClipList clipList = twitchClient.getHelix().getClips(
                authToken,
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
                authToken,
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
                authToken,
                null,
                userId,
                null,
                null
        ).execute();
        return followList.getTotal();
    }

    public List<Follow> getFollowers(String userId) throws HystrixRuntimeException {
        String cursor = null;
        List<Follow> followsOutput = new ArrayList<>();

        do {
            FollowList followList = twitchClient.getHelix().getFollowers(
                    authToken,
                    null,
                    userId,
                    cursor,
                    100
            ).execute();
            cursor = followList.getPagination().getCursor();
            followsOutput.addAll(followList.getFollows());
        } while (cursor != null);
        return followsOutput;
    }

    public List<Follow> getFollowList(String userId) throws HystrixRuntimeException {
        String cursor = null;
        List<Follow> followsOutput = new ArrayList<>();

        do {
            FollowList followList = twitchClient.getHelix().getFollowers(
                    authToken,
                    userId,
                    null,
                    cursor,
                    100
            ).execute();
            cursor = followList.getPagination().getCursor();
            followsOutput.addAll(followList.getFollows());
        } while (cursor != null);
        return followsOutput;
    }

    @Nullable
    public Game getGameById(String gameId) throws HystrixRuntimeException {
        GameList gameList = twitchClient.getHelix().getGames(
                authToken,
                Collections.singletonList(gameId),
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
                    authToken,
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
                authToken,
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
                    authToken,
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
                authToken,
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
                authToken,
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
                authToken,
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
                    authToken,
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
                    authToken,
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
                authToken,
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
        CustomRewardList customRewardList = twitchClient.getHelix().createCustomReward(authToken, broadcasterId, customReward).execute();
        return customRewardList.getRewards().get(0);
    }
    
    public void deleteCustomReward(String broadcasterId, String rewardId) throws HystrixRuntimeException {
        twitchClient.getHelix().deleteCustomReward(authToken, broadcasterId, rewardId).execute();
    }
    
    public List<CustomReward> getCustomRewards(String broadcasterId, Collection<String> rewardIds, Boolean onlyManageableRewards) throws HystrixRuntimeException {
        return twitchClient.getHelix().getCustomRewards(authToken, broadcasterId, rewardIds, onlyManageableRewards).execute().getRewards();
    }
    
    public CustomReward updateCustomReward(String broadcasterId, String rewardId, CustomReward updatedReward) throws  HystrixRuntimeException {
        CustomRewardList customRewardList = twitchClient.getHelix().updateCustomReward(authToken, broadcasterId, rewardId, updatedReward).execute();
        return customRewardList.getRewards().get(0);
    }
    
    public List<CustomRewardRedemption> getCustomRewardRedemptions(String broadcasterId, String rewardId, Collection<String> redemptionIds) throws HystrixRuntimeException {
        String cursor = null;
        List<CustomRewardRedemption> redemptionsOutput = new ArrayList<>();
    
        do {
            CustomRewardRedemptionList redemptionList = twitchClient.getHelix().getCustomRewardRedemption(
                    authToken,
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
        twitchClient.getHelix().updateRedemptionStatus(authToken, broadcasterId, rewardId, redemptionIds, newStatus).execute();
    }
}
