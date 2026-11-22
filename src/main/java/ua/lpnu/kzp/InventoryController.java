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
                        "Склад автозапчастин — лабораторна № 5, варіант 4",
                        "Запуск: java -jar target/%s [--input <файл>] [--output <файл>] [--export <файл>]"
                                .formatted(ApplicationInfo.read("jarName")),
                        "  --input    CSV UTF-8; типово data/input.csv",
                        "  --output   Звіт UTF-8; типово out/report.txt",
                        "  --export   Окремий CSV UTF-8 прийнятих деталей із заголовком",
                        "  --help     Довідка",
                        "  --version  Версія програми",
                        "Формат: sku;name;stock;unitPrice;supplier[;kind]",
                        "kind: ORIGINAL (типово) або REPLACEMENT", ""));
                return 0;
            }
            if (options.version()) {
                output.printf(Locale.ROOT, "%s%n", ApplicationInfo.read("version"));
                return 0;
            }
            if (FileReport.sameFile(options.input(), options.output())) {
                throw new IllegalArgumentException("Вхідний і вихідний файли мають бути різними");
            }
            if (options.export() != null && (FileReport.sameFile(options.input(), options.export())
                    || FileReport.sameFile(options.output(), options.export()))) {
                throw new IllegalArgumentException("CSV-експорт, вхід і звіт мають бути різними файлами");
            }
            InventoryReport.Analysis analysis = InventoryReport.analyze(FileReport.readLines(options.input()));
            if (options.export() != null) {
                CsvExporter.write(options.export(), SparePartRecord.class,
                        analysis.parts().stream().map(SparePartRecord::fromPart).toList());
            }
            String report = InventoryReport.format(analysis);
            FileReport.write(options.output(), report);
            output.print(report);
            return 0;
        } catch (DataStorageException exception) {
            errors.printf(Locale.ROOT, "Помилка CSV: %s%n", exception.getMessage());
            return 1;
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
