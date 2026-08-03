package x.y.z.backend.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

/**
 * Azure environment storage provider: downloads from a Blob Storage container using the
 * Container App's passwordless Managed Identity (DefaultAzureCredential), the same credential
 * strategy already used for Azure SQL access in this codebase.
 */
@Component
@ConditionalOnProperty(name = "attachment.storage.provider", havingValue = "azure")
public class AzureBlobAttachmentStorageService implements AttachmentStorageService {

    @Value("${attachment.storage.azure.account-url:}")
    private String accountUrl;

    @Value("${attachment.storage.azure.container-name:attachments}")
    private String containerName;

    private BlobContainerClient containerClient;

    @PostConstruct
    void init() {
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .endpoint(accountUrl)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
        this.containerClient = serviceClient.getBlobContainerClient(containerName);
    }

    @Override
    public Resource load(String storageLocation) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        containerClient.getBlobClient(storageLocation).downloadStream(outputStream);
        return new ByteArrayResource(outputStream.toByteArray());
    }
}
