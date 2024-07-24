package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.MyUser;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.service.SoundService;
import net.dirtydeeds.discordsoundboard.service.UserService;

/**
 * @author Dave Furrer
 * <p>
 * Command to set or clear the sound that plays when a user enters a voice channel.
 * Arugument can be username or userId
 */
public class EntranceCommand extends Command {

    private final UserService userService;
    private final SoundService soundService;
    private final SoundPlayer soundPlayer;

    public EntranceCommand(SoundPlayer soundPlayer, UserService userService,
                           SoundService soundService) {
        this.soundPlayer = soundPlayer;
        this.userService = userService;
        this.soundService = soundService;
        this.name = "entrance";
        this.help = "Sets entrance sound for user. Leave soundFileName empty to remove";
    }

    @Override
    protected void execute(CommandEvent event) {
        if (!event.getArguments().isEmpty()) {
            String userNameOrId = event.getArguments().getFirst();
            String soundFileName = "";
            if (event.getArguments().size() == 2) {
                soundFileName = event.getArguments().get(1);
            }

            net.dv8tion.jda.api.entities.User pmUser = event.getAuthor();
            if (event.userIsAdmin() ||
                    (pmUser.getName().equalsIgnoreCase(userNameOrId)
                            || pmUser.getId().equals(userNameOrId))) {
                MyUser myUser = userService.findOneByIdOrUsernameIgnoreCase(userNameOrId, userNameOrId);
                if (myUser == null) {
                    net.dv8tion.jda.api.entities.User jdaUser = soundPlayer.retrieveUserById(userNameOrId);
                    if (jdaUser != null) {
                        myUser = new MyUser(jdaUser.getId(), jdaUser.getName(), false, jdaUser.getJDA().getStatus());
                    }
                }
                if (myUser != null) {
                    if (soundFileName.isEmpty()) {
                        myUser.setEntranceSound(null);
                        event.replyByPrivateMessage("User: " + userNameOrId + " entrance sound cleared");
                        userService.save(myUser);
                    } else {
                        SoundFile soundFile = soundService.findOneBySoundFileIdIgnoreCase(soundFileName);
                        if (soundFile == null) {
                            event.replyByPrivateMessage("Could not find sound file: " + soundFileName);
                        } else {
                            myUser.setEntranceSound(soundFileName);
                            event.replyByPrivateMessage("User: " + userNameOrId + " entrance sound set to: " + soundFileName);
                            userService.save(myUser);
                        }
                    }
                } else {
                    event.replyByPrivateMessage("Could not find user with id or name: " + userNameOrId);
                }
            } else {
                //TODO give a better message. It's likely they don't have permission in this case
                event.replyByPrivateMessage("Entrance command incorrect. Required input is " +
                        event.getPrefix() + "entrance <userid/username> <soundfile>");
            }
        }
    }
}