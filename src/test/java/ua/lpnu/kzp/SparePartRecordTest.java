package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SparePartRecordTest {
    @TempDir Path temporary;

    private static List<SparePartRecord> controlRecords() {
        return List.of(new SparePartRecord("A", "Фільтр", 4, 100, "Bosch"),
                new SparePartRecord("B", "Колодки, передні", 5, 300.25, "Brembo"),
                new SparePartRecord("C", "Свічка \"Іскра\"", 0, 0, "Denso"),
                new SparePartRecord("D", "Лампа\nфари", 2, 50.5, "Osram"),
                new SparePartRecord("E", "Ремінь", 6, 200, "Bosch"));
    }

    @Test
    void roundTripMatchesFixtureAllFieldsOrderAndRepositorySearch() throws Exception {
        Repository<SparePartRecord> repository = new Repository<>();
        controlRecords().forEach(repository::add);
        Path file = temporary.resolve("деталі.csv");
        CsvExporter.write(file, SparePartRecord.class, repository.all());
        String text = Files.readString(file, StandardCharsets.UTF_8);
        String fixture = Files.readString(Path.of("src", "test", "resources", "lab05", "input.csv"),
                StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertEquals(fixture, text);
        assertEquals(7, text.lines().count());
        List<SparePartRecord> restored = SparePartRecord.read(file);
        assertEquals(controlRecords(), restored);
        assertEquals(controlRecords().hashCode(), restored.hashCode());
        assertEquals(List.of(controlRecords().get(2)), repository.find(part -> part.article().equals("C")));
        assertThrows(UnsupportedOperationException.class, () -> restored.clear());
    }

    @Test
    void equalityChecksValuesBeyondArticleAndPreservesDuplicateArticles() throws Exception {
        SparePartRecord first = new SparePartRecord("A", " Перший ", 0, 0.1, " B ");
        SparePartRecord second = new SparePartRecord("A", "Інший", 2, Double.MIN_VALUE, "M");
        assertNotEquals(first, second);
        List<SparePartRecord> input = List.of(first, first, second);
        Path file = temporary.resolve("duplicates.csv");
        CsvExporter.write(file, SparePartRecord.class, input);
        assertEquals(input, SparePartRecord.read(file));
    }

    @Test
    void fromCsvSupportsOneMultilineRecordAndProjectionKeepsOldModelUntouched() throws Exception {
        assertEquals(controlRecords().get(3), SparePartRecord.fromCsv("D,Osram,\"Лампа\nфари\",50.5,2"));
        SparePart old = new ReplacementPart("A", "Фільтр", 4, 100, "Bosch");
        assertEquals(controlRecords().getFirst(), SparePartRecord.fromPart(old));
        assertEquals(PartKind.REPLACEMENT, old.kind());
        assertEquals(5, old.warrantyCost());
        assertThrows(NullPointerException.class, () -> SparePartRecord.fromPart(null));
    }

    @Test
    void handlesEmptyFileHeaderOnlyAndBom() throws Exception {
        Path file = temporary.resolve("empty.csv");
        Files.writeString(file, "", StandardCharsets.UTF_8);
        assertTrue(SparePartRecord.read(file).isEmpty());
        CsvExporter.write(file, SparePartRecord.class, List.of());
        assertTrue(SparePartRecord.read(file).isEmpty());
        String header = Files.readString(file, StandardCharsets.UTF_8);
        Files.writeString(file, "\uFEFF%sA,Bosch,Фільтр,100,4\r\n".formatted(header), StandardCharsets.UTF_8);
        assertEquals(List.of(controlRecords().getFirst()), SparePartRecord.read(file));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "A,B,C,1", "A,B,C,1,2,extra", "A,B,C,1,abc", "A,B,C,no,1",
        "A,B,C,NaN,1", "A,B,C,Infinity,1", "A,B,C,-1,1", "A,B,C,1,-1", "A,B,C,1,2147483648",
        ",B,C,1,1", "A,,C,1,1", "A,B, ,1,1", "A,B,C,1,1\nD,E,F,2,2", "A,B,\"broken,1,1"})
    void rejectsInvalidRecord(String csv) {
        assertThrows(DataStorageException.class, () -> SparePartRecord.fromCsv(csv));
    }

    @Test
    void validatesConstructorAndNullFields() {
        assertThrows(IllegalArgumentException.class, () -> new SparePartRecord(null, "N", 1, 1, "M"));
        assertThrows(IllegalArgumentException.class, () -> new SparePartRecord("A", null, 1, 1, "M"));
        assertThrows(IllegalArgumentException.class, () -> new SparePartRecord("A", "N", 1, 1, null));
        assertThrows(DataStorageException.class, () -> SparePartRecord.fromFields(null));
        assertThrows(DataStorageException.class,
                () -> SparePartRecord.fromFields(java.util.Arrays.asList("A", "M", null, "1", "1")));
    }

    @Test
    void invalidNumberPreservesCauseAndMalformedFileReportsRecordNumber() throws Exception {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> SparePartRecord.fromCsv("A,B,C,1,abc"));
        assertInstanceOf(NumberFormatException.class, exception.getCause());
        Path fixture = Path.of("src", "test", "resources", "lab05", "invalid.csv");
        exception = assertThrows(DataStorageException.class, () -> SparePartRecord.read(fixture));
        assertTrue(exception.getMessage().contains("CSV-запис 2"));
        assertInstanceOf(NumberFormatException.class, exception.getCause().getCause());
        Path file = temporary.resolve("header.csv");
        Files.writeString(file, "wrong,header\n", StandardCharsets.UTF_8);
        assertThrows(DataStorageException.class, () -> SparePartRecord.read(file));
    }

    @Test
    void wrapsMissingFileDirectoryAndInvalidUtf8WithIoCause() throws IOException {
        for (Path path : List.of(temporary.resolve("missing.csv"), temporary)) {
            DataStorageException exception = assertThrows(DataStorageException.class,
                    () -> SparePartRecord.read(path));
            assertInstanceOf(IOException.class, exception.getCause());
        }
        Path invalidUtf8 = temporary.resolve("encoding.csv");
        Files.write(invalidUtf8, new byte[]{(byte) 0xc3, 0x28});
        assertInstanceOf(IOException.class, assertThrows(DataStorageException.class,
                () -> SparePartRecord.read(invalidUtf8)).getCause());
    }

    @Test
    @ResourceLock("java.util.Locale.default")
    void numericRoundTripDoesNotDependOnDefaultLocale() throws Exception {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            Path file = temporary.resolve("locale.csv");
            CsvExporter.write(file, SparePartRecord.class, controlRecords());
            assertEquals(controlRecords(), SparePartRecord.read(file));
            assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("300.25"));
        } finally {
            Locale.setDefault(previous);
        }
    }
}
