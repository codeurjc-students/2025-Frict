package com.tfg.backend.controller;

import com.tfg.backend.DTO.UserLoginDTO;
import com.tfg.backend.model.User;
import com.tfg.backend.service.UserService;
import com.tfg.backend.utils.ImageUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Blob;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserRestController {
	
	@Autowired
	private UserService userService;

	@GetMapping("/me")
	public ResponseEntity<UserLoginDTO> me(HttpServletRequest request) {
        Optional<UserLoginDTO> loginInfoOptional = userService.getLoginInfo(request);
		if(loginInfoOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
		}
        return ResponseEntity.ok(loginInfoOptional.get());
	}

    @GetMapping("/image/{id}")
    public ResponseEntity<byte[]> showUserImage(@PathVariable long id) {
        Optional<User> userOptional = userService.findById(id);
        if (!userOptional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOptional.get();
        return ImageUtils.serveImage(user.getProfileImage(), false);
    }

    @GetMapping("/thumbnail/{id}")
    public ResponseEntity<byte[]> showUserThumbnail(@PathVariable long id) {
        Optional<User> userOptional = userService.findById(id);
        if (!userOptional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOptional.get();
        return ImageUtils.serveImage(user.getProfileImage(), true);
    }


    @PutMapping("/image/{id}")
    public ResponseEntity<String> updateUserImage(@PathVariable Long id, @RequestPart("image") MultipartFile image) {
        Optional<User> userOptional = userService.findById(id);
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOptional.get();

        Blob profileImage = ImageUtils.prepareImage(image);
        user.setProfileImage(profileImage);
        userService.save(user);
        return ResponseEntity.ok().build();
    }
}
