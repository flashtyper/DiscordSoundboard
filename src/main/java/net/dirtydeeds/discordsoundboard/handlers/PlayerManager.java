package net.dirtydeeds.discordsoundboard.handlers;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;

import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import net.dirtydeeds.discordsoundboard.JDABot;
import net.dv8tion.jda.api.entities.Guild;

public class PlayerManager extends DefaultAudioPlayerManager {

    private final JDABot bot;

    public PlayerManager(JDABot bot) {
        this.bot = bot;
    }

    public void init() {
        AudioSourceManagers.registerLocalSource(this);
        this.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        this.registerSourceManager(new BandcampAudioSourceManager());
        this.registerSourceManager(new TwitchStreamAudioSourceManager());
        this.registerSourceManager(new BeamAudioSourceManager());
        this.registerSourceManager(new YoutubeAudioSourceManager());
    }


    public JDABot getBot()
    {
        return bot;
    }

    public boolean hasHandler(Guild guild)
    {
        return guild.getAudioManager().getSendingHandler()!=null;
    }

    public AudioHandler setUpHandler(Guild guild) {
        AudioHandler handler;
        if(guild.getAudioManager().getSendingHandler()==null) {
            AudioPlayer player = createPlayer();
            player.setVolume(75);
            handler = new AudioHandler(this, guild, player);
            player.addListener(handler);
            guild.getAudioManager().setSendingHandler(handler);
        }
        else
            handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
        return handler;
    }
}
