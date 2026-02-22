package com.tfg.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tfg.backend.model.Address;
import com.tfg.backend.model.ImageInfo;
import com.tfg.backend.model.PaymentCard;
import com.tfg.backend.model.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class UserDTO {
    private Long id;
    private String name;
    private String username;
    private Set<String> roles;
    private String email;
    private String phone;
    private List<AddressDTO> addresses = new ArrayList<>();
    private List<PaymentCardDTO> cards = new ArrayList<>();
    private ImageInfo imageInfo;
    private boolean banned;
    private boolean deleted;
    private boolean logged;

    //Stats data
    private int ordersCount;
    private int favouriteProductsCount;

    @JsonFormat(pattern = "dd/MM/yy HH:mm")
    private LocalDateTime lastConnection;

    private Long selectedShopId;

    public UserDTO() {
    }

    public UserDTO(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.banned = user.isBanned();
        this.deleted = user.isDeleted();
        this.logged = user.isLogged();
        this.lastConnection = user.getLastConnection();
        if (user.getSelectedShop() != null){
            this.selectedShopId = user.getSelectedShop().getId();
        }
        this.roles = user.getRoles();
        this.id = user.getId();
        this.imageInfo = user.getUserImage();
        for (Address address : user.getAddresses()) {
            this.addresses.add(new AddressDTO(address));
        }
        for (PaymentCard card : user.getCards()) {
            this.cards.add(new PaymentCardDTO(card));
        }

        this.ordersCount = user.getRegisteredOrders().size();
        this.favouriteProductsCount = user.getFavouriteProducts().size();
    }
}
