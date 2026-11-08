package ua.lpnu.kzp;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Консольна програма обліку складу автозапчастин, варіант 4. */
public final class Main {
    private Main() {
    }

    /**
     * Запускає програму; помилки параметрів і файлів повертають ненульовий код.
     *
     * @param args параметри --help, --version, --input та --output
     */
    public static void main(String[] args) {
        final int status;
        try (PrintStream output = new PrintStream(System.out, true, StandardCharsets.UTF_8);
                PrintStream errors = new PrintStream(System.err, true, StandardCharsets.UTF_8)) {
            status = run(args, output, errors);
        }
        if (status != 0) {
            System.exit(status);
        }
    }

    /** Виконує запуск з окремими потоками виводу для перевірки CLI. */
    static int run(String[] args, PrintStream output, PrintStream errors) {
        if (args.length == 1 && "--help".equals(args[0])) {
            output.printf(Locale.ROOT, String.join("%n",
                    "Склад автозапчастин — лабораторна № 1, варіант 4",
                    "Запуск: java -jar target/lab01-1.0.0-all.jar [--input <файл>] [--output <файл>]",
                    "  --input    CSV UTF-8; типово data/input.csv",
                    "  --output   Звіт UTF-8; типово out/report.txt",
                    "  --help     Довідка",
                    "  --version  Версія програми",
                    "Формат: sku;name;stock;unitPrice;supplier", ""));
            return 0;
        }
        if (args.length == 1 && "--version".equals(args[0])) {
            output.printf(Locale.ROOT, "1.0.0%n");
            return 0;
        }
        Path input = Path.of("data", "input.csv");
        Path destination = Path.of("out", "report.txt");
        Set<String> seen = new HashSet<>();
        try {
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
            if (FileReport.sameFile(input, destination)) {
                throw new IllegalArgumentException("Вхідний і вихідний файли мають бути різними");
            }
            String report = InventoryReport.generate(FileReport.readLines(input));
            FileReport.write(destination, report);
            output.print(report);
            return 0;
        } catch (InvalidPathException exception) {
            errors.printf(Locale.ROOT, "Помилка шляху: %s%n", exception.getMessage());
            return 2;
        } catch (IllegalArgumentException exception) {
            errors.printf(Locale.ROOT, "Помилка параметрів: %s. Скористайтеся --help.%n", exception.getMessage());
            return 2;
        } catch (IOException | SecurityException exception) {
            errors.printf(Locale.ROOT, "Помилка роботи з файлами: %s%n", exception.getMessage());
            return 1;
        }
    }
}
