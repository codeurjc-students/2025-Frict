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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

	public Optional<UserLoginDTO> getLoginInfo(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if(principal == null) {
            return Optional.empty();
        }
        return Optional.of(new UserLoginDTO(userRepository.findByUsername(principal.getName()).orElseThrow()));
	}

    public Optional<User> getLoggedUser(HttpServletRequest request) {
        Optional<UserLoginDTO> loginInfo = this.getLoginInfo(request);
        if(loginInfo.isEmpty()){
            return Optional.empty();
        }
        return userRepository.findById(loginInfo.get().getId());
    }

    public User registerUser(UserSignupDTO dto) {
        User newUser = new User(dto.getName(), dto.getUsername(), dto.getEmail(), passwordEncoder.encode(dto.getPassword()), "USER");
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

    // Removes (deletes) all user relations in order for the user to be deleted from DB without data integrity violations
    @Transactional
    public User unlinkUser(User user){
       for (Order order : user.getRegisteredOrders()) {
            order.setUser(null);
            orderService.save(order);
        }

        user.getAllOrderItems().clear();
        orderItemService.unlinkItemsFromUser(user.getId()); //Custom query, as items are needed to be removed automatically when removed from cart, but not removed when deleting an user

        // PASO 3: Reviews
        for (Review review : reviewService.findAllByUser(user)) {
            review.setUser(null);
            reviewService.save(review);
        }
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
}
