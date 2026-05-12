package com.tfg.backend.controller;

import com.tfg.backend.dto.*;
import com.tfg.backend.model.User;
import com.tfg.backend.service.ShopUserOrchestrator;
import com.tfg.backend.service.ConnectionService;
import com.tfg.backend.service.UserService;
import com.tfg.backend.utils.PageFormatter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "System users data management")
@RequiredArgsConstructor
public class UserRestController {

    private final UserService userService;
    private final ConnectionService connectionService; //Mongo
    private final ShopUserOrchestrator shopUserOrchestrator;


    @Operation(summary = "(All) Get current session information")
    @GetMapping("/session")
    public ResponseEntity<UserLoginDTO> getSessionInfo() {
        UserLoginDTO loginInfoOptional = userService.getLoginInfo();
        return ResponseEntity.ok(loginInfoOptional);
    }


    @Operation(summary = "(Users) Set selected shop")
    @PutMapping("/shop")
    public ResponseEntity<Boolean> setSelectedShop(@RequestBody Map<String, Long> body) {
        boolean result = shopUserOrchestrator.setSelectedShop(body);
        return ResponseEntity.ok(result);
    }


    @Operation(summary = "(All) Get logged user information")
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getLoggedUser() {
        UserDTO loggedUser = new UserDTO(userService.findLoggedUserHelper());
        connectionService.enrichWithConnection(loggedUser);
        return ResponseEntity.ok(loggedUser);
    }


    @Operation(summary = "(Admin) Get all available drivers (no truck assigned)")
    @GetMapping("/drivers/available/")
    public ResponseEntity<List<UserDTO>> getAvailableDrivers() {
        List<UserDTO> dtos = userService.findAvailableDrivers().stream().map(UserDTO::new).toList();
        connectionService.enrichWithConnections(dtos);
        return ResponseEntity.ok(dtos);
    }


    @Operation(summary = "(Admin) Get all users by role")
    @GetMapping("/role/")
    public ResponseEntity<List<UserDTO>> getAllUsersByRole(@RequestParam String role) {
        List<UserDTO> dtos = userService.findAllByRole(role).stream().map(UserDTO::new).toList();
        connectionService.enrichWithConnections(dtos);
        return ResponseEntity.ok(dtos);
    }


    @Operation(summary = "(Admin) Get all users information")
    @GetMapping("/")
    public ResponseEntity<PageResponse<UserDTO>> getAllUsers(Pageable pageable) {
        Page<User> allUsers = userService.findAll(pageable);
        PageResponse<UserDTO> pageResponse = PageFormatter.toPageResponse(allUsers, UserDTO::new);
        connectionService.enrichWithConnections(pageResponse.getItems());
        return ResponseEntity.ok(pageResponse);
    }


    //Needs the id as path variable to allow changing the profile image when the user is firstly created (registered)
    @Operation(summary = "(User) Update remote user image")
    @PutMapping("/image/{id}")
    public ResponseEntity<UserDTO> uploadUserImage(@PathVariable Long id, @RequestParam("image") MultipartFile image) {
        UserDTO savedUser = new UserDTO(userService.uploadUserImage(id, image));
        connectionService.enrichWithConnection(savedUser);
        return ResponseEntity.ok(savedUser);
    }


    //Option 1: Delete User entities (statistics information will be lost, reviews and orders need to be reassigned to a generic anon user, which affects data possession)
    //Option 2 (active): Anonymize / Clear sensible user data (delete address and cards, anonymize the rest of sensible information, mark account as deleted (non-accessible))
    @Operation(summary = "(User) Anonymize logged user account")
    @PutMapping("/anonymize")
    public ResponseEntity<UserDTO> anonymizeLoggedUser() {
        UserDTO savedUser = new UserDTO(userService.anonymizeLoggedUser());
        connectionService.enrichWithConnection(savedUser);
        return ResponseEntity.ok(savedUser);
    }


    @Operation(summary = "(User) Delete logged user image")
    @DeleteMapping("/image")
    public ResponseEntity<UserDTO> deleteUserImage() {
        UserDTO savedUser = new UserDTO(userService.deleteUserImage());
        connectionService.enrichWithConnection(savedUser);
        return ResponseEntity.ok(savedUser);
    }


    @Operation(summary = "(User) Update logged user data")
    @PutMapping("/data")
    public ResponseEntity<UserDTO> updateLoggedUserData(@RequestBody UserDTO userDTO){
        UserDTO savedUser = new UserDTO(userService.updateLoggedUserData(userDTO));
        connectionService.enrichWithConnection(savedUser);
        return ResponseEntity.ok(savedUser);
    }


    @Operation(summary = "(User) Create logged user address")
    @PostMapping("/addresses")
    public ResponseEntity<UserDTO> createAddress(@RequestBody AddressDTO addressDTO){
        UserDTO savedUser = new UserDTO(userService.createAddress(addressDTO));
        connectionService.enrichWithConnection(savedUser);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedUser.getId())
                .toUri();

        return ResponseEntity.created(location).body(savedUser);
    }


    @Operation(summary = "(User) Edit logged user address")
    @PutMapping("/addresses")
    public ResponseEntity<UserDTO> editAddress(@RequestBody AddressDTO addressDTO){
        UserDTO savedUser = new UserDTO(userService.editAddress(addressDTO));
        connectionService.enrichWithConnection(savedUser);
        return ResponseEntity.ok(savedUser);
    }


    @Operation(summary = "(User) Delete logged user address by ID")
    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<UserDTO> deleteAddress(@PathVariable Long id){
        UserDTO savedUser = new UserDTO(userService.deleteAddress(id));
        connectionService.enrichWithConnection(savedUser);
        return ResponseEntity.ok(savedUser);
    }


    @Operation(summary = "(User) Create logged user card")
    @PostMapping("/cards")
    public ResponseEntity<UserDTO> createPaymentCard(@RequestBody PaymentCardDTO cardDTO){
        UserDTO savedUser = new UserDTO(userService.createPaymentCard(cardDTO));
        connectionService.enrichWithConnection(savedUser);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedUser.getId())
                .toUri();

        return ResponseEntity.created(location).body(savedUser);
    }


    @Operation(summary = "(User) Update logged user card")
    @PutMapping("/cards")
    public ResponseEntity<UserDTO> editPaymentCard(@RequestBody PaymentCardDTO paymentCardDTO){
        UserDTO savedUser = new UserDTO(userService.editPaymentCard(paymentCardDTO));
        connectionService.enrichWithConnection(savedUser);
        return ResponseEntity.ok(savedUser);
    }


    @Operation(summary = "(User) Delete logged user card by ID")
    @DeleteMapping("/cards/{id}")
    public ResponseEntity<UserDTO> deletePaymentCard(@PathVariable Long id){
        UserDTO savedUser = new UserDTO(userService.deletePaymentCard(id));
        connectionService.enrichWithConnection(savedUser);
        return ResponseEntity.ok(savedUser);
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
        boolean savedState = userService.toggleAllUsersBan(banState);
        return ResponseEntity.ok(savedState);
    }


    @Operation(summary = "(Admin) Unban user by ID")
    @PutMapping("/ban/{id}")
    public ResponseEntity<UserDTO> toggleUserBanById(@PathVariable Long id, @RequestBody boolean banState){
        UserDTO savedUser = new UserDTO(userService.toggleUserBanById(id, banState));
        connectionService.enrichWithConnection(savedUser);
        return ResponseEntity.ok(savedUser);
    }


    @Operation(summary = "(Admin) Anonymize all users (except admin)")
    @PutMapping("/anon/")
    public ResponseEntity<Boolean> anonAllUsers(){
        boolean result = userService.anonAllUsers();
        return ResponseEntity.ok(result);
    }


    @Operation(summary = "(Admin) Anonymize user by ID")
    @PutMapping("/anon/{id}")
    public ResponseEntity<UserDTO> anonUserById(@PathVariable Long id){
        UserDTO savedUser = new UserDTO(userService.anonUserById(id));
        connectionService.enrichWithConnection(savedUser);
        return ResponseEntity.ok(savedUser);
    }


    @Operation(summary = "(Admin) Delete all users (except admin)")
    @DeleteMapping("/")
    public ResponseEntity<Boolean> deleteAllUsers(){
        boolean result = userService.deleteAllUsers();
        return ResponseEntity.ok(result);
    }


    @Operation(summary = "(Admin) Delete user by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteUserById(@PathVariable Long id){
        boolean result = userService.deleteUserById(id);
        return ResponseEntity.ok(result);
    }


    @Operation(summary = "(Admin) Get users stats")
    @GetMapping("/stats")
    public ResponseEntity<List<StatDTO>> getUsersStats(){
        List<StatDTO> usersStats = userService.getUsersStats();
        return ResponseEntity.ok(usersStats);
    }
}
