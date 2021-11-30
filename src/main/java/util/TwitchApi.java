package util;

import com.github.twitch4j.eventsub.domain.RedemptionStatus;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.TwitchHelixBuilder;
import com.github.twitch4j.helix.domain.*;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TwitchApi {
    private final TwitchHelix helixClient;
    private final String authToken;

    public TwitchApi(String authToken, String clientId) {
        this.authToken = authToken;
        helixClient = TwitchHelixBuilder.builder()
                .withClientId(clientId)
                .build();
    }

    @Nullable
    public Clip getClipById(String id) throws HystrixRuntimeException {
        ClipList clipList = helixClient.getClips(
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
        FollowList followList = helixClient.getFollowers(
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
        FollowList followList = helixClient.getFollowers(
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
            FollowList followList = helixClient.getFollowers(
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
            FollowList followList = helixClient.getFollowers(
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
        GameList gameList = helixClient.getGames(
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
            ModeratorList moderatorList = helixClient.getModerators(
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
        StreamList streamList = helixClient.getStreams(
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
            SubscriptionList subscriptionList = helixClient.getSubscriptions(
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
        SubscriptionList subscriptionList = helixClient.getSubscriptions(
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
        UserList userList = helixClient.getUsers(
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
        UserList userList = helixClient.getUsers(
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
            UserList resultList = helixClient.getUsers(
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
            UserList resultList = helixClient.getUsers(
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
        VideoList videoList = helixClient.getVideos(
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
        CustomRewardList customRewardList = helixClient.createCustomReward(authToken, broadcasterId, customReward).execute();
        return customRewardList.getRewards().get(0);
    }
    
    public void deleteCustomReward(String broadcasterId, String rewardId) throws HystrixRuntimeException {
        helixClient.deleteCustomReward(authToken, broadcasterId, rewardId).execute();
    }
    
    public List<CustomReward> getCustomRewards(String broadcasterId, Collection<String> rewardIds, Boolean onlyManageableRewards) throws HystrixRuntimeException {
        return helixClient.getCustomRewards(authToken, broadcasterId, rewardIds, onlyManageableRewards).execute().getRewards();
    }
    
    public CustomReward updateCustomReward(String broadcasterId, String rewardId, CustomReward updatedReward) throws  HystrixRuntimeException {
        CustomRewardList customRewardList = helixClient.updateCustomReward(authToken, broadcasterId, rewardId, updatedReward).execute();
        return customRewardList.getRewards().get(0);
    }
    
    public List<CustomRewardRedemption> getCustomRewardRedemptions(String broadcasterId, String rewardId, Collection<String> redemptionIds) throws HystrixRuntimeException {
        String cursor = null;
        List<CustomRewardRedemption> redemptionsOutput = new ArrayList<>();
    
        do {
            CustomRewardRedemptionList redemptionList = helixClient.getCustomRewardRedemption(
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
        helixClient.updateRedemptionStatus(authToken, broadcasterId, rewardId, redemptionIds, newStatus).execute();
    }
}
