package com.loanpro.security;

import com.loanpro.common.exception.ResourceNotFoundException;
import com.loanpro.modules.identity.domain.User;
import com.loanpro.modules.identity.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User require(UserPrincipal principal) {
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
