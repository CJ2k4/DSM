package com.DSM.Platform.like;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, UUID> {

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    Optional<Like> findByPostIdAndUserId(UUID postId, UUID userId);

    long countByPostId(UUID postId);
}
