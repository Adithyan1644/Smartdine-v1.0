package com.smartdine.coreheart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.smartdine")
@ComponentScan(basePackages = "com.smartdine")
@EnableJpaRepositories(basePackages = "com.smartdine.repository")
@EntityScan(basePackages = "com.smartdine.coreheart")
@EnableAsync
@EnableScheduling
public class CoreHeartApplication {

	public static void main(String[] args) {
		String activeProfiles = System.getProperty("spring.profiles.active");
		if (activeProfiles == null) {
			activeProfiles = System.getenv("SPRING_PROFILES_ACTIVE");
		}
		
		boolean isProd = activeProfiles != null && activeProfiles.contains("prod");
		boolean isHeadless = java.awt.GraphicsEnvironment.isHeadless();

		if (isProd || isHeadless) {
			System.out.println("🖥️ [CoreHeartApplication] Running in Headless/Cloud Mode. Bypassing JavaFX GUI...");
			SpringApplication.run(CoreHeartApplication.class, args);
		} else {
			System.out.println("🖥️ [CoreHeartApplication] Running in Desktop Mode. Launching JavaFX GUI...");
			try {
				Class<?> launcherClass = Class.forName("com.smartdine.coreheart.UiLauncher");
				Class<?> appClass = Class.forName("javafx.application.Application");
				java.lang.reflect.Method launchMethod = appClass.getMethod("launch", Class.class, String[].class);
				launchMethod.invoke(null, launcherClass, args);
			} catch (Exception e) {
				System.err.println("❌ Failed to launch desktop GUI: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

}
