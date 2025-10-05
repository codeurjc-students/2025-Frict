package com.tfg.backend.security.jwt;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Setter
@Getter
public class Token {

	private TokenType tokenType;
	private String tokenValue;
	private Long duration;
	private LocalDateTime expiryDate;

	public enum TokenType {
		ACCESS, REFRESH
	}

	public Token(TokenType tokenType, String tokenValue, Long duration, LocalDateTime expiryDate) {
		super();
		this.tokenType = tokenType;
		this.tokenValue = tokenValue;
		this.duration = duration;
		this.expiryDate = expiryDate;
	}

    @Override
	public String toString() {
		return "Token [tokenType=" + tokenType + ", tokenValue=" + tokenValue + ", duration=" + duration
				+ ", expiryDate=" + expiryDate + "]";
	}
}
