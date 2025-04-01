package com.aciworldwide.aclabs22;

import com.aciworldwide.aclabs22.services.AccountService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Aclabs22Application {

	public static void main(String[] args) {
		//System.setProperty("com.sun.jndi.ldap.object.trustURLCodebase", "true");
		SpringApplication.run(Aclabs22Application.class, args);
	}

	@Bean
	public AccountService getAccountService() {
		return new AccountService();
	}
}
