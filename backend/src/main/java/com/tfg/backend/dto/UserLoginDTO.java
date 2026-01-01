package com.tfg.backend.dto;


import com.tfg.backend.model.User;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UserLoginDTO {
    private Long id;
    private String imageUrl; //May be null when the user is firstly registered into the system
    private String name;
    private String username;
    private Set<String> roles;

    public UserLoginDTO(User u){
        this.id = u.getId();
        if(u.getUserImage() != null){
            this.imageUrl = u.getUserImage().getImageUrl();
        }
        this.name = u.getName();
        this.username = u.getUsername();
        this.roles = u.getRoles();
    }
}