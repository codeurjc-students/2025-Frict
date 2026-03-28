package com.tfg.backend.service;

import com.tfg.backend.dto.*;
import com.tfg.backend.model.*;
import com.tfg.backend.repository.UserRepository;
import com.tfg.backend.utils.GlobalDefaults;
import com.tfg.backend.utils.StatDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;
    private final UserRepository userRepository;

    // --- READ-ONLY METHODS ---

    public List<User> findAll() { return userRepository.findAll(); }
    public Page<User> findAll(Pageable pageInfo) { return userRepository.findAll(pageInfo); }
    public Optional<User> findById(Long userId) { return userRepository.findById(userId); }
    public Optional<User> findByUsername(String username) { return userRepository.findByUsername(username); }
    public Optional<User> findByEmail(String email) { return userRepository.findByEmail(email); }
    public boolean existsByUsername(String username) { return userRepository.existsByUsername(username); }
    public boolean existsByEmail(String email) { return userRepository.existsByEmail(email); }
    public boolean isEmailTaken(String email) { return userRepository.existsByEmail(email); }
    public boolean isUsernameTaken(String username) { return userRepository.existsByUsername(username); }
    public boolean isBannedByUsername(String username) { return userRepository.existsByUsernameAndIsBannedTrue(username); }
    public boolean isDeletedByUsername(String username) { return userRepository.existsByUsernameAndIsDeletedTrue(username); }
    public boolean isBannedByEmail(String email) { return userRepository.existsByEmailAndIsBannedTrue(email); }
    public boolean isDeletedByEmail(String email) { return userRepository.existsByEmailAndIsDeletedTrue(email); }
    public List<User> findAllByRole(String role){ return userRepository.findByRolesContaining(role); }

    public List<User> findAvailableDrivers() {
        return userRepository.findByRolesContainingAndAssignedTruckIsNull("DRIVER");
    }

    public UserLoginDTO getLoginInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "No user logged in this session.");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Authenticated user does not have an associated user."));

        return new UserLoginDTO(user);
    }

    public Optional<User> getLoggedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        return userRepository.findByUsername(authentication.getName());
    }

    public User findUserHelper(Long id) {
        return this.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "The user with ID'" + id + "' does not exist."));
    }

    public User findUserHelper(String username) {
        return this.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "The user '" + username + "' does not exist."));
    }

    public User findLoggedUserHelper() {
        return this.getLoggedUser().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You must be logged to perform this operation."));
    }

    // --- WRITING METHODS (override Transactional) ---

    @Transactional
    public User save(User u) { return userRepository.save(u); }

    @Transactional
    public List<User> saveAll(List<User> l) { return userRepository.saveAll(l); }

    @Transactional
    public void delete(User u){ userRepository.delete(u); }

    @Transactional
    public User registerUser(UserSignupDTO dto) {
        if (this.isUsernameTaken(dto.getUsername()) || this.isEmailTaken(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This username or email is already taken.");
        }

        String role = (dto.getRole() == null || dto.getRole().isEmpty()) ? "USER" : dto.getRole();
        User newUser = new User(dto.getName(), dto.getUsername(), dto.getEmail(), passwordEncoder.encode(dto.getPassword()), role);
        newUser.setUserImage(GlobalDefaults.USER_IMAGE);

        return userRepository.save(newUser);
    }

    @Transactional
    public boolean applyShopSelection(User loggedUser, Shop shop){
        loggedUser.setSelectedShop(shop); // Shop parameter may be null

        List<OrderItem> itemsToRemove = loggedUser.getItemsInCart();
        if (!itemsToRemove.isEmpty()) {
            loggedUser.getAllOrderItems().removeAll(itemsToRemove);
        }

        return true; // Saved automatically
    }

    @Transactional
    public User updateLoggedUserData(UserDTO userDTO){
        User loggedUser = this.findLoggedUserHelper();

        if(!userDTO.getUsername().equals(loggedUser.getUsername()) && this.existsByUsername(userDTO.getUsername())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Username is already taken.");
        }

        loggedUser.setName(userDTO.getName());
        loggedUser.setUsername(userDTO.getUsername());
        loggedUser.setEmail(userDTO.getEmail());
        loggedUser.setPhone(userDTO.getPhone());

        return loggedUser;
    }

    // --- ADDRESS AND CARD METHODS ---

    @Transactional
    public User createAddress(AddressDTO addressDTO){
        User loggedUser = this.findLoggedUserHelper();
        Address address = new Address(addressDTO.getAlias(), addressDTO.getStreet(), addressDTO.getNumber(), addressDTO.getFloor(), addressDTO.getPostalCode(), addressDTO.getCity(), addressDTO.getCountry());
        loggedUser.getAddresses().add(address);
        return loggedUser;
    }

    @Transactional
    public User editAddress(AddressDTO addressDTO){
        User loggedUser = this.findLoggedUserHelper();
        Address address = loggedUser.getAddresses().stream().filter(a -> a.getId().equals(addressDTO.getId())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address with ID " + addressDTO.getId() + " does not exist."));

        address.setAlias(addressDTO.getAlias());
        address.setStreet(addressDTO.getStreet());
        address.setNumber(addressDTO.getNumber());
        address.setFloor(addressDTO.getFloor());
        address.setPostalCode(addressDTO.getPostalCode());
        address.setCity(addressDTO.getCity());
        address.setCountry(addressDTO.getCountry());

        return loggedUser;
    }

    @Transactional
    public User deleteAddress(Long id){
        User loggedUser = this.findLoggedUserHelper();
        if(!loggedUser.getAddresses().removeIf(address -> address.getId().equals(id))){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Address with ID " + id + " not deleted as it does not exist.");
        }
        return loggedUser;
    }

    @Transactional
    public User createPaymentCard(PaymentCardDTO cardDTO){
        User loggedUser = this.findLoggedUserHelper();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
        PaymentCard card = new PaymentCard(cardDTO.getAlias(), cardDTO.getCardOwnerName(), cardDTO.getNumber(), cardDTO.getCvv(), YearMonth.parse(cardDTO.getDueDate(), formatter));
        loggedUser.getCards().add(card);
        return loggedUser;
    }

    @Transactional
    public User editPaymentCard(PaymentCardDTO paymentCardDTO){
        User loggedUser = this.findLoggedUserHelper();
        PaymentCard card = loggedUser.getCards().stream().filter(c -> c.getId().equals(paymentCardDTO.getId())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card with ID " + paymentCardDTO.getId() + " does not exist."));

        card.setAlias(paymentCardDTO.getAlias());
        card.setCardOwnerName(paymentCardDTO.getCardOwnerName());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
        card.setDueDate(YearMonth.parse(paymentCardDTO.getDueDate(), formatter));

        return loggedUser;
    }

    @Transactional
    public User deletePaymentCard(Long id){
        User loggedUser = this.findLoggedUserHelper();
        if(!loggedUser.getCards().removeIf(card -> card.getId().equals(id))){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Card with ID " + id + " not deleted as it does not exist.");
        }
        return loggedUser;
    }

    // --- IMAGE METHODS ---

    @Transactional
    public User uploadUserImage(Long id, MultipartFile image){
        User loggedUser = this.findUserHelper(id);

        if (loggedUser.getUserImage() != null && !loggedUser.getUserImage().equals(GlobalDefaults.USER_IMAGE)) {
            storageService.deleteFile(loggedUser.getUserImage().getS3Key());
        }

        if (!image.isEmpty()){
            try {
                Map<String, String> res = storageService.uploadFile(image, "users");
                ImageInfo userImageInfo = new ImageInfo(res.get("url"), res.get("key"), image.getOriginalFilename());
                loggedUser.setUserImage(userImageInfo);
            } catch (IOException e){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not upload user image to storage");
            }
        } else {
            loggedUser.setUserImage(GlobalDefaults.USER_IMAGE);
        }
        return loggedUser;
    }

    @Transactional
    public User deleteUserImage() {
        User loggedUser = this.findLoggedUserHelper();
        if (!loggedUser.getUserImage().equals(GlobalDefaults.USER_IMAGE)) {
            storageService.deleteFile(loggedUser.getUserImage().getS3Key());
            loggedUser.setUserImage(GlobalDefaults.USER_IMAGE);
        }
        return loggedUser;
    }

    // --- BAN AND DELETE METHODS ---

    @Transactional
    public boolean toggleAllUsersBan(boolean banState){
        List<User> allUsers = this.findAll();
        for (User u : allUsers) {
            if (!u.getRoles().contains("ADMIN")){
                u.setBanned(banState);
            }
        }
        return banState;
    }

    @Transactional
    public User toggleUserBanById(Long id, boolean banState){
        User user = this.findUserHelper(id);
        user.setBanned(banState);
        return user;
    }

    @Transactional
    public User anonymizeLoggedUser(){
        User loggedUser = this.findLoggedUserHelper();
        return this.anonymizeUser(loggedUser);
    }

    @Transactional
    public boolean anonAllUsers(){
        List<User> allUsers = this.findAll();
        for (User u : allUsers) {
            if (!u.getRoles().contains("ADMIN")){
                this.anonymizeUser(u);
            }
        }
        return true;
    }

    @Transactional
    public User anonUserById(Long id){
        User user = this.findUserHelper(id);
        return this.anonymizeUser(user);
    }

    @Transactional
    public boolean deleteAllUsers(){
        List<User> allUsers = this.findAll();
        for (User u : allUsers) {
            if (!u.getRoles().contains("ADMIN")){
                userRepository.delete(u);
            }
        }
        return true;
    }

    @Transactional
    public boolean deleteUserById(Long id){
        User user = this.findUserHelper(id);
        userRepository.delete(user);
        return true;
    }

    // --- AUXILIARY METHODS AND METRICS ---

    public User anonymizeUser(User user){
        String uniqueUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        user.setName("Usuario eliminado " + uniqueUuid);
        user.setUsername("deleteduser_" + uniqueUuid);
        user.setEncodedPassword("");
        user.setOtpCode(null);
        user.setOtpExpiration(null);
        user.setEmail("deleteduser_" + uniqueUuid + "@frictapp.com");
        user.setPhone(null);

        if (user.getRegisteredOrders() != null) {
            for (Order order : user.getRegisteredOrders()) {
                order.setFullSendingAddress(null);
            }
        }
        user.getAddresses().clear();
        user.getCards().clear();

        if (user.getUserImage() != null && !user.getUserImage().getS3Key().equals(GlobalDefaults.USER_IMAGE.getS3Key())) {
            storageService.deleteFile(user.getUserImage().getS3Key());
        }
        user.setUserImage(GlobalDefaults.USER_IMAGE);
        user.setDeleted(true);
        user.getAllOrderItems().removeIf(item -> item.getOrder() == null);

        return user;
    }

    public List<StatDTO> getUsersStats(){
        List<StatDTO> stats = new ArrayList<>();
        stats.add(new StatDTO("Totales", userRepository.count()));
        stats.add(new StatDTO("Baneados", userRepository.countByIsBannedTrue()));
        stats.add(new StatDTO("Anonimizados", userRepository.countByIsDeletedTrue()));
        Long internalAccounts = userRepository.countByRolesContaining("ADMIN") + userRepository.countByRolesContaining("MANAGER") + userRepository.countByRolesContaining("DRIVER");
        stats.add(new StatDTO("Cuentas Internas", internalAccounts));
        return stats;
    }
}