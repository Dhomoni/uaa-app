package com.dhomoni.uaa.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper=false)
public class CustomUser extends User {
	
	private static final long serialVersionUID = 1L;
	private final Long registrationId;
	
	public CustomUser(String username, String password,
			Collection<? extends GrantedAuthority> authorities, Long registrationId) {
		super(username, password, authorities);
		this.registrationId = registrationId;
	}
}
