package x.y.z.backend.handler;

import org.springframework.stereotype.Component;
import x.y.z.backend.domain.model.RefreshToken;
import x.y.z.backend.repository.mapper.RefreshTokenMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * RefreshTokenHandler - Handles refresh token data access operations.
 * Pure data access - business logic should be in the Service layer.
 */
@Component
public class RefreshTokenHandler {

    private final RefreshTokenMapper refreshTokenMapper;

    public RefreshTokenHandler(RefreshTokenMapper refreshTokenMapper) {
        this.refreshTokenMapper = refreshTokenMapper;
    }

    public RefreshToken insert(RefreshToken refreshToken) {
        int rowsInserted = refreshTokenMapper.insert(refreshToken);
        if (rowsInserted == 0) {
            throw new RuntimeException("Failed to insert refresh token");
        }
        return refreshToken;
    }

    public RefreshToken findById(UUID id) {
        return refreshTokenMapper.findById(id);
    }

    public RefreshToken findByTokenHash(String tokenHash) {
        return refreshTokenMapper.findByTokenHash(tokenHash);
    }

    public List<RefreshToken> findActiveByUserId(UUID userId) {
        return refreshTokenMapper.findActiveByUserId(userId);
    }

    public List<RefreshToken> findByUserId(UUID userId) {
        return refreshTokenMapper.findByUserId(userId);
    }

    public void revoke(UUID tokenId, LocalDateTime revokedAt) {
        refreshTokenMapper.revoke(tokenId, revokedAt);
    }

    public void revokeAllByUserId(UUID userId, LocalDateTime revokedAt) {
        refreshTokenMapper.revokeAllByUserId(userId, revokedAt);
    }

    public void deleteExpired(LocalDateTime currentTime) {
        refreshTokenMapper.deleteExpired(currentTime);
    }

    public void deleteById(UUID tokenId) {
        refreshTokenMapper.deleteById(tokenId);
    }
}
