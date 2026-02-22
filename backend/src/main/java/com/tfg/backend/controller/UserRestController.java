package com.tfg.backend.controller;

import com.tfg.backend.dto.*;
import com.tfg.backend.model.*;
import com.tfg.backend.service.ShopService;
import com.tfg.backend.service.StorageService;
import com.tfg.backend.service.UserService;
import com.tfg.backend.utils.GlobalDefaults;
import com.tfg.backend.utils.PageFormatter;
import com.tfg.backend.utils.StatDataDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "System users data management")
public class UserRestController {
	
	@Autowired
	private UserService userService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private StorageService storageService;


    @Operation(summary = "(All) Get current session information")
	@GetMapping("/session")
	public ResponseEntity<UserLoginDTO> getSessionInfo(HttpServletRequest request) {
        Optional<UserLoginDTO> loginInfoOptional = userService.getLoginInfo(request);
		if(loginInfoOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "No user logged in this session.");
		}
        return ResponseEntity.ok(loginInfoOptional.get());
	}


    @Operation(summary = "(Users) Set selected shop")
    @PostMapping("/shop")
    public ResponseEntity<Boolean> setSelectedShop(HttpServletRequest request, @RequestBody Map<String, Long> body) {

        User loggedUser = findLoggedUserHelper(request);
        Long shopId = body.get("shopId");

        if (shopId == null){
            loggedUser.setSelectedShop(null);
        }
        else {
            Optional<Shop> shopOptional = shopService.findById(shopId);
            if(shopOptional.isEmpty()){
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop with ID " + shopId + " does not exist.");
            }
            Shop shop = shopOptional.get();
            loggedUser.setSelectedShop(shop);
        }
        userService.save(loggedUser);
        return ResponseEntity.ok(true);
    }


    @Operation(summary = "(All) Get logged user information")
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getLoggedUser(HttpServletRequest request) {
        User loggedUser = findLoggedUserHelper(request);
        return ResponseEntity.ok(new UserDTO(loggedUser));
    }


    @Operation(summary = "(Admin) Get all users by role")
    @GetMapping("/role/")
    public ResponseEntity<List<UserDTO>> getAllUsersByRole(@RequestParam String role) {
        List<User> allUsers = userService.findAllByRole(role);
        List<UserDTO> dtos = new ArrayList<>();
        for (User u : allUsers) {
            dtos.add(new UserDTO(u));
        }
        return ResponseEntity.ok(dtos);
    }


    @Operation(summary = "(Admin) Get all users information")
    @GetMapping("/")
    public ResponseEntity<PageResponse<UserDTO>> getAllUsers(Pageable pageable) {
        Page<User> allUsers = userService.findAll(pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(allUsers, UserDTO::new));
    }


    //Needs the id as path variable to allow changing the profile image when the user is firstly created (registered)
    @Operation(summary = "(User) Update remote user image")
    @PostMapping("/image/{id}")
    public ResponseEntity<UserDTO> uploadUserImage(@PathVariable Long id, @RequestParam("image") MultipartFile image) throws IOException {
        Optional<User> userOptional = userService.findById(id);
        if(userOptional.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User with ID " + id + " does not exist.");
        }
        User loggedUser = userOptional.get();

        // Clean previous image (if exists and it is not the default user image)
        if (loggedUser.getUserImage() != null && !loggedUser.getUserImage().equals(GlobalDefaults.USER_IMAGE)) {
            storageService.deleteFile(loggedUser.getUserImage().getS3Key());
        }

        // Upload
        if (image.isEmpty()){
            Map<String, String> res = storageService.uploadFile(image, "users");
            ImageInfo userImageInfo = new ImageInfo(
                    res.get("url"),
                    res.get("key"),
                    image.getOriginalFilename()
            );
            loggedUser.setUserImage(userImageInfo);
        }
        else {
            loggedUser.setUserImage(GlobalDefaults.USER_IMAGE);
        }

        return ResponseEntity.ok(new UserDTO(userService.save(loggedUser)));
    }


    //Option 1: Delete User entities (statistics information will be lost, reviews and orders need to be reassigned to a generic anon user, which affects data possession)
    //Option 2 (active): Anonymize / Clear sensible user data (delete address and cards, anonymize the rest of sensible information, mark account as deleted (non-accessible))
    @Operation(summary = "(User) Anonymize logged user account")
    @DeleteMapping
    public ResponseEntity<UserDTO> anonymizeLoggedUser(HttpServletRequest request) {
        User loggedUser = findLoggedUserHelper(request);
        User savedUser = userService.save(userService.anonymizeUser(loggedUser));
        return ResponseEntity.ok(new UserDTO(savedUser));
    }


    @Operation(summary = "(User) Delete logged user image")
    @DeleteMapping("/image")
    public ResponseEntity<UserDTO> deleteUserImage(HttpServletRequest request) {
        User loggedUser = findLoggedUserHelper(request);

        // Check if there is something to delete
        if (!loggedUser.getUserImage().equals(GlobalDefaults.USER_IMAGE)) {
            storageService.deleteFile(loggedUser.getUserImage().getS3Key());
            loggedUser.setUserImage(GlobalDefaults.USER_IMAGE);
            return ResponseEntity.ok(new UserDTO(userService.save(loggedUser)));
        }
        return ResponseEntity.ok(new UserDTO(loggedUser));
    }


    @Operation(summary = "(User) Update logged user data")
    @PutMapping("/data")
    public ResponseEntity<UserDTO> updateLoggedUserData(HttpServletRequest request, @RequestBody UserDTO userDTO){
        User loggedUser = findLoggedUserHelper(request);

        //Check if the username exists if it is not the same (lazy check)
        if(!userDTO.getUsername().equals(loggedUser.getUsername()) && userService.existsByUsername(userDTO.getUsername())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Username is already taken.");
        }

        //Edit user data
        loggedUser.setName(userDTO.getName());
        loggedUser.setUsername(userDTO.getUsername());
        loggedUser.setEmail(userDTO.getEmail());
        loggedUser.setPhone(userDTO.getPhone());

        User savedUser = userService.save(loggedUser);
        return ResponseEntity.ok(new UserDTO(savedUser));
    }


    @Operation(summary = "(User) Create logged user address")
    @PostMapping("/addresses")
    public ResponseEntity<UserDTO> createAddress(HttpServletRequest request, @RequestBody AddressDTO addressDTO){
        User loggedUser = findLoggedUserHelper(request);

        Address address = new Address(addressDTO.getAlias(), addressDTO.getStreet(), addressDTO.getNumber(), addressDTO.getFloor(), addressDTO.getPostalCode(), addressDTO.getCity(), addressDTO.getCountry());
        loggedUser.getAddresses().add(address);
        User savedUser = userService.save(loggedUser);

        return ResponseEntity.ok(new UserDTO(savedUser));
    }


    @Operation(summary = "(User) Edit logged user address")
    @PutMapping("/addresses")
    public ResponseEntity<UserDTO> editAddress(HttpServletRequest request, @RequestBody AddressDTO addressDTO){
        User loggedUser = findLoggedUserHelper(request);

        Optional<Address> addressOptional = loggedUser.getAddresses().stream().filter(address -> address.getId().equals(addressDTO.getId())).findFirst();
        if(addressOptional.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Address with ID " + addressDTO.getId() + " does not exist.");
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


    @Operation(summary = "(User) Delete logged user address by ID")
    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<UserDTO> deleteAddress(HttpServletRequest request, @PathVariable Long id){
        User loggedUser = findLoggedUserHelper(request);

        boolean removed = loggedUser.getAddresses().removeIf(address -> address.getId().equals(id));
        if(!removed){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Address with ID " + id + " not deleted as it does not exist.");
        }

        User savedUser = userService.save(loggedUser);
        return ResponseEntity.ok(new UserDTO(savedUser));
    }


    @Operation(summary = "(User) Create logged user card")
    @PostMapping("/cards")
    public ResponseEntity<UserDTO> createPaymentCard(HttpServletRequest request, @RequestBody PaymentCardDTO cardDTO){
        User loggedUser = findLoggedUserHelper(request);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
        PaymentCard card = new PaymentCard(cardDTO.getAlias(), cardDTO.getCardOwnerName(), cardDTO.getNumber(), cardDTO.getCvv(), YearMonth.parse(cardDTO.getDueDate(), formatter));
        loggedUser.getCards().add(card);
        User savedUser = userService.save(loggedUser);

        return ResponseEntity.ok(new UserDTO(savedUser));
    }


    @Operation(summary = "(User) Update logged user card")
    @PutMapping("/cards")
    public ResponseEntity<UserDTO> editPaymentCard(HttpServletRequest request, @RequestBody PaymentCardDTO paymentCardDTO){
        User loggedUser = findLoggedUserHelper(request);

        Optional<PaymentCard> cardOptional = loggedUser.getCards().stream().filter(card -> card.getId().equals(paymentCardDTO.getId())).findFirst();
        if(cardOptional.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Card with ID " + paymentCardDTO.getId() + " does not exist.");
        }
        PaymentCard card = cardOptional.get();

        card.setAlias(paymentCardDTO.getAlias());
        card.setCardOwnerName(paymentCardDTO.getCardOwnerName());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
        card.setDueDate(YearMonth.parse(paymentCardDTO.getDueDate(), formatter));

        User savedUser = this.userService.save(loggedUser);
        return ResponseEntity.ok(new UserDTO(savedUser));
    }


    @Operation(summary = "(User) Delete logged user card by ID")
    @DeleteMapping("/cards/{id}")
    public ResponseEntity<UserDTO> deletePaymentCard(HttpServletRequest request, @PathVariable Long id){
        User loggedUser = findLoggedUserHelper(request);

        boolean removed = loggedUser.getCards().removeIf(card -> card.getId().equals(id));
        if(!removed){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Address with ID " + id + " not deleted as it does not exist.");
        }

        User savedUser = userService.save(loggedUser);
        return ResponseEntity.ok(new UserDTO(savedUser));
    }

    private User findLoggedUserHelper(HttpServletRequest request) {
        return this.userService.getLoggedUser(request)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You must be logged to perform this operation."));
    }


    //Reactive endpoints
    @Operation(summary = "(All) Check if a username is taken")
    @GetMapping("/username")
    public ResponseEntity<Boolean> checkUsername(@RequestParam String username) {
        return ResponseEntity.ok(userService.isUsernameTaken(username));
    }

    @Operation(summary = "(All) Check if an email is taken")
    @GetMapping("/email")
    public ResponseEntity<Boolean> checkEmail(@RequestParam String email) {
        return ResponseEntity.ok(userService.isEmailTaken(email));
    }


    //Ban / Anon / Delete endpoints
    @Operation(summary = "(Admin) Toggle all users ban (except admin)")
    @PutMapping("/ban/")
    public ResponseEntity<Boolean> toggleAllUsersBan(@RequestBody boolean banState){
        List<User> allUsers = this.userService.findAll();
        for (User u : allUsers) {
            if (!u.getRoles().contains("ADMIN")){
                u.setBanned(banState);
            }
        }
        userService.saveAll(allUsers);
        return ResponseEntity.ok(true);
    }


    @Operation(summary = "(Admin) Unban user by ID")
    @PutMapping("/ban/{id}")
    public ResponseEntity<UserDTO> toggleUserBanById(@PathVariable Long id, @RequestBody boolean banState){
        Optional<User> userOptional = userService.findById(id);
        if(userOptional.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User with ID " + id + " does not exist.");
        }
        User user = userOptional.get();
        user.setBanned(banState);
        User savedUser = userService.save(user);
        return ResponseEntity.ok(new UserDTO(savedUser));
    }


    @Operation(summary = "(Admin) Anonymize all users (except admin)")
    @PutMapping("/anon/")
    public ResponseEntity<Boolean> anonAllUsers(){
        List<User> allUsers = this.userService.findAll();
        for (User u : allUsers) {
            if (!u.getRoles().contains("ADMIN")){
                User anonymizedUser = this.userService.anonymizeUser(u);
                userService.save(anonymizedUser);
            }
        }
        return ResponseEntity.ok(true);
    }


    @Operation(summary = "(Admin) Anonymize user by ID")
    @PutMapping("/anon/{id}")
    public ResponseEntity<UserDTO> anonUserById(@PathVariable Long id){
        Optional<User> userOptional = userService.findById(id);
        if(userOptional.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User with ID " + id + " does not exist.");
        }
        User savedUser = userService.save(userService.anonymizeUser(userOptional.get()));
        return ResponseEntity.ok(new UserDTO(savedUser));
    }


    @Operation(summary = "(Admin) Delete all users (except admin)")
    @DeleteMapping("/")
    public ResponseEntity<Boolean> deleteAllUsers(){
        List<User> allUsers = this.userService.findAll();
        for (User u : allUsers) {
            if (!u.getRoles().contains("ADMIN")){
                userService.delete(u);
            }
        }
        return ResponseEntity.ok(true);
    }


    @Operation(summary = "(Admin) Delete user by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteUserById(@PathVariable Long id){
        Optional<User> userOptional = userService.findById(id);
        if(userOptional.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User with ID " + id + " does not exist.");
        }
        userService.delete(userOptional.get());
        return ResponseEntity.ok(true);
    }


    @Operation(summary = "(Admin) Get users stats")
    @GetMapping("/stats")
    public ResponseEntity<List<StatDataDTO>> getUsersStats(){
        List<StatDataDTO> stats = new ArrayList<>();
        stats.add(new StatDataDTO("Totales", userService.count()));
        stats.add(new StatDataDTO("Baneados", userService.countByIsBannedTrue()));
        stats.add(new StatDataDTO("Anonimizados", userService.countByIsDeletedTrue()));
        Long internalAccounts = userService.countByRole("ADMIN") + userService.countByRole("MANAGER") + userService.countByRole("DRIVER");
        stats.add(new StatDataDTO("Cuentas Internas", internalAccounts));
        return ResponseEntity.ok(stats);
    }
}
