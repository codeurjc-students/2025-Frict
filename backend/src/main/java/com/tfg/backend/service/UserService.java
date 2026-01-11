package com.tfg.backend.service;

import com.tfg.backend.dto.UserLoginDTO;
import com.tfg.backend.dto.UserSignupDTO;
import com.tfg.backend.model.Product;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.hamcrest.Condition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

	@Autowired
	private UserRepository userRepository;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Page<User> findAll(Pageable pageInfo) {
        return userRepository.findAll(pageInfo);
    }

    public User save(User u) {
        return userRepository.save(u);
    }

    public void delete(User u){
        userRepository.delete(u);
    }

    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean existsByUsername(String username) { return userRepository.existsByUsername(username); }

    public boolean existsByEmail(String email) { return userRepository.existsByEmail(email); }

	public Optional<UserLoginDTO> getLoginInfo(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if(principal == null) {
            return Optional.empty();
        }
        return Optional.of(new UserLoginDTO(userRepository.findByUsername(principal.getName()).orElseThrow()));
	}

    public Optional<User> getLoggedUser(HttpServletRequest request) {
        Optional<UserLoginDTO> loginInfo = this.getLoginInfo(request);
        if(loginInfo.isEmpty()){
            return Optional.empty();
        }
        return userRepository.findById(loginInfo.get().getId());
    }

    public User registerUser(UserSignupDTO dto) {
        User newUser = new User(dto.getName(), dto.getUsername(), dto.getEmail(), passwordEncoder.encode(dto.getPassword()), "USER");
        return this.save(newUser);
    }

    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean isUsernameTaken(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean isBannedByUsername(String username) {
        return userRepository.existsByUsernameAndIsBannedTrue(username);
    }

    public boolean isDeletedByUsername(String username) {
        return userRepository.existsByUsernameAndIsDeletedTrue(username);
    }

    public boolean isBannedByEmail(String email) {
        return userRepository.existsByEmailAndIsBannedTrue(email);
    }

    public boolean isDeletedByEmail(String email) {
        return userRepository.existsByEmailAndIsDeletedTrue(email);
    }
}
