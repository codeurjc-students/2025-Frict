package com.tfg.backend.security.oauth2;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Profile("!prod")
@Slf4j
public class DirectGoogleAuthVerifier implements GoogleAuthVerifier {

    @Value("${google.auth.clientId}")
    private String googleClientId;

    @Override
    public GoogleAuthPayload verify(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new BadCredentialsException("Invalid or expired Google token.");
            }
            GoogleIdToken.Payload payload = token.getPayload();
            return new GoogleAuthPayload(payload.getEmail(), (String) payload.get("name"));
        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            throw new BadCredentialsException("Error while verifying Google token.", e);
        }
    }
}
