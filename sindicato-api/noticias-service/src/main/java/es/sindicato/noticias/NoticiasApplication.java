package es.sindicato.noticias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "es.sindicato")
public class NoticiasApplication {
    public static void main(String[] args) {
        SpringApplication.run(NoticiasApplication.class, args);
    }
}
