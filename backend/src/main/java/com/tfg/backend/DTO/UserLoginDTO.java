package com.tfg.backend.DTO;


import com.tfg.backend.model.User;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

//Basic user auth info sent to frontend after user login (saves it for further requests) or registration (takes only important fields)
@Getter
@Setter
public class UserLoginDTO {
    private Long id;
    private String name;
    private String username;
    private List<String> roles;

    public UserLoginDTO(User u){
        this.id = u.getId();
        this.name = u.getName();
        this.username = u.getUsername();
        this.roles = u.getRoles();
    }
}