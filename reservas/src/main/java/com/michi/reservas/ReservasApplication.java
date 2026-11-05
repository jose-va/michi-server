package com.michi.reservas;

import com.michi.reservas.push.PushProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.TimeZone;

@EnableConfigurationProperties(PushProperties.class)
@SpringBootApplication
public class ReservasApplication {
	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(ReservasApplication.class, args);
	}
}
