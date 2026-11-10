package ua.lpnu.kzp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Читає метадані, які Maven підставляє під час збірки. */
final class ApplicationInfo {
    private ApplicationInfo() {
    }

    static String read(String key) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = ApplicationInfo.class.getResourceAsStream("/application.properties")) {
            if (input == null) {
                throw new IOException("Немає метаданих програми; виконайте Maven-збірку");
            }
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        }
        String value = properties.getProperty(key);
        if (value == null || value.isBlank() || value.contains("${")) {
            throw new IOException("Некоректні метадані збірки: %s".formatted(key));
        }
        return value;
    }
}
