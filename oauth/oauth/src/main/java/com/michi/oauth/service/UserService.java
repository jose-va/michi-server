package com.michi.oauth.service;

import com.michi.oauth.entity.Role;
import com.michi.oauth.entity.User;
import com.michi.oauth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserService extends OidcUserService {

    private final UserRepository userRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getAttribute("email");
        String name  = oidcUser.getAttribute("name");
        String picture  = oidcUser.getAttribute("picture");
        String sub = oidcUser.getAttribute("sub");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(email)
                                .name(name)
                                .picture(picture)
                                .role(Role.ROLE_USER)
                                .googleId(sub)
                                .build()
                ));

        return new DefaultOidcUser(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().toString())),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo()
        );
    }
}