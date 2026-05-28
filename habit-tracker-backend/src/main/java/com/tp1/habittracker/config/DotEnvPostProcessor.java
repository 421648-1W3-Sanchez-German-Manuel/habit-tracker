package com.tp1.habittracker.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

public class DotEnvPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        File envFile = new File(".env");
        if (!envFile.exists()) {
            return;
        }

        Map<String, Object> props = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx < 0) {
                    continue;
                }
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                } else if (value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                }
                props.put(key, value);
            }
        } catch (Exception ignored) {
            return;
        }

        if (!props.isEmpty()) {
            // SystemEnvironmentPropertySource applies the same relaxed-binding rules as
            // real OS env vars, so APP_JWT_SECRET resolves to app.jwt.secret automatically.
            environment.getPropertySources().addLast(
                new SystemEnvironmentPropertySource("dotenvFile", props)
            );
        }
    }
}
