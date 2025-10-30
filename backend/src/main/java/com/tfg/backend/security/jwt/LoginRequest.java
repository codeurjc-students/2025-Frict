package com.tfg.backend.security.jwt;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginRequest {

	private String username;
	private String password;

	public LoginRequest() {
	}

	public LoginRequest(String username, String password) {
		this.username = username;
		this.password = password;
	}

    @Override
	public String toString() {
		return "LoginRequest [username=" + username + ", password=" + password + "]";
	}
}
