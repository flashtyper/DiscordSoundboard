package net.dirtydeeds.discordsoundboard.beans;

import jakarta.annotation.Nullable;
import lombok.*;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.ZonedDateTime;

/**
 * Class that represents a sound file.
 *
 * @author dfurrer.
 */
@Data
@Entity
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class SoundFile {
    
    @Id
    @NonNull
    private String soundFileId;
    @NonNull
    private String soundFileLocation;
    @NonNull
    private String category;
    @Nullable
    private Integer timesPlayed;
    @Nullable
    private ZonedDateTime dateAdded;
}