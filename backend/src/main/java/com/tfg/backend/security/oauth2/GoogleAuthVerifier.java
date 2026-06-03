package com.tfg.backend.security.oauth2;

public interface GoogleAuthVerifier {
    GoogleAuthPayload verify(String idToken);
}
