package com.tfg.backend.service;

import com.tfg.backend.DTO.UserLoginDTO;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

	@Autowired
	private UserRepository userRepository;

    public List<User> findAll() { return userRepository.findAll(); }

    public User save(User u) {
        return userRepository.save(u);
    }

    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> findByEmail(String userEmail) {
        return userRepository.findByEmail(userEmail);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

	public UserLoginDTO getLoggedUserInfo(String name) {
		return new UserLoginDTO(userRepository.findByUsername(name).orElseThrow());
	}
}
