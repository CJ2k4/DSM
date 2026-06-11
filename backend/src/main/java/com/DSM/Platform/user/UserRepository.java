package com.DSM.Platform.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    @Query("""
            select user from User user
            where user.status = com.DSM.Platform.user.UserStatus.ACTIVE
              and (
                lower(user.username) like lower(concat('%', :query, '%'))
                or lower(user.displayName) like lower(concat('%', :query, '%'))
              )
            """)
    Page<User> searchActiveUsers(@Param("query") String query, Pageable pageable);
}
