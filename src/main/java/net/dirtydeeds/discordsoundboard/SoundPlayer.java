package net.dirtydeeds.discordsoundboard;

import com.sedmelluq.discord.lavaplayer.natives.ConnectorNativeLibLoader;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.PreDestroy;
import net.dirtydeeds.discordsoundboard.beans.MyUser;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.commands.*;
import net.dirtydeeds.discordsoundboard.controllers.response.ChannelResponse;
import net.dirtydeeds.discordsoundboard.listeners.*;
import net.dirtydeeds.discordsoundboard.handlers.AudioHandler;
import net.dirtydeeds.discordsoundboard.service.SoundService;
import net.dirtydeeds.discordsoundboard.service.UserService;
import net.dirtydeeds.discordsoundboard.util.ShutdownManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.*;
import java.nio.file.*;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author dfurrer.
 * <p>
 * This class handles moving into channels and playing sounds. Also, it loads the available sound files
 * and the configuration properties.
 */
@Component
@Singleton
public class SoundPlayer {

    private static final Logger LOG = LoggerFactory.getLogger(SoundPlayer.class);

    private final SoundService soundService;
    private final UserService userService;
    private final MainWatch mainWatch;
    @SuppressWarnings("unused")
    @Autowired
    private ServletWebServerApplicationContext webServerAppCtxt;
    private final ShutdownManager shutdownManager;
    private final BotConfig botConfig;
    private JDA bot;
    private JDABot jdaBot;

    private Date lastReload;

    @Inject
    public SoundPlayer(MainWatch mainWatch, SoundService soundService,
                       UserService userService, ShutdownManager shutdownManager, BotConfig botConfig) {
        this.mainWatch = mainWatch;
        this.mainWatch.setSoundPlayer(this);
        this.soundService = soundService;
        this.userService = userService;
        this.shutdownManager = shutdownManager;
        this.botConfig = botConfig;

        init();
    }

    private void init() {
        jdaBot = new JDABot(botConfig);
        bot = jdaBot.getJda();
        if (bot == null) {
            shutdownManager.initiateShutdown(0);
            return;
        }

        updateFileList();
        getUsers();

        CommandListener commandListener = new CommandListener(botConfig);
        commandListener.addCommand(new EntranceCommand(this, userService, soundService));
        commandListener.addCommand(new InfoCommand(this, botConfig));
        commandListener.addCommand(new LeaveCommand(this, userService, soundService));

        bot.addEventListener(commandListener);
        bot.addEventListener(new EntranceSoundBoardListener(this, userService, soundService,
                botConfig.isPlayEntranceOnJoin(), botConfig));
        bot.addEventListener(new LeaveSoundBoardListener(this, userService, soundService, botConfig));
        bot.addEventListener(new MovedChannelListener(this, userService, soundService,
                botConfig.isPlayEntranceOnMove(), botConfig));
        bot.addEventListener(new BotLeaveListener(botConfig));

        ConnectorNativeLibLoader.loadConnectorLibrary();

        mainWatch.watchDirectoryPath(Paths.get(botConfig.getSoundFileDir()));
    }

    public ServletWebServerApplicationContext getApplicationContext() {
        return webServerAppCtxt;
    }

    /**
     * Gets a Map of the loaded sound files.
     *
     * @return Map of sound files that have been loaded.
     */
    public Map<String, SoundFile> getAvailableSoundFiles() {
        Map<String, SoundFile> returnFiles = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (SoundFile soundFile : soundService.findAll(Pageable.unpaged())) {
            returnFiles.put(soundFile.getSoundFileId(), soundFile);
        }
        return returnFiles;
    }

    public long getLastReloadTimestamp() {
        return lastReload.getTime() / 1000L;
    }


    public void playRandomSoundFile(String voiceChannelID) throws SoundPlaybackException {
        try {
            Map<String, SoundFile> sounds = getAvailableSoundFiles();
            List<String> keysAsArray = new ArrayList<>(sounds.keySet());
            Random r = new Random();
            SoundFile randomValue = sounds.get(keysAsArray.get(r.nextInt(keysAsArray.size())));

            LOG.info("Attempting to play random file: " + randomValue.getSoundFileId());
            try {
                playFileInChannel(randomValue.getSoundFileId(), voiceChannelID);
            } catch (Exception e) {
                LOG.error("Could not play random file: " + randomValue.getSoundFileId());
            }
        } catch (Exception e) {
            throw new SoundPlaybackException("Problem playing random file.");
        }
    }

    /**
     * Plays the fileName requested in the requested channel.
     *
     * @param fileName - The name of the file to play.
     * @param channel  -  The channel to play the file in
     */
    public void playFileInChannel(String fileName, VoiceChannel channel) {
        if (channel == null) return;
        moveToChannel(channel, channel.getGuild());
        LOG.info("Playing file for user: " + fileName + " in channel: " + channel.getName());
        if (fileName != null) {
            playFile(fileName, channel.getGuild(), 1);

        }
        if (botConfig.isLeaveAfterPlayback()) {
            disconnectFromChannel(channel.getGuild());
        }
    }

    public void playFileInChannel(String fileName, String channelId) {
        VoiceChannel channel = bot.getVoiceChannelById(channelId);
        playFileInChannel(fileName, channel);
    }

    /**
     * Plays the fileName requested.
     *
     * @param fileName - The name of the file to play.
     * @param event    -  The event that triggered the sound playing request. The event is used to find the channel to play
     *                 the sound back in.
     */
    private void playFileForEvent(String fileName, MessageReceivedEvent event) {
        SoundFile fileToPlay = soundService.findOneBySoundFileIdIgnoreCase(fileName);
        if (event != null) {
            Guild guild = event.getGuild();
            if (fileToPlay != null) {
                moveToUserIdsChannel(event, guild);

                playFile(fileName, guild, 1);

                if (botConfig.isLeaveAfterPlayback()) {
                    disconnectFromChannel(event.getGuild());
                }
            } else {
                event.getAuthor().openPrivateChannel().complete().sendMessage("Could not find sound to play. Requested sound: " + fileName + ".").queue();
            }
        }
    }

    /**
     * Play file name requested. Will first try to load the file from the map of available sounds.
     *
     * @param fileName - fileName to play.
     */
    private void playFile(String fileName, Guild guild, Integer repeatTimes) {
        SoundFile fileToPlay = soundService.findOneBySoundFileIdIgnoreCase(fileName);

        if (fileToPlay != null) {
            File soundFile = new File(fileToPlay.getSoundFileLocation());
            if (guild == null) {
                LOG.error("Guild is null or you're not in a voice channel the bot has permission to access. Have you added your bot to a guild? https://discord.com/developers/docs/topics/oauth2");
            } else {
                fileToPlay = soundService.updateSoundPlayed(fileToPlay);
                soundService.save(fileToPlay);
                jdaBot.getPlayerManager().loadItem(soundFile.getAbsolutePath(), new FileLoadResultHandler(guild, repeatTimes));
            }
        } else {
            jdaBot.getPlayerManager().loadItem(fileName, new FileLoadResultHandler(guild, repeatTimes));
        }
    }

    /**
     * Stops sound playback and returns true or false depending on if playback was stopped.
     *
     * @return boolean representing whether playback was stopped.
     */
    public boolean stop(String user, String voiceChannelId) {
        LOG.info("STOPPING PLAYBACK FOR: User:" + user + "or Channel" + voiceChannelId);
        Guild guild = getGuildForUserOrChannelId(user, voiceChannelId);
        LOG.info("STOPPING FOR GUILD: " + guild.toString());
        if (guild != null) {
            LOG.info("STOPPING GUILD IS NOT NULL");
            AudioHandler handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
            LOG.info("STOPPING HANDLER " + handler.toString());
            if (handler != null) {
                LOG.info("STOPPING HANDLER " + handler.getPlayer().toString());
                handler.getPlayer().stopTrack();
                return true;
            }
        }

        return false;
    }

    /**
     * Get a list of users
     *
     * @return List of soundboard users.
     */
    public List<MyUser> getUsers() {
        //String userNameToSelect = botConfig.getBotOwnerName();
        List<MyUser> myUsers = new ArrayList<>();
        for (net.dv8tion.jda.api.entities.User discordUser : bot.getUsers()) {
            boolean selected = false;
            String username = discordUser.getName();
            //if (userNameToSelect != null && userNameToSelect.equals(username)) {
            selected = true;
            //}
            Optional<MyUser> optionalUser = userService.findById(discordUser.getId());
            if (optionalUser.isPresent()) {
                MyUser myUser = optionalUser.get();
                myUser.setSelected(selected);
                myUsers.add(myUser);
            } else {
                myUsers.add(new MyUser(discordUser.getId(), username, selected, discordUser.getJDA().getStatus()));
            }
        }
        myUsers.sort(Comparator.comparing(MyUser::getUsername));
        userService.saveAll(myUsers);
        return myUsers;
    }

    public net.dv8tion.jda.api.entities.User retrieveUserById(String id) {
        return bot.retrieveUserById(id).complete();
    }

    public boolean isUserAllowed(String username) {
        if (botConfig.getAllowedUsersList() == null) {
            return true;
        } else if (botConfig.getAllowedUsersList().isEmpty()) {
            return true;
        } else return botConfig.getAllowedUsersList().contains(username);
    }

    public boolean isUserBanned(String username) {
        return botConfig.getBannedUsersList() != null && !botConfig.getBannedUsersList().isEmpty()
                && botConfig.getBannedUsersList().contains(username);
    }

    /**
     * This method loads the files. This checks if you are running from a .jar file and loads from the /sounds dir relative
     * to the jar file. If not it assumes you are running from code and loads relative to your resource dir.
     */
    public void updateFileList() {
        try {
            String soundFileDir = botConfig.getSoundFileDir();
            if (StringUtils.isEmpty(soundFileDir)) {
                soundFileDir = System.getProperty("user.dir") + "/sounds";
            }
            LOG.info("Loading from " + soundFileDir);
            Path soundFilePath = Paths.get(soundFileDir);

            if (!soundFilePath.toFile().exists()) {
                System.out.println("creating directory: " + soundFilePath.toFile());
                boolean result = false;

                try {
                    result = soundFilePath.toFile().mkdir();
                } catch (SecurityException se) {
                    LOG.error("Could not create directory: " + soundFilePath.toFile());
                }
                if (result) {
                    LOG.info("DIR: " + soundFilePath.toFile() + " created.");
                }
            }

            List<Path> dirList = Files.walk(soundFilePath).collect(Collectors.toList());
            for (SoundFile sound : soundService.findAll(Pageable.unpaged())) {
                if (!dirList.removeIf(xx -> xx.toString().equals(sound.getSoundFileLocation()))) {
                    soundService.delete(sound);
                }
            }


            for (Path filePath : dirList) {
                if (Files.isRegularFile(filePath)) {
                    String fileName = filePath.getFileName().toString();
                    fileName = fileName.substring(fileName.indexOf("/") + 1);
                    int fileExtensionPeriodIndex = fileName.lastIndexOf(".");
                    if (fileExtensionPeriodIndex > 0) {
                        fileName = fileName.substring(0, fileExtensionPeriodIndex);
                        //LOG.info(fileName);
                        File file = filePath.toFile();
                        String parent = file.getParentFile().getName();
                        SoundFile soundFile = new SoundFile(fileName, filePath.toString(), parent, 0, ZonedDateTime.now());
                        soundService.save(soundFile);
                    }
                }
            }

            lastReload = new Date();
        } catch (IOException e) {
            LOG.error(e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Looks through all the guilds the bot has access to and returns the VoiceChannel the requested user is connected to.
     *
     * @param userName       - The username to look for.
     * @param voiceChannelId - The voice channel to return the guild for.
     * @return The voice channel the user is connected to. If user is not connected to a voice channel will return null.
     */
    private Guild getGuildForUserOrChannelId(String userName, String voiceChannelId) {
        if (StringUtils.isBlank(voiceChannelId) || voiceChannelId.equals("undefined")) {
            for (Guild guild : bot.getGuilds()) {
                for (VoiceChannel channel : guild.getVoiceChannels()) {
                    for (Member user : channel.getMembers()) {
                        if (user.getEffectiveName().equalsIgnoreCase(userName)
                                || user.getUser().getName().equalsIgnoreCase(userName)
                                || user.getId().equals(userName)) {
                            return guild;
                        }
                    }
                }
            }
        } else if (!StringUtils.isBlank(voiceChannelId)) {
            return Objects.requireNonNull(bot.getVoiceChannelById(voiceChannelId)).getGuild();
        }

        return null;
    }

    /**
     * Find the "author" of the event and join the voice channel they are in.
     *
     * @param event - The event
     */
    private void moveToUserIdsChannel(MessageReceivedEvent event, Guild guild) {
        VoiceChannel channel = findUsersChannel(event, guild);

        if (channel == null) {
            event.getAuthor().openPrivateChannel().complete()
                    .sendMessage("Hello @" + event.getAuthor().getName() + "! I can not find you in any Voice Channel. Are you sure you are connected to voice?.").queue();
            LOG.warn("Problem moving to requested users channel. Maybe user, " + event.getAuthor().getName() + " is not connected to Voice?");
        } else {
            moveToChannel(channel, guild);
        }
    }

    /**
     * Moves to the specified voice channel.
     *
     * @param channel - The channel specified.
     */
    private void moveToChannel(VoiceChannel channel, Guild guild) {
        AudioManager audioManager = guild.getAudioManager();

        audioManager.openAudioConnection(channel);

        int i = 0;
        int waitTime = 100;
        int maxIterations = 40;
        //Wait for the audio connection to be ready before proceeding.
        synchronized (this) {
            try {
                while (!audioManager.isConnected()) {
                    wait(waitTime);
                    i++;
                    if (i >= maxIterations) {
                        break; //break out if after 1 second it doesn't get a connection;
                    }

                }
            } catch (InterruptedException e) {
                LOG.warn("Waiting for audio connection was interrupted.");
            }
        }
    }

    /**
     * Finds a users voice channel based on event and what guild to look in.
     *
     * @param event - The event that triggered this search. This is used to get th events author.
     * @param guild - The guild (discord server) to look in for the author.
     * @return The VoiceChannel if one is found. Otherwise return null.
     */
    private VoiceChannel findUsersChannel(MessageReceivedEvent event, Guild guild) {
        VoiceChannel channel = null;

        outerloop:
        for (VoiceChannel channel1 : guild.getVoiceChannels()) {
            for (Member user : channel1.getMembers()) {
                if (user.getId().equals(event.getAuthor().getId())) {
                    channel = channel1;
                    break outerloop;
                }
            }
        }

        return channel;
    }

    private List<VoiceChannel> getCurrentVoiceChannel() {
        List<VoiceChannel> connectedChannel = new LinkedList<VoiceChannel>();
        for (AudioManager audioManager : bot.getAudioManagers()) {
            if (audioManager.getConnectedChannel() != null) {
                connectedChannel.add(audioManager.getConnectedChannel().asVoiceChannel());
            }
        }
        return connectedChannel;
    }

    private boolean isAllowedToJoinChannel(VoiceChannel channel) {
        return PermissionUtil.checkPermission(channel, channel.getGuild().getSelfMember(), Permission.VOICE_CONNECT);
    }

    public List<ChannelResponse> getVoiceChannels() {
        List<VoiceChannel> connectedChannel = getCurrentVoiceChannel();


        return bot.getVoiceChannels().stream()
                .map(v -> {
                            boolean isCurrentChannel = false;
                            for (VoiceChannel channel : connectedChannel) {
                                if (channel != null && channel.getId().equals(v.getId())) {
                                    isCurrentChannel = true;
                                }
                            }
                            if (isAllowedToJoinChannel(v)) {
                                return new ChannelResponse(v.getName(), v.getId(), v.getGuild().getName(),
                                        v.getGuild().getId(), isCurrentChannel);
                            }
                            return null;
                        }
                ).collect(Collectors.toList());
    }

    public void disconnectFromChannel(Guild guild) {
        if (guild != null) {
            guild.getAudioManager().closeAudioConnection();
            LOG.info("Disconnecting from channel.");
        }
    }

    public void disconnectFromAllChannels() {
        for (Guild guild : bot.getGuilds()) {
            if (guild != null) {
                guild.getAudioManager().closeAudioConnection();
            }
        }
        LOG.info("Disconnecting from all channels.");
    }

    @PreDestroy
    @SuppressWarnings("unused")
    public void cleanUp() {
        System.out.println("SoundPlayer is shutting down. Cleaning up.");
        bot.shutdown();
    }
}
