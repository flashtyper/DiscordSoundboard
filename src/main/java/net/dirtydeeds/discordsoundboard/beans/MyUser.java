package net.dirtydeeds.discordsoundboard.beans;

import lombok.*;
import net.dv8tion.jda.api.JDA;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Class that represents a user of discord.
 *
 * @author dfurrer.
 */
@Data
@Entity
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class MyUser {

    @Id
    @NonNull
    private String id;
    private String username;
    private String entranceSound;
    private String leaveSound;
    @NonNull
    private Boolean selected;
    @NonNull
    private JDA.Status status;

    public MyUser(@NonNull String id, String username, @NonNull Boolean selected, @NonNull JDA.Status status) {
        this.id = id;
        this.username = username;
        this.selected = selected;
        this.status = status;
    }
}