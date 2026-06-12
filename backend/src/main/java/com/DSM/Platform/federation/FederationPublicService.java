package com.DSM.Platform.federation;

import com.DSM.Platform.federation.dto.FederationInfoDto;
import com.DSM.Platform.federation.dto.FederationPostDto;
import com.DSM.Platform.federation.dto.FederationUserDto;
import com.DSM.Platform.post.PostRepository;
import com.DSM.Platform.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side of the federation protocol: what this node shares with peers. */
@Service
public class FederationPublicService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FederationProperties properties;

    public FederationPublicService(
            PostRepository postRepository,
            UserRepository userRepository,
            FederationProperties properties
    ) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public Page<FederationPostDto> getPublicPosts(Pageable pageable) {
        return postRepository.findPublicPosts(pageable).map(FederationPostDto::from);
    }

    @Transactional(readOnly = true)
    public Page<FederationUserDto> getPublicUsers(Pageable pageable) {
        return userRepository.findActiveUsers(pageable).map(FederationUserDto::from);
    }

    public FederationInfoDto getInfo() {
        return new FederationInfoDto(
                properties.getServerName(),
                FederationServerService.normalizeBaseUrl(properties.getSelfBaseUrl()),
                "dsm",
                userRepository.count(),
                postRepository.count()
        );
    }
}
