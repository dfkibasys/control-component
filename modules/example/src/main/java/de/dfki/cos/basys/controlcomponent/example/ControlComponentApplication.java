package de.dfki.cos.basys.controlcomponent.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "de.dfki.cos.basys.controlcomponent.spring")
public class ControlComponentApplication {

	public static void main(String[] args) {
		SpringApplication.run(ControlComponentApplication.class, args);
	}

}
