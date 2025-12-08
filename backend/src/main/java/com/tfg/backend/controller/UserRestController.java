package com.tfg.backend.controller;

import com.tfg.backend.DTO.AddressDTO;
import com.tfg.backend.DTO.PaymentCardDTO;
import com.tfg.backend.DTO.UserDTO;
import com.tfg.backend.DTO.UserLoginDTO;
import com.tfg.backend.model.Address;
import com.tfg.backend.model.ImageInfo;
import com.tfg.backend.model.PaymentCard;
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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserRestController {
	
	@Autowired
	private UserService userService;

    @Autowired
    private StorageService storageService;

	@GetMapping("/session")
	public ResponseEntity<UserLoginDTO> getSessionInfo(HttpServletRequest request) {
        Optional<UserLoginDTO> loginInfoOptional = userService.getLoginInfo(request);
		if(loginInfoOptional.isEmpty()) {
            return ResponseEntity.noContent().build();
		}
        return ResponseEntity.ok(loginInfoOptional.get());
	}

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getLoggedUser(HttpServletRequest request) {
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        return ResponseEntity.ok(new UserDTO(loggedUser));
    }

    @PostMapping("/image")
    public ResponseEntity<UserDTO> uploadUserAvatar(HttpServletRequest request, @RequestParam("file") MultipartFile image) throws IOException {
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        // Clean previous image
        if (loggedUser.getUserImage() != null) {
            storageService.deleteFile(loggedUser.getUserImage().getS3Key());
        }

        // Upload
        Map<String, String> res = storageService.uploadFile(image, "users");

        // Create ImageInfo object (not an entity)
        ImageInfo avatarInfo = new ImageInfo(
                res.get("url"),
                res.get("key"),
                image.getOriginalFilename()
        );

        // Add to user
        loggedUser.setUserImage(avatarInfo);

        return ResponseEntity.ok(new UserDTO(userService.save(loggedUser)));
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<UserDTO> deleteAvatar(HttpServletRequest request) {
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        // Check if there is something to delete
        if (loggedUser.getUserImage() != null) {
            // Delete from MinIO
            storageService.deleteFile(loggedUser.getUserImage().getS3Key());

            // Unlink (orphanRemoval deletes it from DB)
            loggedUser.setUserImage(null);

            // Save changes
            return ResponseEntity.ok(new UserDTO(userService.save(loggedUser)));
        }

        return ResponseEntity.ok(new UserDTO(loggedUser)); // Return original user if did not have image
    }


    @PostMapping("/addresses")
    public ResponseEntity<UserDTO> createAddress(HttpServletRequest request, @RequestBody AddressDTO addressDTO){
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        Address address = new Address(addressDTO.getAlias(), addressDTO.getStreet(), addressDTO.getNumber(), addressDTO.getFloor(), addressDTO.getPostalCode(), addressDTO.getCity(), addressDTO.getCountry());
        loggedUser.getAddresses().add(address);
        User savedUser = userService.save(loggedUser);

        return ResponseEntity.ok(new UserDTO(savedUser));
    }


    @PostMapping("/cards")
    public ResponseEntity<UserDTO> createPaymentCard(HttpServletRequest request, @RequestBody PaymentCardDTO cardDTO){
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
        PaymentCard card = new PaymentCard(cardDTO.getAlias(), cardDTO.getCardOwnerName(), cardDTO.getNumber(), cardDTO.getCvv(), YearMonth.parse(cardDTO.getDueDate(), formatter));
        loggedUser.getCards().add(card);
        User savedUser = userService.save(loggedUser);

        return ResponseEntity.ok(new UserDTO(savedUser));
    }
}
