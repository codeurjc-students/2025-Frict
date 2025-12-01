package com.tfg.backend.controller;

import com.tfg.backend.DTO.UserLoginDTO;
import com.tfg.backend.model.ImageInfo;
import com.tfg.backend.model.User;
import com.tfg.backend.service.StorageService;
import com.tfg.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Blob;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserRestController {
	
	@Autowired
	private UserService userService;

    @Autowired
    private StorageService storageService;

	@GetMapping("/me")
	public ResponseEntity<UserLoginDTO> me(HttpServletRequest request) {
        Optional<UserLoginDTO> loginInfoOptional = userService.getLoginInfo(request);
		if(loginInfoOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
		}
        return ResponseEntity.ok(loginInfoOptional.get());
	}

    @PostMapping("/image/{id}")
    public ResponseEntity<User> uploadUserAvatar(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        User user = userService.findById(id).orElseThrow();

        // Clean previous image
        if (user.getUserImage() != null) {
            storageService.deleteFile(user.getUserImage().getS3Key());
        }

        // Upload
        Map<String, String> res = storageService.uploadFile(file, "users");

        // Create ImageInfo object (not an entity)
        ImageInfo avatarInfo = new ImageInfo(
                res.get("url"),
                res.get("key"),
                file.getOriginalFilename()
        );

        // Add to user
        user.setUserImage(avatarInfo);

        return ResponseEntity.ok(userService.save(user));
    }

    @DeleteMapping("/{id}/avatar")
    public ResponseEntity<User> deleteAvatar(@PathVariable Long id) {
        User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Check if there is something to delete
        if (user.getUserImage() != null) {
            // Delete from MinIO
            storageService.deleteFile(user.getUserImage().getS3Key());

            // Unlink (orphanRemoval deletes it from DB)
            user.setUserImage(null);

            // Save changes
            return ResponseEntity.ok(userService.save(user));
        }

        return ResponseEntity.ok(user); // Return original user if did not have image
    }
}
