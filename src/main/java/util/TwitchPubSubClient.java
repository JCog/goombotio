package util;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.eventsub.events.ChannelPredictionEvent;
import com.github.twitch4j.pubsub.TwitchPubSub;
import com.github.twitch4j.pubsub.TwitchPubSubBuilder;
import com.github.twitch4j.pubsub.events.*;

public abstract class TwitchPubSubClient {
    private final String streamerId;
    private final TwitchPubSub client;
    private final OAuth2Credential oAuth2Credential;

    public TwitchPubSubClient(String streamerId, String authToken) {
        this.streamerId = streamerId;
        client = TwitchPubSubBuilder.builder().build();
        oAuth2Credential = new OAuth2Credential("twitch", authToken);
    }

    public TwitchPubSubClient listenForBits() {
        client.listenForCheerEvents(oAuth2Credential, streamerId);
        client.getEventManager().onEvent(ChannelBitsEvent.class, this::onBitsEvent);
        return this;
    }

    public TwitchPubSubClient listenForChannelPoints() {
        client.listenForChannelPointsRedemptionEvents(oAuth2Credential, streamerId);
        client.getEventManager().onEvent(RewardRedeemedEvent.class, this::onChannelPointsEvent);
        return this;
    }

    public TwitchPubSubClient listenForSubGifts() {
        client.listenForChannelSubGiftsEvents(oAuth2Credential, streamerId);
        client.getEventManager().onEvent(ChannelSubGiftEvent.class, this::onSubGiftsEvent);
        return this;
    }
    
    public TwitchPubSubClient listenForPredictions() {
        client.listenForChannelPredictionsEvents(oAuth2Credential, streamerId);
        client.getEventManager().onEvent(ChannelPredictionEvent.class, this::onPredictionsEvent);
        return this;
    }
    
    public TwitchPubSubClient listenForHypeTrainStart() {
        client.listenForChannelPredictionsEvents(oAuth2Credential, streamerId);
        client.getEventManager().onEvent(HypeTrainStartEvent.class, this::onHypeTrainStartEvent);
        return this;
    }
    
    public TwitchPubSubClient listenForHypeTrainEnd() {
        client.listenForChannelPredictionsEvents(oAuth2Credential, streamerId);
        client.getEventManager().onEvent(HypeTrainEndEvent.class, this::onHypeTrainEndEvent);
        return this;
    }
    
    public void close() {
        client.close();
    }

    public abstract void onBitsEvent(ChannelBitsEvent event);

    public abstract void onChannelPointsEvent(RewardRedeemedEvent event);

    public abstract void onSubGiftsEvent(ChannelSubGiftEvent event);
    
    public abstract void onPredictionsEvent(ChannelPredictionEvent event);
    
    public abstract void onHypeTrainStartEvent(HypeTrainStartEvent event);
    
    public abstract void onHypeTrainEndEvent(HypeTrainEndEvent event);
}
