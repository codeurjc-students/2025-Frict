package com.tfg.backend.DTO;

import com.tfg.backend.model.User;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FullUserDTO {
    private Long id;
    private String name;
    private String username;
    private String email;
    private String photo;
    private boolean banned;
    private List<String> roles;

    public FullUserDTO(String name, String username, String email, boolean banned, List<String> roles, Long id) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.email = email;
        this.banned = banned;
        this.roles = roles;
        this.photo = "/api/users/img/" + id;
    }

    public FullUserDTO(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.banned = user.isBanned();
        this.roles = user.getRoles();
        this.id = user.getId();
        this.photo = "/api/users/img/" + user.getId();
    }
}
