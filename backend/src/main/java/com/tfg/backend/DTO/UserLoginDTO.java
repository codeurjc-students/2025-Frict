package com.tfg.backend.DTO;


import com.tfg.backend.model.User;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Getter
@Setter
public class UserLoginDTO {
    private Long id;
    private String thumbnailUrl;
    private String name;
    private String username;
    private Set<String> roles;

    public UserLoginDTO(User u){
        this.id = u.getId();
        this.thumbnailUrl = "/api/v1/users/thumbnail/" + u.getId();
        this.name = u.getName();
        this.username = u.getUsername();
        this.roles = u.getRoles();
    }
}