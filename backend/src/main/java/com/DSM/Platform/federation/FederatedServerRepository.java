package com.DSM.Platform.federation;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FederatedServerRepository extends JpaRepository<FederatedServer, UUID> {

    Optional<FederatedServer> findByBaseUrl(String baseUrl);

    boolean existsByBaseUrl(String baseUrl);
}
