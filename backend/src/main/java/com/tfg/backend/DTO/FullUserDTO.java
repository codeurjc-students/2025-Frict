package com.tfg.backend.DTO;

import com.tfg.backend.model.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class FullUserDTO implements UserDetails {
    private Long id;
    private String name;
    private String username;
    private String email;
    private String photo;
    private boolean banned;
    private List<String> roles;

    public FullUserDTO(String name, String username, String email, boolean banned, List<String> roles, Long id) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.email = email;
        this.banned = banned;
        this.roles = roles;
        this.photo = "/api/users/img/" + id;
    }

    public FullUserDTO(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.banned = user.isBanned();
        this.roles = user.getRoles();
        this.id = user.getId();
        this.photo = "/api/users/img/" + user.getId();
    }

    // UserDetails interface required methods

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public List<SimpleGrantedAuthority> getAuthorities() {
        // Change roles to an authority list
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !this.banned;  // The account is blocked is user is banned
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;  // If credentials have not expired returns true
    }

    @Override
    public boolean isEnabled() {
        return !this.banned;  // If user is banned is not enabled
    }
}
