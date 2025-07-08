package com.tfg.ProjectBackend.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegisterDTO {
    private String name;
    private String userName;
    private String password;
    private String email;
    private String address;

    public UserRegisterDTO() {
    }

    public UserRegisterDTO(String name, String userName, String password, String email, String address) {
        this.name = name;
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.address = address;
    }
}
