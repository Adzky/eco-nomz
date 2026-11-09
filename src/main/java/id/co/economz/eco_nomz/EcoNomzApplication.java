package id.co.economz.eco_nomz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EcoNomzApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcoNomzApplication.class, args);
	}

}
