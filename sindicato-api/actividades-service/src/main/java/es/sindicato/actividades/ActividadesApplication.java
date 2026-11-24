package es.sindicato.actividades;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "es.sindicato")
public class ActividadesApplication {
    public static void main(String[] args) {
        SpringApplication.run(ActividadesApplication.class, args);
    }
}
