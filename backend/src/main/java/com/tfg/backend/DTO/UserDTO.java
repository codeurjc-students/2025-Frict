package com.tfg.backend.DTO;

import com.tfg.backend.model.User;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Getter
@Setter
public class UserDTO {
    private Long id;
    private String name;
    private String username;
    private String email;
    private String imageUrl;
    private boolean banned;
    private Set<String> roles;

    public UserDTO(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.banned = user.isBanned();
        this.roles = user.getRoles();
        this.id = user.getId();
        this.imageUrl = user.getUserImage().getImageUrl();
    }
}
