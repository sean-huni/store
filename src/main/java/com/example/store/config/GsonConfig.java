package com.example.store.config;

import com.example.store.config.adapter.ZonedDateTimeBiSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZonedDateTime;

@Configuration
@RequiredArgsConstructor
public class GsonConfig {

    private final ZonedDateTimeBiSerializer zonedDateTimeBiSerializer;

    @Bean
    public Gson gson() {
        return new GsonBuilder()
                .registerTypeAdapter(ZonedDateTime.class, zonedDateTimeBiSerializer)
//                .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                .setPrettyPrinting()
                .create();
    }
}