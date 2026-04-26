package com.tfg.backend.dto;

import com.tfg.backend.model.Address;
import com.tfg.backend.model.ImageInfo;
import com.tfg.backend.model.PaymentCard;
import com.tfg.backend.model.User;
import com.tfg.backend.notification.ConnectionDTO;
import lombok.Getter;
import lombok.Setter;

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

    //Stats data
    private int ordersCount;
    private int favouriteProductsCount;
    private Long selectedShopId;

    //Connection data
    private ConnectionDTO connection;

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
