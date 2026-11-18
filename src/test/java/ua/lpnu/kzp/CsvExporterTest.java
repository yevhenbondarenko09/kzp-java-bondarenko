package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvExporterTest {
    @TempDir Path temporary;

    private record Other(@CsvColumn("Текст,опис") String text, @CsvColumn("Вид") PartKind kind) { }

    private static class Parent {
        @CsvColumn("База") private final String base = "root";
        private final String ignored = "secret";
    }

    private static final class Child extends Parent {
        @CsvColumn("Число") private final int count = 7;
    }

    private record Duplicate(@CsvColumn("X") String a, @CsvColumn("X") String b) { }
    private record Blank(@CsvColumn(" ") String a) { }
    private static final class StaticColumn {
        @CsvColumn("X") private static final String VALUE = "x";
    }

    @Test
    void annotationHasRuntimeRetentionAndFieldTarget() {
        assertEquals(RetentionPolicy.RUNTIME, CsvColumn.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[]{ElementType.FIELD}, CsvColumn.class.getAnnotation(Target.class).value());
        assertEquals(5, Arrays.stream(SparePartRecord.class.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(CsvColumn.class)).count());
    }

    @Test
    void exportsUnrelatedClassWithEnumNullAndEscapedHeader() throws Exception {
        Path file = temporary.resolve("other.csv");
        List<Other> input = List.of(new Other("a,\"b\"\nline\r\nend", PartKind.ORIGINAL),
                new Other(null, PartKind.REPLACEMENT));
        CsvExporter.write(file, Other.class, input);
        List<List<String>> rows = CsvParser.parseDocument(Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(List.of(List.of("Вид", "Текст,опис"),
                List.of("ORIGINAL", "a,\"b\"\nline\r\nend"), List.of("REPLACEMENT", "")), rows);
        assertEquals("a,\"b\"\nline\r\nend", input.getFirst().text());
    }

    @Test
    void exportsInheritedPrivateFieldsAndIgnoresUnannotatedFields() throws Exception {
        Path file = temporary.resolve("nested").resolve("child.csv");
        CsvExporter.write(file, Child.class, List.of(new Child()));
        assertEquals("База,Число\nroot,7\n", Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void emptyExportKeepsHeaderAndTruncatesOldRows() throws Exception {
        Path file = temporary.resolve("empty.csv");
        CsvExporter.write(file, Other.class, List.of(new Other("old data", PartKind.ORIGINAL)));
        CsvExporter.write(file, Other.class, List.of());
        assertEquals("Вид,\"Текст,опис\"\n", Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void validatesSchemaAndRecordsBeforeOverwritingFile() throws Exception {
        Path file = temporary.resolve("keep.csv");
        Files.writeString(file, "keep", StandardCharsets.UTF_8);
        assertThrows(DataStorageException.class, () -> CsvExporter.write(file, String.class, List.of()));
        assertThrows(DataStorageException.class, () -> CsvExporter.write(file, Duplicate.class, List.of()));
        assertThrows(DataStorageException.class, () -> CsvExporter.write(file, Blank.class, List.of()));
        assertThrows(DataStorageException.class, () -> CsvExporter.write(file, StaticColumn.class, List.of()));
        assertThrows(DataStorageException.class, () -> CsvExporter.write(file, Other.class,
                Arrays.asList(new Other("valid", PartKind.ORIGINAL), null)));
        assertEquals("keep", Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void fileFailurePreservesIoCauseAndPathContext() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> CsvExporter.write(temporary, Other.class, Collections.emptyList()));
        assertInstanceOf(IOException.class, exception.getCause());
        assertTrue(exception.getMessage().contains(temporary.toString()));
        DataStorageException messageOnly = new DataStorageException("format");
        assertEquals("format", messageOnly.getMessage());
        assertNull(messageOnly.getCause());
    }

    @Test
    void rejectsNullApiArguments() {
        Path file = temporary.resolve("null.csv");
        assertThrows(NullPointerException.class, () -> CsvExporter.write(null, Other.class, List.of()));
        assertThrows(NullPointerException.class, () -> CsvExporter.write(file, null, List.of()));
        assertThrows(NullPointerException.class, () -> CsvExporter.write(file, Other.class, null));
    }
}
