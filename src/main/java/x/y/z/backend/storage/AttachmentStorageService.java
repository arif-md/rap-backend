package x.y.z.backend.storage;

import java.io.IOException;

import org.springframework.core.io.Resource;

/**
 * Reads back an attachment's bytes from wherever this environment stores attachments, given
 * the opaque location string recorded in RAP.ATTACHMENTS.storage_location by the processes
 * service's write-side counterpart of this interface
 * (x.y.z.process.service.storage.AttachmentStorageService). Both services are configured with
 * the same "attachment.storage.*" property names so they agree on where a given location
 * string points - callers here only ever see this interface, never branch on environment.
 */
public interface AttachmentStorageService {

    Resource load(String storageLocation) throws IOException;
}
