package com.bodywise;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@SpringBootApplication
@EnableScheduling
public class BodywiseApplication {

    public static void main(String[] args) {
        SpringApplication.run(BodywiseApplication.class, args);
    }

    /** All "today / tomorrow" calculations use the app time zone (APP_TIMEZONE, default Asia/Kolkata). */
    @Bean
    Clock clock(@Value("${app.timezone}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
