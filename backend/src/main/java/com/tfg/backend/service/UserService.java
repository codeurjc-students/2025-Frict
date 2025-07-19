package com.tfg.backend.service;

import com.tfg.backend.model.User;
import com.tfg.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository repository;

    public List<User> findAll() { return repository.findAll(); }

    public User save(User u) {
        return repository.save(u);
    }

    public Optional<User> findByEmail(String userEmail) {
        return repository.findByEmail(userEmail);
    }
}
