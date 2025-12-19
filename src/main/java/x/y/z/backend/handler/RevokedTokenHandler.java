package x.y.z.backend.handler;

import org.springframework.stereotype.Component;
import x.y.z.backend.domain.model.RevokedToken;
import x.y.z.backend.repository.mapper.RevokedTokenMapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RevokedTokenHandler - Handles revoked token data access operations.
 * Pure data access - business logic should be in the Service layer.
 */
@Component
public class RevokedTokenHandler {

    private final RevokedTokenMapper revokedTokenMapper;

    public RevokedTokenHandler(RevokedTokenMapper revokedTokenMapper) {
        this.revokedTokenMapper = revokedTokenMapper;
    }

    public RevokedToken insert(RevokedToken revokedToken) {
        int rowsInserted = revokedTokenMapper.insert(revokedToken);
        if (rowsInserted == 0) {
            throw new RuntimeException("Failed to insert revoked token");
        }
        return revokedToken;
    }

    public RevokedToken findById(Long id) {
        return revokedTokenMapper.findById(id);
    }

    public RevokedToken findByJti(String jti) {
        return revokedTokenMapper.findByJti(jti);
    }

    public boolean isRevoked(String jti) {
        return revokedTokenMapper.isRevoked(jti);
    }

    public List<RevokedToken> findByUserId(Long userId) {
        return revokedTokenMapper.findByUserId(userId);
    }

    public void deleteExpired(LocalDateTime currentTime) {
        revokedTokenMapper.deleteExpired(currentTime);
    }

    public void deleteById(Long tokenId) {
        revokedTokenMapper.deleteById(tokenId);
    }
}
