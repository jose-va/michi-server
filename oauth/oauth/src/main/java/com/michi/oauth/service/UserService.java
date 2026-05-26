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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        String id = oidcUser.getAttribute("sub");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .id(id)
                                .email(email)
                                .name(name)
                                .picture(picture)
                                .role(Role.ROLE_USER)
                                .favoriteProducts(new ArrayList<>())
                                .build()
                ));

        return new DefaultOidcUser(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().toString())),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo()
        );
    }

    public List<String> favoriteProducts(String id) {
        return userRepository.findById(id)
                .map(User::getFavoriteProducts)
                .orElse(new ArrayList<>());
    }

    public void markFavorite(String googleId, String productId) {
        User user = userRepository.findById(googleId)
                .orElseThrow(() -> new RuntimeException("No se ha encontrado al usuario..."));

        if (!user.getFavoriteProducts().contains(productId)) {
            user.getFavoriteProducts().add(productId);
            userRepository.save(user);
        }
    }

    public void unmarkFavorite(String id, String productId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se ha encontrado al usuario..."));

        user.getFavoriteProducts().remove(productId);
        userRepository.save(user);
    }
}