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
        this.id = id;
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

    // Métodos requeridos por la interfaz UserDetails

    @Override
    public String getUsername() {
        return this.email;  // Usamos el email como nombre de usuario
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public List<SimpleGrantedAuthority> getAuthorities() {
        // Convertir los roles a una lista de autoridades
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;  // Aquí puedes aplicar tu lógica para determinar si la cuenta ha expirado
    }

    @Override
    public boolean isAccountNonLocked() {
        return !this.banned;  // Si está baneado, la cuenta está bloqueada
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;  // Devolver true si las credenciales no han expirado
    }

    @Override
    public boolean isEnabled() {
        return !this.banned;  // Si está baneado, no está habilitado
    }
}
