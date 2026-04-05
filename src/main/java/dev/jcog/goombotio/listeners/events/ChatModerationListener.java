package dev.jcog.goombotio.listeners.events;

import com.github.twitch4j.eventsub.events.StreamOnlineEvent;
import com.github.twitch4j.eventsub.events.SuspiciousUserMessageEvent;
import dev.jcog.goombotio.listeners.TwitchEventListener;
import dev.jcog.goombotio.util.CommonUtils;
import dev.jcog.goombotio.util.TwitchApi;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dev.jcog.goombotio.listeners.TwitchEventListener.EVENT_TYPE.*;

public class ChatModerationListener implements TwitchEventListener {
    private final TwitchApi twitchApi;
    private final Set<String> susUsers;

    public ChatModerationListener(CommonUtils commonUtils) {
        twitchApi = commonUtils.twitchApi();
        susUsers = new HashSet<>();
    }

    @Override
    public List<EVENT_TYPE> getEventTypes() {
        return List.of(GO_LIVE, SUSPICIOUS_USER_MESSAGE);
    }

    @Override
    public void onGoLive(StreamOnlineEvent goLiveEvent) {
        susUsers.clear();
    }

    @Override
    public void onSuspiciousUserMessage(SuspiciousUserMessageEvent suspiciousUserMessageEvent) {
        if (susUsers.contains(suspiciousUserMessageEvent.getUserId())) {
            return;
        }

        twitchApi.sendWhisper(twitchApi.getStreamerUser().getId(), String.format(
                "Suspicious user @%s in chat",
                suspiciousUserMessageEvent.getUserName()
        ));
        susUsers.add(suspiciousUserMessageEvent.getUserId());
    }
}
