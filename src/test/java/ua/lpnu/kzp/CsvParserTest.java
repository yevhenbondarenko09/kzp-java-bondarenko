package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CsvParserTest {
    @Test
    void preservesEscapedQuotesCommasMultilineAndWhitespace() throws DataStorageException {
        assertEquals(List.of(List.of(" A ", "кома,лапка\"", "рядок\r\nще\nкінець\rкрай", "")),
                CsvParser.parseDocument(" A ,\"кома,лапка\"\"\",\"рядок\r\nще\nкінець\rкрай\",\"\""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n", "\r"})
    void acceptsRecordSeparatorsAndOptionalFinalSeparator(String separator) throws DataStorageException {
        String text = String.join(separator, "a,b", "c,d");
        assertEquals(List.of(List.of("a", "b"), List.of("c", "d")), CsvParser.parseDocument(text));
        assertEquals(CsvParser.parseDocument(text),
                CsvParser.parseDocument("%s%s".formatted(text, separator)));
    }

    @Test
    void keepsEmptyFieldsIncludingQuotedFinalFieldWithoutNewline() throws DataStorageException {
        assertEquals(List.of(List.of("")), CsvParser.parseDocument("\"\""));
        assertEquals(List.of(List.of("", "", "")), CsvParser.parseDocument(",,"));
        assertEquals(List.of(List.of("a", "")), CsvParser.parseDocument("a,\"\""));
        assertEquals(List.of(List.of("a", "")), CsvParser.parseDocument("a,"));
        assertEquals(List.of(List.of("")), CsvParser.parseDocument("\n"));
        assertTrue(CsvParser.parseDocument("").isEmpty());
        assertTrue(CsvParser.parseDocument("\uFEFF").isEmpty());
        assertEquals(List.of(List.of("a", "b")), CsvParser.parseDocument("\uFEFFa,b\n"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"broken", "a\"b,c", "\"a\"b,c", "\"a\" ,b", "a,\"b\"\"", "\"a\"\""})
    void rejectsMalformedQuotes(String text) {
        assertThrows(DataStorageException.class, () -> CsvParser.parseDocument(text));
    }

    @Test
    void rejectsNullAndReturnsDeeplyUnmodifiableResult() throws DataStorageException {
        assertThrows(DataStorageException.class, () -> CsvParser.parseDocument(null));
        List<List<String>> parsed = CsvParser.parseDocument("a,b");
        assertThrows(UnsupportedOperationException.class, () -> parsed.clear());
        assertThrows(UnsupportedOperationException.class, () -> parsed.getFirst().clear());
    }
}
