package com.example.springboot_api.config.security;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.springboot_api.models.User;

import lombok.Getter;

@Getter
public class UserPrincipal implements UserDetails, Principal {

    private final UUID id;
    private final String email;
    private final String password;
    private final List<GrantedAuthority> authorities;

    public UserPrincipal(User user, List<GrantedAuthority> authorities) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.authorities = authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    // GIỜ override hợp lệ
    @Override
    public String getName() {
        return id.toString();
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
