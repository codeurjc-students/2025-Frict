package com.tfg.backend.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSignupDTO {
    private String name;
    private String username;
    private String password;
    private String email;
    private String phone;
    private String address;

    public UserSignupDTO() {
    }

    public UserSignupDTO(String name, String username, String password, String email, String phone, String address) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }
}
