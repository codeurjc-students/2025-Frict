package com.tfg.ProjectBackend.service;

import com.tfg.ProjectBackend.model.User;
import com.tfg.ProjectBackend.repository.UserRepository;
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
