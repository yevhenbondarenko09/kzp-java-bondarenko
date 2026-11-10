package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MainTest {
    @TempDir
    Path temporary;

    @Test
    void writesIdenticalUtf8ReportAndCreatesOutputDirectories() throws IOException {
        Path input = temporary.resolve("вхід з пробілами.csv");
        Path output = temporary.resolve("новий каталог").resolve("звіт.txt");
        Files.writeString(input, "SKU;Фільтр;2;125.50;Постачальник\n", StandardCharsets.UTF_8);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int status = run(new String[]{"--input", input.toString(), "--output", output.toString()}, stdout, stderr);
        assertEquals(0, status);
        assertEquals("", stderr.toString(StandardCharsets.UTF_8));
        String report = Files.readString(output, StandardCharsets.UTF_8);
        assertEquals(report, stdout.toString(StandardCharsets.UTF_8));
        assertTrue(report.contains("251.00 грн"));
        assertTrue(report.contains("Фільтр"));
    }

    @Test
    void helpAndVersionDoNotNeedInputFile() {
        for (String option : new String[]{"--help", "--version"}) {
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            assertEquals(0, run(new String[]{option}, stdout, stderr));
            assertTrue(stdout.toString(StandardCharsets.UTF_8).contains(
                    "--version".equals(option) ? System.getProperty("project.version") : "--input"));
            assertEquals("", stderr.toString(StandardCharsets.UTF_8));
        }
    }

    @Test
    void versionAndHelpMatchMavenBuildMetadata() {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        assertEquals(0, run(new String[]{"--version"}, stdout, stderr));
        assertEquals(System.getProperty("project.version"),
                stdout.toString(StandardCharsets.UTF_8).strip());
        stdout.reset();
        assertEquals(0, run(new String[]{"--help"}, stdout, stderr));
        assertTrue(stdout.toString(StandardCharsets.UTF_8).contains(System.getProperty("project.jarName")));
        assertEquals("", stderr.toString(StandardCharsets.UTF_8));
    }

    @Test
    void parserReturnsDefaultAndExplicitPathsWithoutReadingFiles() {
        ArgumentParser.Options defaults = ArgumentParser.parse(new String[]{});
        assertEquals(Path.of("data", "input.csv"), defaults.input());
        assertEquals(Path.of("out", "report.txt"), defaults.output());
        assertFalse(defaults.help());
        assertFalse(defaults.version());
        ArgumentParser.Options explicit = ArgumentParser.parse(new String[]{
            "--output", "custom-output.txt", "--input", "custom-input.csv"});
        assertEquals(Path.of("custom-input.csv"), explicit.input());
        assertEquals(Path.of("custom-output.txt"), explicit.output());
    }

    @ParameterizedTest
    @ValueSource(strings = {"--unknown", "--input", "--output"})
    void invalidOptionsReturnUsageError(String option) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        assertEquals(2, run(new String[]{option}, stdout, stderr));
        assertEquals("", stdout.toString(StandardCharsets.UTF_8));
        assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("--help"));
    }

    @Test
    void rejectsDuplicatesAndMissingValues() {
        String[][] arguments = {
            {"--input", "--output", "out.txt"},
            {"--input", "a", "--input", "b"},
            {"--output", "a", "--output", "b"},
            {"--input", " "}, {"--help", "--version"}, {"--input", "\u0000"}
        };
        for (String[] args : arguments) {
            assertEquals(2, run(args, new ByteArrayOutputStream(), new ByteArrayOutputStream()));
        }
    }

    @Test
    void missingInputReturnsFileErrorAndDoesNotCreateReport() {
        Path destination = temporary.resolve("report.txt");
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        assertEquals(1, run(new String[]{"--input", temporary.resolve("missing.csv").toString(),
                "--output", destination.toString()}, new ByteArrayOutputStream(), stderr));
        assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("Помилка роботи з файлами"));
        assertFalse(Files.exists(destination));
    }

    @Test
    void refusesToOverwriteInput() throws IOException {
        Path input = temporary.resolve("input.csv");
        String original = "SKU;Деталь;2;3;Постачальник";
        Files.writeString(input, original, StandardCharsets.UTF_8);
        assertEquals(2, run(new String[]{"--input", input.toString(), "--output", input.toString()},
                new ByteArrayOutputStream(), new ByteArrayOutputStream()));
        assertEquals(original, Files.readString(input, StandardCharsets.UTF_8));
    }

    @Test
    void reportsInvalidUtf8AndOutputDirectoryAsErrors() throws IOException {
        Path input = temporary.resolve("input.csv");
        Files.write(input, new byte[]{(byte) 0xC3, (byte) 0x28});
        assertEquals(1, run(new String[]{"--input", input.toString()},
                new ByteArrayOutputStream(), new ByteArrayOutputStream()));
        Files.writeString(input, "a;b;1;2;s", StandardCharsets.UTF_8);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        assertEquals(1, run(new String[]{"--input", input.toString(), "--output", temporary.toString()},
                stdout, new ByteArrayOutputStream()));
        assertEquals("", stdout.toString(StandardCharsets.UTF_8));
    }

    private static int run(String[] args, ByteArrayOutputStream stdout, ByteArrayOutputStream stderr) {
        try (PrintStream output = new PrintStream(stdout, true, StandardCharsets.UTF_8);
                PrintStream errors = new PrintStream(stderr, true, StandardCharsets.UTF_8)) {
            return Main.run(args, output, errors);
        }
    }
}
