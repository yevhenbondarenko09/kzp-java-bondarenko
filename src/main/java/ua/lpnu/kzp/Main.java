package ua.lpnu.kzp;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

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
        return new InventoryController().run(args, output, errors);
    }
}
