package com.tfg.backend.security;

import java.util.ArrayList;
import java.util.List;

import com.tfg.backend.model.User;
import com.tfg.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;



@Service
public class RepositoryUserDetailsService implements UserDetailsService {

	@Autowired
	private UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String name) throws UsernameNotFoundException {

		User user;
        user = userRepository.findByName(name)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<GrantedAuthority> roles = new ArrayList<>();
		for (String role : user.getRoles()) {
			roles.add(new SimpleGrantedAuthority("ROLE_" + role));
		}

		return new org.springframework.security.core.userdetails.User(user.getName(), 
				user.getEncodedPassword(), roles);
	}
}
