package com.example.onlinebanking.config;

import com.example.onlinebanking.domain.IbanGenerator;
import com.example.onlinebanking.domain.PasswordGenerator;
import com.example.onlinebanking.domain.SecurePasswordGenerator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class ApplicationConfiguration {
    @Bean
    Clock businessClock(AppProperties properties) {
        return Clock.system(ZoneId.of(properties.clockZone()));
    }

    @Bean
    SecureRandom secureRandom() {
        return new SecureRandom();
    }

    @Bean
    PasswordGenerator passwordGenerator(SecureRandom secureRandom) {
        return new SecurePasswordGenerator(secureRandom);
    }

    @Bean
    IbanGenerator ibanGenerator(AppProperties properties, SecureRandom secureRandom) {
        return new IbanGenerator(
                properties.account().ibanCountryCode(),
                properties.account().ibanBankCode(),
                secureRandom
        );
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
