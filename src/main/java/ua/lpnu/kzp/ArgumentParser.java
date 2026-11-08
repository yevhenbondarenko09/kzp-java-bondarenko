package ua.lpnu.kzp;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Перевіряє параметри до запуску файлової обробки. */
final class ArgumentParser {
    private ArgumentParser() {
    }

    record Options(Path input, Path output, boolean help, boolean version) {
    }

    static Options parse(String[] args) {
        if (args.length == 1 && "--help".equals(args[0])) {
            return new Options(null, null, true, false);
        }
        if (args.length == 1 && "--version".equals(args[0])) {
            return new Options(null, null, false, true);
        }
        Path input = Path.of("data", "input.csv");
        Path destination = Path.of("out", "report.txt");
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < args.length; index++) {
            String option = args[index];
            if (!"--input".equals(option) && !"--output".equals(option)) {
                throw new IllegalArgumentException("Невідомий параметр: %s".formatted(option));
            }
            if (!seen.add(option)) {
                throw new IllegalArgumentException("Параметр повторено: %s".formatted(option));
            }
            if (index + 1 >= args.length || args[index + 1].startsWith("--")
                    || args[index + 1].isBlank()) {
                throw new IllegalArgumentException("Для %s потрібно вказати шлях".formatted(option));
            }
            Path value = Path.of(args[++index]);
            if ("--input".equals(option)) {
                input = value;
            } else {
                destination = value;
            }
        }
        return new Options(input, destination, false, false);
    }
}
