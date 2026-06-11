package com.DSM.Platform.follow;

import com.DSM.Platform.user.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    long countByFollowingId(UUID followingId);

    long countByFollowerId(UUID followerId);

    @Query("""
            select follow.follower from Follow follow
            where follow.following.id = :userId
              and follow.follower.status = com.DSM.Platform.user.UserStatus.ACTIVE
            """)
    Page<User> findFollowersOf(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            select follow.following from Follow follow
            where follow.follower.id = :userId
              and follow.following.status = com.DSM.Platform.user.UserStatus.ACTIVE
            """)
    Page<User> findFollowingOf(@Param("userId") UUID userId, Pageable pageable);
}
