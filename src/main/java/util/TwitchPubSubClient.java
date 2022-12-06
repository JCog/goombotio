package util;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.eventsub.events.ChannelPredictionEvent;
import com.github.twitch4j.pubsub.TwitchPubSub;
import com.github.twitch4j.pubsub.events.*;

public abstract class TwitchPubSubClient {
    private final String streamerId;
    private final TwitchPubSub pubSub;
    private final OAuth2Credential oAuth2Credential;

    public TwitchPubSubClient(TwitchApi twitchApi, String streamerId, String authToken) {
        this.pubSub = twitchApi.getPubSub();
        this.streamerId = streamerId;
        oAuth2Credential = new OAuth2Credential("twitch", authToken);
    }

    public TwitchPubSubClient listenForBits() {
        pubSub.listenForCheerEvents(oAuth2Credential, streamerId);
        pubSub.getEventManager().onEvent(ChannelBitsEvent.class, this::onBitsEvent);
        return this;
    }

    public TwitchPubSubClient listenForChannelPoints() {
        pubSub.listenForChannelPointsRedemptionEvents(oAuth2Credential, streamerId);
        pubSub.getEventManager().onEvent(RewardRedeemedEvent.class, this::onChannelPointsEvent);
        return this;
    }

    public TwitchPubSubClient listenForSubGifts() {
        pubSub.listenForChannelSubGiftsEvents(oAuth2Credential, streamerId);
        pubSub.getEventManager().onEvent(ChannelSubGiftEvent.class, this::onSubGiftsEvent);
        return this;
    }
    
    public TwitchPubSubClient listenForPredictions() {
        pubSub.listenForChannelPredictionsEvents(oAuth2Credential, streamerId);
        pubSub.getEventManager().onEvent(ChannelPredictionEvent.class, this::onPredictionsEvent);
        return this;
    }
    
    public TwitchPubSubClient listenForHypeTrainStart() {
        pubSub.listenForChannelPredictionsEvents(oAuth2Credential, streamerId);
        pubSub.getEventManager().onEvent(HypeTrainStartEvent.class, this::onHypeTrainStartEvent);
        return this;
    }
    
    public TwitchPubSubClient listenForHypeTrainEnd() {
        pubSub.listenForChannelPredictionsEvents(oAuth2Credential, streamerId);
        pubSub.getEventManager().onEvent(HypeTrainEndEvent.class, this::onHypeTrainEndEvent);
        return this;
    }

    public abstract void onBitsEvent(ChannelBitsEvent event);

    public abstract void onChannelPointsEvent(RewardRedeemedEvent event);

    public abstract void onSubGiftsEvent(ChannelSubGiftEvent event);
    
    public abstract void onPredictionsEvent(ChannelPredictionEvent event);
    
    public abstract void onHypeTrainStartEvent(HypeTrainStartEvent event);
    
    public abstract void onHypeTrainEndEvent(HypeTrainEndEvent event);
}
