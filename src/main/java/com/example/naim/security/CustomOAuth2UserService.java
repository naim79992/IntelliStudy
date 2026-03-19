package com.example.naim.security;

import com.example.naim.model.User;
import com.example.naim.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        return processOAuth2User(userRequest, oAuth2User);
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        String provider = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        String providerId = oAuth2User.getName();

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            // Update existing user info
            user.setFullName(name);
            user.setPictureUrl(picture);
        } else {
            // Register new user
            user = User.builder()
                    .email(email)
                    .fullName(name)
                    .pictureUrl(picture)
                    .provider(provider)
                    .providerId(providerId)
                    .build();
        }
        userRepository.save(user);

        return new UserPrincipal(user, oAuth2User.getAttributes());
    }
}
