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

    // join fetch so the federation export can map authors outside a transaction
    // (open-in-view is disabled); fetch + Pageable requires an explicit countQuery.
    @Query(
            value = """
                    select post from Post post
                    join fetch post.author
                    where post.author.status = com.DSM.Platform.user.UserStatus.ACTIVE
                    order by post.createdAt desc
                    """,
            countQuery = """
                    select count(post) from Post post
                    where post.author.status = com.DSM.Platform.user.UserStatus.ACTIVE
                    """
    )
    Page<Post> findPublicPosts(Pageable pageable);
}
