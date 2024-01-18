package io.github.kubaseai.av.utils;

import java.util.List;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class RolesHelper {
	
	public final static void rewriteRoles(final List<GrantedAuthority> roles) {
		SecurityContext ctx = SecurityContextHolder.getContext();
		Authentication auth = ctx!=null ? ctx.getAuthentication() : null;
		if (auth!=null) {
			auth.getAuthorities().forEach( role -> {
				if (!containsRoles(roles, role)) {
					roles.add(new SimpleGrantedAuthority(role.getAuthority()));
				}
			});
		}
	}

	private static boolean containsRoles(List<GrantedAuthority> roles, GrantedAuthority role) {
		for (GrantedAuthority a : roles) {
			if (Objects.equals(a.getAuthority(), role.getAuthority())) {
				return true;
			}
		}
		return false;
	}

}
