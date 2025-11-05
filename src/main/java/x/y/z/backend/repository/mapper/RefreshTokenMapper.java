package x.y.z.backend.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import x.y.z.backend.domain.model.RefreshToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * MyBatis Mapper interface for RefreshToken entity.
 * Queries are defined in RefreshTokenMapper.xml
 */
@Mapper
@Repository
public interface RefreshTokenMapper {

    /**
     * Insert a new refresh token record
     */
    int insert(RefreshToken refreshToken);

    /**
     * Find refresh token by ID
     */
    RefreshToken findById(@Param("id") UUID id);

    /**
     * Find refresh token by token hash
     */
    RefreshToken findByTokenHash(@Param("tokenHash") String tokenHash);

    /**
     * Get all active (non-revoked, non-expired) refresh tokens for a user
     */
    List<RefreshToken> findActiveByUserId(@Param("userId") UUID userId);

    /**
     * Get all refresh tokens for a user (including revoked and expired)
     */
    List<RefreshToken> findByUserId(@Param("userId") UUID userId);

    /**
     * Revoke a specific refresh token
     */
    int revoke(@Param("id") UUID id, @Param("revokedAt") LocalDateTime revokedAt);

    /**
     * Revoke all refresh tokens for a user
     */
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("revokedAt") LocalDateTime revokedAt);

    /**
     * Delete expired tokens (cleanup job)
     */
    int deleteExpired(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Delete a refresh token by ID
     */
    int deleteById(@Param("id") UUID id);
}
