package x.y.z.backend.mapper;

import org.springframework.stereotype.Component;
import x.y.z.backend.domain.model.Application;
import x.y.z.backend.dto.ApplicationResponse;
import x.y.z.backend.dto.CreateApplicationRequest;
import x.y.z.backend.dto.UpdateApplicationRequest;

/**
 * Mapper to convert between DTOs and domain models.
 * Keeps conversion logic separate from controllers and services.
 */
@Component
public class ApplicationDtoMapper {

    /**
     * Convert CreateApplicationRequest DTO to Application domain model.
     */
    public Application toEntity(CreateApplicationRequest request, String currentUser) {
        Application application = new Application(
            request.getApplicationName(),
            request.getApplicationCode(),
            request.getDescription(),
            request.getStatus(),
            request.getOwnerName(),
            request.getOwnerEmail()
        );
        
        // Set audit fields
        application.setCreatedBy(currentUser);
        application.setUpdatedBy(currentUser);
        
        return application;
    }

    /**
     * Convert UpdateApplicationRequest DTO to Application domain model.
     */
    public Application toEntity(Long id, UpdateApplicationRequest request, String currentUser) {
        Application application = new Application(
            request.getApplicationName(),
            request.getApplicationCode(),
            request.getDescription(),
            request.getStatus(),
            request.getOwnerName(),
            request.getOwnerEmail()
        );
        
        application.setId(id);
        application.setUpdatedBy(currentUser);
        
        return application;
    }

    /**
     * Convert Application domain model to ApplicationResponse DTO.
     */
    public ApplicationResponse toDto(Application application) {
        return new ApplicationResponse(
            application.getId(),
            application.getApplicationName(),
            application.getApplicationCode(),
            application.getDescription(),
            application.getStatus(),
            application.getOwnerName(),
            application.getOwnerEmail(),
            application.getCreatedAt(),
            application.getCreatedBy(),
            application.getUpdatedAt(),
            application.getUpdatedBy()
        );
    }
}
