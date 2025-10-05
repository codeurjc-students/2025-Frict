package com.tfg.backend.controller;

import com.tfg.backend.model.User;
import com.tfg.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserRestController {

    @Autowired
    private UserService userService;

    @PutMapping("/{userId}/photo")
    public ResponseEntity<String> updateUserPhoto(@PathVariable Long userId, @RequestPart("photo") MultipartFile photo) {
        Optional<User> userOptional = userService.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOptional.get();

        try {
            Blob profilePhoto = new SerialBlob(photo.getBytes());
            user.setProfilePhoto(profilePhoto);
            userService.save(user);
            return ResponseEntity.ok().build();
        }
        catch (IOException e) {
            return ResponseEntity.status(400).build();
        }
        catch (SQLException e) {
            return ResponseEntity.status(404).build();
        }
    }
}
