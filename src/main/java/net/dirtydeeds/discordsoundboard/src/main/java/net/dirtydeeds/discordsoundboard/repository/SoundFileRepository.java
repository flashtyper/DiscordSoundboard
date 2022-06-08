package net.dirtydeeds.discordsoundboard.repository;

import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author dfurrer.
 */
public interface SoundFileRepository extends PagingAndSortingRepository<SoundFile, String> {
    SoundFile findOneBySoundFileIdIgnoreCase(String name);
}
