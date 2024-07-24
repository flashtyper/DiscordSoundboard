package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.MyUser;
import net.dirtydeeds.discordsoundboard.service.UserService;

/**
 * @author Dave Furrer
 * <p>
 * Command to get the details of the request user
 */
public class UserDetailsCommand extends Command {

    private final UserService userService;
    private final SoundPlayer soundPlayer;

    public UserDetailsCommand(UserService userService, SoundPlayer soundPlayer) {
        this.userService = userService;
        this.soundPlayer = soundPlayer;
        this.name = "userdetails";
        this.help = "userDetails userName - Get details for user";
    }

    @Override
    protected void execute(CommandEvent event) {
        if (!event.getArguments().isEmpty()) {
            String userNameOrId = event.getArguments().getFirst();
            MyUser myUser = userService.findOneByIdOrUsernameIgnoreCase(userNameOrId, userNameOrId);
            if (myUser == null) {
                net.dv8tion.jda.api.entities.User jdaUser = soundPlayer.retrieveUserById(userNameOrId);
                if (jdaUser != null) {
                    myUser = new MyUser(jdaUser.getId(), jdaUser.getName(), false, jdaUser.getJDA().getStatus());
                }
            }
            if (myUser != null) {
                StringBuilder response = new StringBuilder();
                response.append("User details for ").append(userNameOrId).append("```")
                        .append("\nDiscord Id: ").append(myUser.getId())
                        .append("\nUsername: ").append(myUser.getUsername())
                        .append("\nEntrance Sound: ");
                if (myUser.getEntranceSound() != null) {
                    response.append(myUser.getEntranceSound());
                }
                response.append("\nLeave Sound: ");
                if (myUser.getLeaveSound() != null) {
                    response.append(myUser.getLeaveSound());
                }
                response.append("```");
                event.replyByPrivateMessage(response.toString());
            }
        }
    }
}