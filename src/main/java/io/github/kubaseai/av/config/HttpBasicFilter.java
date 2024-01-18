package io.github.kubaseai.av.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import io.github.kubaseai.av.utils.RolesHelper;

@WebFilter(urlPatterns = {"/**" })
public class HttpBasicFilter implements Filter {

	private final MainConfiguration cfg;

	public HttpBasicFilter(MainConfiguration cfg) {
		this.cfg = cfg;
	}

	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException, IOException, ServletException {
		String token = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (token != null && token.toLowerCase().startsWith("basic ")) {
			String userPass = new String(Base64.getDecoder().decode(token.substring(6)));
			int sepPos = userPass.indexOf(':');
			if (sepPos==-1) {
				return currentAuthentication();
			}
			String user = userPass.substring(0, sepPos);
			String tkn = DigestUtils.sha256Hex(userPass);
			if (Arrays.asList(cfg.getAccessTokens().split("\\,")).contains(tkn)) {
				final List<GrantedAuthority> roles = new LinkedList<>();
				roles.add(new SimpleGrantedAuthority("ROLE_USER"));
				if (!"user".equals(user)) {
					roles.add(new SimpleGrantedAuthority("ROLE_"+user.toUpperCase()));
				}
				RolesHelper.rewriteRoles(roles);
				return new UsernamePasswordAuthenticationToken(user, "", roles);
			}
		}
		return currentAuthentication();
	}

	private Authentication currentAuthentication() {
		return SecurityContextHolder.getContext().getAuthentication();
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		Authentication auth = attemptAuthentication((HttpServletRequest)request, (HttpServletResponse)response);
		if (auth!=null) {
			SecurityContextHolder.getContext().setAuthentication(auth);
		}
		chain.doFilter(request, response);
	}
}