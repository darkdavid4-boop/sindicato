package es.sindicato.afiliados;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "es.sindicato")
public class AfiliadosApplication {
    public static void main(String[] args) {
        SpringApplication.run(AfiliadosApplication.class, args);
    }
}
