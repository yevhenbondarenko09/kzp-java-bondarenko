package ua.lpnu.kzp;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.InvalidPathException;
import java.util.Locale;

/** Керує запуском звіту, довідкою та обробкою помилок. */
final class InventoryController {
    int run(String[] args, PrintStream output, PrintStream errors) {
        try {
            ArgumentParser.Options options = ArgumentParser.parse(args);
            if (options.help()) {
                output.printf(Locale.ROOT, String.join("%n",
                        "Склад автозапчастин — лабораторна № 2, варіант 4",
                        "Запуск: java -jar target/%s [--input <файл>] [--output <файл>]"
                                .formatted(ApplicationInfo.read("jarName")),
                        "  --input    CSV UTF-8; типово data/input.csv",
                        "  --output   Звіт UTF-8; типово out/report.txt",
                        "  --help     Довідка",
                        "  --version  Версія програми",
                        "Формат: sku;name;stock;unitPrice;supplier", ""));
                return 0;
            }
            if (options.version()) {
                output.printf(Locale.ROOT, "%s%n", ApplicationInfo.read("version"));
                return 0;
            }
            if (FileReport.sameFile(options.input(), options.output())) {
                throw new IllegalArgumentException("Вхідний і вихідний файли мають бути різними");
            }
            String report = InventoryReport.generate(FileReport.readLines(options.input()));
            FileReport.write(options.output(), report);
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
