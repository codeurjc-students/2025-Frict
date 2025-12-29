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
import com.tfg.backend.utils.GlobalDefaults;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

    //Needs the id as path variable to allow changing the profile image when the user is firstly created (registered)
    @PostMapping("/image/{id}")
    public ResponseEntity<UserDTO> uploadUserImage(@PathVariable Long id, @RequestParam("image") MultipartFile image) throws IOException {
        Optional<User> userOptional = userService.findById(id);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(404).build(); //User not found in DB (not registered)
        }
        User loggedUser = userOptional.get();

        // Clean previous image (if exists and it is not the default user image)
        if (loggedUser.getUserImage() != null && !loggedUser.getUserImage().getS3Key().equals(GlobalDefaults.USER_IMAGE.getS3Key())) {
            storageService.deleteFile(loggedUser.getUserImage().getS3Key());
        }

        // Upload
        Map<String, String> res = storageService.uploadFile(image, "users");

        // Create ImageInfo object (not an entity)
        ImageInfo userImageInfo = new ImageInfo(
                res.get("url"),
                res.get("key"),
                image.getOriginalFilename()
        );

        // Add to user
        loggedUser.setUserImage(userImageInfo);

        return ResponseEntity.ok(new UserDTO(userService.save(loggedUser)));
    }

    //Option 1: Delete User entities (statistics information will be lost, reviews and orders need to be reassigned to a generic anon user, which affects data possession)
    //Option 2 (active): Anonymize / Clear sensible user data (delete address and cards, anonymize the rest of sensible information, mark account as deleted (non-accessible))
    @DeleteMapping
    public ResponseEntity<UserDTO> deleteLoggedUser(HttpServletRequest request) {
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        //Erase and anonymize all user data but the name (in order to identify review creators)
        String uniqueUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        loggedUser.setName("Usuario eliminado " + uniqueUuid);
        loggedUser.setUsername("deleteduser_" + uniqueUuid);
        loggedUser.setEncodedPassword(""); // Empty password, as it may contain sensible data
        loggedUser.setOtpCode(null);
        loggedUser.setOtpExpiration(null);
        loggedUser.getRoles().clear(); // Unauthorized to access secured pages
        loggedUser.setEmail("deleteduser_" + uniqueUuid + "@frictapp.com");
        loggedUser.setPhone(null);
        loggedUser.getAddresses().clear();
        loggedUser.getCards().clear();

        if (loggedUser.getUserImage() != null && !loggedUser.getUserImage().getS3Key().equals(GlobalDefaults.USER_IMAGE.getS3Key())) {
            storageService.deleteFile(loggedUser.getUserImage().getS3Key());
        }
        loggedUser.setUserImage(GlobalDefaults.USER_IMAGE);

        loggedUser.setDeleted(true); //Mark as deleted user
        loggedUser.getAllOrderItems().removeIf(item -> item.getOrder() == null); //Clear cart items

        User savedUser = userService.save(loggedUser);
        return ResponseEntity.ok(new UserDTO(savedUser));
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

    @PutMapping("/data")
    public ResponseEntity<UserDTO> updateLoggedUserData(HttpServletRequest request, @RequestBody UserDTO userDTO){
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        //Check if the username exists if it is not the same (lazy check)
        if(!userDTO.getUsername().equals(loggedUser.getUsername()) && userService.existsByUsername(userDTO.getUsername())){
            return ResponseEntity.status(403).build(); //Forbidden, as username must be unique
        }

        //Edit user data
        loggedUser.setName(userDTO.getName());
        loggedUser.setUsername(userDTO.getUsername());
        loggedUser.setEmail(userDTO.getEmail());
        loggedUser.setPhone(userDTO.getPhone());

        User savedUser = userService.save(loggedUser);
        return ResponseEntity.ok(new UserDTO(savedUser));
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

    @PutMapping("/addresses")
    public ResponseEntity<UserDTO> editAddress(HttpServletRequest request, @RequestBody AddressDTO addressDTO){
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        Optional<Address> addressOptional = loggedUser.getAddresses().stream().filter(address -> address.getId().equals(addressDTO.getId())).findFirst();
        if(addressOptional.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        Address address = addressOptional.get();

        address.setAlias(addressDTO.getAlias());
        address.setStreet(addressDTO.getStreet());
        address.setNumber(addressDTO.getNumber());
        address.setFloor(addressDTO.getFloor());
        address.setPostalCode(addressDTO.getPostalCode());
        address.setCity(addressDTO.getCity());
        address.setCountry(addressDTO.getCountry());

        User savedUser = this.userService.save(loggedUser);
        return ResponseEntity.ok(new UserDTO(savedUser));
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<UserDTO> deleteAddress(HttpServletRequest request, @PathVariable Long id){
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        boolean removed = loggedUser.getAddresses().removeIf(address -> address.getId().equals(id));
        if(!removed){
            return ResponseEntity.notFound().build();
        }

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

    @PutMapping("/cards")
    public ResponseEntity<UserDTO> editPaymentCard(HttpServletRequest request, @RequestBody PaymentCardDTO paymentCardDTO){
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        Optional<PaymentCard> cardOptional = loggedUser.getCards().stream().filter(card -> card.getId().equals(paymentCardDTO.getId())).findFirst();
        if(cardOptional.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        PaymentCard card = cardOptional.get();

        card.setAlias(paymentCardDTO.getAlias());
        card.setCardOwnerName(paymentCardDTO.getCardOwnerName());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
        card.setDueDate(YearMonth.parse(paymentCardDTO.getDueDate(), formatter));

        User savedUser = this.userService.save(loggedUser);
        return ResponseEntity.ok(new UserDTO(savedUser));
    }

    @DeleteMapping("/cards/{id}")
    public ResponseEntity<UserDTO> deletePaymentCard(HttpServletRequest request, @PathVariable Long id){
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        boolean removed = loggedUser.getCards().removeIf(card -> card.getId().equals(id));
        if(!removed){
            return ResponseEntity.notFound().build();
        }

        User savedUser = userService.save(loggedUser);
        return ResponseEntity.ok(new UserDTO(savedUser));
    }
}
