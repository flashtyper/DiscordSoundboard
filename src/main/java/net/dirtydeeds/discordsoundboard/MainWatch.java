package net.dirtydeeds.discordsoundboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * MainWatch monitors the sound file directory for changes (create/modify/delete) and updates the file lis if one
 * of those events happen.
 *
 * @author dfurrer.
 */
@Service
public class MainWatch {

    private static final Logger LOG = LoggerFactory.getLogger(MainWatch.class);

    private SoundPlayer soundPlayer;
    private Timer reloadTimer;
    private Map<WatchKey, Path> watchKeyPathMap;
    private final ReentrantLock lock = new ReentrantLock();
    @Async
    public void watchDirectoryPath(Path path) {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            watchKeyPathMap = new HashMap<>();
            reloadTimer = new Timer();
            Boolean isFolder = (Boolean) Files.getAttribute(path,
                    "basic:isDirectory", NOFOLLOW_LINKS);
            if (!isFolder) {
                throw new IllegalArgumentException("Path: " + path + " is not a folder");
            }

            LOG.info("Watching path: {} for changes. Will update sound file list when modified", path);


            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                    LOG.info("Watching sub-path: {} for changes. Will update sound file list when modified", dir.toString());
                    WatchKey dirKey = dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
                    watchKeyPathMap.put(dirKey,dir);
                    return FileVisitResult.CONTINUE;
                }

            });



            while (true) {
                final WatchKey watchKey;
                try {
                    watchKey = watchService.take(); // wait for a key to be available
                } catch (InterruptedException ex) {
                    return;
                }



                watchKey.pollEvents().forEach(event -> {
                    Path eventKeyPath = watchKeyPathMap.get(watchKey);
                    if (event.context().toString().contains("part")){
                        return;
                    }
                    LOG.info("File watcher event: {} from watcher {}", event.context().toString(), path.toAbsolutePath());
                    onChanged();
                } );

                // Reset the watch key everytime for continuing to use it for further event polling
                boolean valid = watchKey.reset();
                if (!valid) {
                    break;
                }
                Thread.sleep(1000);
            }
        } catch (IOException | InterruptedException e) {
            // Folder does not exist or we were interrupted
            e.printStackTrace();
        }
    }

    private void onChanged()
    {
        lock.lock();
        try{

            if (reloadTimer != null){
                LOG.info("File watcher postponing reload Timer");
                reloadTimer.cancel();
            }else {
                LOG.info("File creating reload Timer");
            }
            reloadTimer = new Timer();
            reloadTimer.schedule(new ReloadTask(), 2000 );

        }finally {
            lock.unlock();
        }
    }

    class ReloadTask extends TimerTask {

        public void run() {
            lock.lock();
            try {
                LOG.info("File watcher reload Timer fired");
                soundPlayer.updateFileList();
                reloadTimer.cancel();
            }finally {
                lock.unlock();
            }

        }
    }

    public void setSoundPlayer(SoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
    }
}