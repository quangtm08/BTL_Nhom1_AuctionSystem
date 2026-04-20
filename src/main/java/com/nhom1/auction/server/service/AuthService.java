package com.nhom1.auction.server.service;

import com.nhom1.auction.common.entity.User;
import com.nhom1.auction.common.enums.UserRole;
import com.nhom1.auction.common.exception.AuthenticationException;
import com.nhom1.auction.common.exception.UserAlreadyExistsException;
import com.nhom1.auction.server.repository.UserRepository;

public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public User login(String identifier, String password) throws AuthenticationException {
        return userRepository.findByIdentifier(identifier)
            .filter(user -> user.getPassword().equals(password))
            .orElseThrow(() -> new AuthenticationException("Wrong email/username or password"));
    }

    public User register(String username, String email, String password) throws UserAlreadyExistsException {
        if (userRepository.existsByEmail(email)){
            throw new UserAlreadyExistsException("Email already exists");
        }
        if (userRepository.existsByUsername(username)){
            throw new UserAlreadyExistsException("Username already exists");
        }

        User newUser = new User(username, email, password, UserRole.USER);
        userRepository.save(newUser);

        return newUser;
    }
}
}