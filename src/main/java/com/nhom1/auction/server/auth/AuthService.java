package com.nhom1.auction.server.auth;

import com.nhom1.auction.common.entity.User;
import com.nhom1.auction.common.enums.UserRole;
import com.nhom1.auction.common.exception.AuthenticationException;
import com.nhom1.auction.common.exception.UserAlreadyExistsException;

import java.util.List;

/*
 - Execute authentication business logic. Does not know anything about JSON or SQL:
 + AuthHandler already turns JSON to DTOs and use it to call this service
 + This service asks repository to read/write to database
  */
public class AuthService {
    private final UserRepository userRepository;


    public AuthService(UserRepository userRepository){
        this.userRepository = userRepository;
    }
    
    //Return all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    //Take in identifier (email or username) and password. Return User object if success, throw exception if failed
    public User login(String identifier, String password) {
        return userRepository.findByIdentifier(identifier)
            .filter(user -> user.getPassword().equals(password))
            .orElseThrow(() -> new AuthenticationException("Wrong email/username or password"));
    }

    //Take in username, email and password. Return User object if success, throw exception if failed
    public User register(String username, String email, String password) {
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
