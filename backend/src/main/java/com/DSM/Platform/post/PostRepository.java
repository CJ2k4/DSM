package com.DSM.Platform.post;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, UUID> {

    Page<Post> findByAuthorIdOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    @Query("""
            select post from Post post
            where post.author.status = com.DSM.Platform.user.UserStatus.ACTIVE
              and (
                post.author.id = :viewerId
                or post.author.id in (
                    select follow.following.id from Follow follow
                    where follow.follower.id = :viewerId
                )
              )
            order by post.createdAt desc
            """)
    Page<Post> findFeed(@Param("viewerId") UUID viewerId, Pageable pageable);
}
