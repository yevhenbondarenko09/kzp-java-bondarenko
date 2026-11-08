package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Відокремлює читання та запис UTF-8 від обчислення показників. */
public final class FileReport {
    private FileReport() {
    }

    /**
     * Читає CSV без залежності від системного кодування.
     * @param input вхідний файл
     * @return рядки в порядку файла
     * @throws IOException якщо файл відсутній, недоступний або має некоректне UTF-8
     */
    public static List<String> readLines(Path input) throws IOException {
        return Files.readAllLines(input, StandardCharsets.UTF_8);
    }

    /**
     * Створює батьківські каталоги й записує готовий звіт.
     * @param output вихідний файл
     * @param report текст звіту
     * @throws IOException якщо каталог або файл неможливо записати
     */
    public static void write(Path output, String report) throws IOException {
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(output, report, StandardCharsets.UTF_8);
    }

    /** Перевіряє однакові шляхи та посилання на той самий наявний файл. */
    static boolean sameFile(Path input, Path output) throws IOException {
        return input.toAbsolutePath().normalize().equals(output.toAbsolutePath().normalize())
                || (Files.exists(input) && Files.exists(output) && Files.isSameFile(input, output));
    }
}
