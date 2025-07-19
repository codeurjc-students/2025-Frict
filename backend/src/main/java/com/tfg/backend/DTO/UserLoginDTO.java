package com.tfg.backend.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserLoginDTO {
    private String email;
    private String password;

    public UserLoginDTO() {}

    public UserLoginDTO(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
