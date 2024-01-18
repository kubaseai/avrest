package io.github.kubaseai.av.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;

@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
	
	private final MainConfiguration cfg;
	
	public WebSecurityConfig(MainConfiguration cfg) {
		this.cfg = cfg;
	}
	
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests().antMatchers("/**").authenticated().and().httpBasic();
        	http.csrf().disable();
        	http.addFilterBefore(new HttpBasicFilter(cfg), X509AuthenticationFilter.class);        
    	}
}
