package x.y.z.backend.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Default storage provider: reads from a local directory. Used both for bare-metal local runs
 * and for local Docker Desktop (where ATTACHMENT_LOCAL_DIR points at a bind-mounted volume
 * shared with the processes container, see docker-compose.yml) - identical code either way,
 * only the configured path differs.
 */
@Component
@ConditionalOnProperty(name = "attachment.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileSystemAttachmentStorageService implements AttachmentStorageService {

    @Value("${attachment.storage.local.base-dir:./data/attachments}")
    private String baseDir;

    @Override
    public Resource load(String storageLocation) throws IOException {
        Path path = Paths.get(baseDir).resolve(storageLocation).normalize();
        if (!path.startsWith(Paths.get(baseDir).normalize()) || !Files.exists(path)) {
            throw new IOException("Attachment file not found: " + storageLocation);
        }
        return new FileSystemResource(path);
    }
}
