package com.tfg.backend.security.jwt;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {

	private Status status;
	private String message;
	private String error;
	private String newUserEmail = null;

	public enum Status {
		SUCCESS, FAILURE
	}

	public AuthResponse() {
	}

	public AuthResponse(Status status, String message) {
		this.status = status;
		this.message = message;
	}

	public AuthResponse(Status status, String message, String newUserEmail) {
		this.status = status;
		this.message = message;
		this.newUserEmail = newUserEmail;
	}

	@Override
	public String toString() {
		return "LoginResponse [status=" + status + ", message=" + message + ", error=" + error + "]";
	}

}
