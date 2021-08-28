package functions;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;

public class DiscordListener extends ListenerAdapter {
    
    private static final String TWITCH_ROLE_ID = "twitch-alert";
    private static final String YOUTUBE_ROLE_ID = "youtube-alert";
    private static final String LOGGING_CHANNEL = "server-logging";
    
    @Override
    public void onButtonClick(@Nonnull ButtonClickEvent event) {
        switch (event.getComponentId()) {
            case "twitch":
                if (toggleTwitchRole(event.getGuild(), event.getMember())) {
                    event.reply("You have been given the @twitch-alert role and will now be pinged when JCog goes live on Twitch.").setEphemeral(true).queue();
                    log(event.getGuild(), "%s has been given **@twitch-alert**", event.getMember());
                }
                else {
                    event.reply("The @twitch-alert role has been removed and you'll no longer be pinged when JCog goes live on Twitch.").setEphemeral(true).queue();
                    log(event.getGuild(), "%s has removed **@twitch-alert**", event.getMember());
                }
                break;
            case "youtube":
                if (toggleYoutubeRole(event.getGuild(), event.getMember())) {
                    event.reply("You have been given the @youtube-alert role and will now be pinged when JCog uploads new YouTube videos.").setEphemeral(true).queue();
                    log(event.getGuild(), "%s has been given **@youtube-alert**", event.getMember());
                }
                else {
                    event.reply("The @youtube-alert role has been removed and you'll no longer be pinged when JCog uploads new YouTube videos.").setEphemeral(true).queue();
                    log(event.getGuild(), "%s has removed **@youtube-alert**", event.getMember());
                }
                break;
        }
    }
    
    private void log(Guild server, String message, @Nonnull Object... args) {
        TextChannel channel = server.getTextChannelsByName(LOGGING_CHANNEL, true).get(0);
        channel.sendMessageFormat(message, args).queue();
    }
    
    private boolean toggleTwitchRole(Guild server, Member member) {
        Role twitchAlertRole = server.getRolesByName(TWITCH_ROLE_ID, false).get(0);
        return toggleRole(server, member, twitchAlertRole);
    }
    
    private boolean toggleYoutubeRole(Guild server, Member member) {
        Role youtubeAlertRole = server.getRolesByName(YOUTUBE_ROLE_ID, false).get(0);
        return toggleRole(server, member, youtubeAlertRole);
    }
    
    private boolean toggleRole(Guild server, Member member, Role role) {
        if (member.getRoles().contains(role)) {
            removeRoleFromMember(server, member, role);
            return false;
        }
        else {
            addRoleToMember(server, member, role);
            return true;
        }
    }
    
    private void addRoleToMember(Guild server, Member member, Role role) {
        server.addRoleToMember(member, role).queue();
    }
    
    private void removeRoleFromMember(Guild server, Member member, Role role) {
        server.removeRoleFromMember(member, role).queue();
    }
}
