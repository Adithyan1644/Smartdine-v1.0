package com.smartdine.coreheart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.smartdine")
@ComponentScan(basePackages = "com.smartdine")
@EnableJpaRepositories(basePackages = "com.smartdine.repository")
@EntityScan(basePackages = "com.smartdine.coreheart")
public class CoreHeartApplication {

	public static void main(String[] args) {
		javafx.application.Application.launch(UiLauncher.class, args);
	}

}
