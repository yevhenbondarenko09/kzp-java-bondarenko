package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;

/** Розбирає CSV-документ із комами, лапками й багаторядковими полями. */
public final class CsvParser {
    private CsvParser() { }

    /**
     * Розбирає весь UTF-8-текст без зміни значень усередині полів.
     * Межі записів: LF, CRLF або CR; BOM дозволено тільки на початку документа.
     * @param text текст CSV
     * @return незмінні записи й поля; порожній документ дає порожній список
     * @throws DataStorageException якщо текст null або лапки пошкоджені
     */
    public static List<List<String>> parseDocument(String text) throws DataStorageException {
        if (text == null) {
            throw new DataStorageException("CSV-документ не може бути null");
        }
        List<List<String>> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean closed = false;
        int start = text.startsWith("\uFEFF") ? 1 : 0;
        for (int index = start; index < text.length(); index++) {
            char symbol = text.charAt(index);
            if (quoted) {
                if (symbol == '"') {
                    if (index + 1 < text.length() && text.charAt(index + 1) == '"') {
                        current.append('"');
                        index++;
                    } else {
                        quoted = false;
                        closed = true;
                    }
                } else {
                    current.append(symbol);
                }
            } else if (symbol == ',' || symbol == '\r' || symbol == '\n') {
                fields.add(current.toString());
                current.setLength(0);
                closed = false;
                if (symbol != ',') {
                    records.add(List.copyOf(fields));
                    fields.clear();
                    if (symbol == '\r' && index + 1 < text.length() && text.charAt(index + 1) == '\n') {
                        index++;
                    }
                }
            } else if (closed) {
                throw new DataStorageException("Символ після закритих CSV-лапок");
            } else if (symbol == '"') {
                if (!current.isEmpty()) {
                    throw new DataStorageException("Лапка всередині неекранованого CSV-поля");
                }
                quoted = true;
            } else {
                current.append(symbol);
            }
        }
        if (quoted) {
            throw new DataStorageException("Незакриті CSV-лапки");
        }
        // closed враховує останнє порожнє quoted-поле без завершального перенесення.
        if (closed || !fields.isEmpty() || !current.isEmpty()) {
            fields.add(current.toString());
            records.add(List.copyOf(fields));
        }
        return List.copyOf(records);
    }
}
