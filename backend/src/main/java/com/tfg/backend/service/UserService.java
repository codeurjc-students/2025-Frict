package com.tfg.backend.service;

import com.tfg.backend.dto.UserLoginDTO;
import com.tfg.backend.dto.UserSignupDTO;
import com.tfg.backend.model.*;
import com.tfg.backend.repository.UserRepository;
import com.tfg.backend.utils.GlobalDefaults;
import jakarta.servlet.http.HttpServletRequest;
import org.hamcrest.Condition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.*;

@Service
public class UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StorageService storageService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private ReviewService reviewService;

	@Autowired
	private UserRepository userRepository;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Page<User> findAll(Pageable pageInfo) {
        return userRepository.findAll(pageInfo);
    }

    public User save(User u) {
        return userRepository.save(u);
    }

    public List<User> saveAll(List<User> l) {
        return userRepository.saveAll(l);
    }

    public void delete(User u){
        userRepository.delete(u);
    }

    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean existsByUsername(String username) { return userRepository.existsByUsername(username); }

    public boolean existsByEmail(String email) { return userRepository.existsByEmail(email); }

    public Optional<UserLoginDTO> getLoginInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .map(UserLoginDTO::new);
    }

    public Optional<User> getLoggedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if ("anonymousUser".equals(principal)) {
            return Optional.empty();
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username);
    }

    public User registerUser(UserSignupDTO dto) {
        String role;
        if (dto.getRole() == null || dto.getRole().isEmpty()){
            role = "USER";
        }
        else role = dto.getRole();

        User newUser = new User(dto.getName(), dto.getUsername(), dto.getEmail(), passwordEncoder.encode(dto.getPassword()), role);

        //Assign default user image to prevent later errors
        newUser.setUserImage(GlobalDefaults.USER_IMAGE);

        return this.save(newUser);
    }

    public User anonymizeUser(User user){
        //Erase and anonymize all user data (in order to preserve historic data by hiding identities)
        String uniqueUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        user.setName("Usuario eliminado " + uniqueUuid);
        user.setUsername("deleteduser_" + uniqueUuid);
        user.setEncodedPassword(""); // Empty password, as it may contain sensible data
        user.setOtpCode(null);
        user.setOtpExpiration(null);
        user.getRoles().clear(); // Unauthorized to access secured pages
        user.setEmail("deleteduser_" + uniqueUuid + "@frictapp.com");
        user.setPhone(null);
        user.getAddresses().clear();
        user.getCards().clear();

        if (user.getUserImage() != null && !user.getUserImage().getS3Key().equals(GlobalDefaults.USER_IMAGE.getS3Key())) {
            storageService.deleteFile(user.getUserImage().getS3Key());
        }
        user.setUserImage(GlobalDefaults.USER_IMAGE);

        user.setDeleted(true); //Mark as deleted user
        user.getAllOrderItems().removeIf(item -> item.getOrder() == null); //Clear cart items

        return user;
    }

    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean isUsernameTaken(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean isBannedByUsername(String username) {
        return userRepository.existsByUsernameAndIsBannedTrue(username);
    }

    public boolean isDeletedByUsername(String username) {
        return userRepository.existsByUsernameAndIsDeletedTrue(username);
    }

    public boolean isBannedByEmail(String email) {
        return userRepository.existsByEmailAndIsBannedTrue(email);
    }

    public boolean isDeletedByEmail(String email) {
        return userRepository.existsByEmailAndIsDeletedTrue(email);
    }

    //Role-based queries
    public List<User> findAllByRole(String role){ return userRepository.findByRolesContaining(role); }

    //Stats
    public Long count(){ return this.userRepository.count(); }
    public Long countByRole(String role){ return this.userRepository.countByRolesContaining(role); }
    public Long countByIsBannedTrue(){ return this.userRepository.countByIsBannedTrue();}
    public Long countByIsDeletedTrue(){ return this.userRepository.countByIsDeletedTrue();}

    public User findUserHelper(Long id) {
        return this.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "The user with ID'" + id + "' does not exist.")); //Captured by ResponseStatusExceptionResolver (Spring DispatcherServlet internal helper class)
    }

    public User findUserHelper(String username) {
        return this.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "The user '" + username + "' does not exist.")); //Captured by ResponseStatusExceptionResolver (Spring DispatcherServlet internal helper class)
    }

    public User findLoggedUserHelper() {
        return this.getLoggedUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You must be logged to perform this operation."));
    }
}
