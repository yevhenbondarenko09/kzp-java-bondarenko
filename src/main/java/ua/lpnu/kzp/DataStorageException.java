package ua.lpnu.kzp;

/** Повідомляє про помилки формату, значень, рефлексії або файлових операцій CSV. */
public final class DataStorageException extends Exception {
    private static final long serialVersionUID = 1L;

    /** Створює помилку формату або контракту.
     * @param message опис помилки
     */
    public DataStorageException(String message) { super(message); }

    /** Зберігає контекст операції та її першопричину.
     * @param message опис операції
     * @param cause першопричина
     */
    public DataStorageException(String message, Throwable cause) { super(message, cause); }
}
