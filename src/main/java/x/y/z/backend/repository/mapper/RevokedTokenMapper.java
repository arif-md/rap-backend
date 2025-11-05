package x.y.z.backend.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import x.y.z.backend.domain.model.RevokedToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * MyBatis Mapper interface for RevokedToken entity.
 * Queries are defined in RevokedTokenMapper.xml
 */
@Mapper
@Repository
public interface RevokedTokenMapper {

    /**
     * Insert a new revoked token record
     */
    int insert(RevokedToken revokedToken);

    /**
     * Find revoked token by ID
     */
    RevokedToken findById(@Param("id") UUID id);

    /**
     * Find revoked token by JTI (JWT ID)
     */
    RevokedToken findByJti(@Param("jti") String jti);

    /**
     * Check if a token is revoked
     */
    boolean isRevoked(@Param("jti") String jti);

    /**
     * Get all revoked tokens for a user
     */
    List<RevokedToken> findByUserId(@Param("userId") UUID userId);

    /**
     * Delete expired revoked tokens (cleanup job)
     */
    int deleteExpired(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Delete a revoked token by ID
     */
    int deleteById(@Param("id") UUID id);
}
