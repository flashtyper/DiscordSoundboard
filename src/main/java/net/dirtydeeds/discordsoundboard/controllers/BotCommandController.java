package net.dirtydeeds.discordsoundboard.controllers;

import net.dirtydeeds.discordsoundboard.SoundPlaybackException;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.controllers.response.ChannelResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.inject.Inject;
import java.util.List;

@RestController
@RequestMapping("/bot")
@SuppressWarnings("unused")
public class BotCommandController {

    private final SoundPlayer soundPlayer;

    @Inject
    public BotCommandController(SoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
    }

    @PostMapping(value = "/playFileInChannel")
    public HttpStatus playSoundFileInChannel(@RequestParam String soundFileId, @RequestParam String voiceChannelId) {
        soundPlayer.playFileInChannel(soundFileId,voiceChannelId);
        return HttpStatus.OK;
    }

    @PostMapping(value = "/connect")
    public HttpStatus connect(@RequestParam String voiceChannelId) {
        soundPlayer.playFileInChannel(null, voiceChannelId);
        return HttpStatus.OK;
    }

    @PostMapping(value = "/playUrl")
    public HttpStatus playSoundUrl(@RequestParam String url, @RequestParam(defaultValue = "") String voiceChannelId) {
        soundPlayer.playFileInChannel(url, voiceChannelId);
        return HttpStatus.OK;
    }

    @PostMapping(value = "/random")
    public HttpStatus soundCommand(@RequestParam(defaultValue = "") String voiceChannelId) {
        try {
            soundPlayer.playRandomSoundFile(voiceChannelId);
        } catch (SoundPlaybackException e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.OK;
    }

    @PostMapping(value = "/stop")
    public HttpStatus stopPlayback(@RequestParam(defaultValue = "") String username,
                                   @RequestParam String voiceChannelId) {
        soundPlayer.stop(username, voiceChannelId);
        return HttpStatus.OK;
    }


    @PostMapping(value = "/reload")
    public HttpStatus reload() {
        soundPlayer.updateFileList();
        return HttpStatus.OK;
    }

    @PostMapping(value = "/disconnect")
    public HttpStatus disconnect() {
        soundPlayer.disconnectFromAllChannels();
        return HttpStatus.OK;
    }

    @GetMapping(value = "/lastReload")
    public long getLastReload() {
        return soundPlayer.getLastReloadTimestamp();
    }


    @GetMapping(value = "/channels")
    public List<ChannelResponse> getVoiceChannels() {
        return soundPlayer.getVoiceChannels();
    }
}
