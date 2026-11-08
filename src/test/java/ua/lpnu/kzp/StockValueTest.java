package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StockValueTest {
    @Test
    void exposesComponentsAndHasValueEquality() {
        StockValue value = new StockValue(4, 12.5);
        assertEquals(4, value.stock());
        assertEquals(12.5, value.value());
        assertEquals(value, new StockValue(4, 12.5));
        assertEquals(value.hashCode(), new StockValue(4, 12.5).hashCode());
        assertNotEquals(value, new StockValue(5, 12.5));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidValue(double value) {
        assertThrows(IllegalArgumentException.class, () -> new StockValue(0, value));
    }

    @Test
    void rejectsNegativeStockAndAcceptsZero() {
        assertThrows(IllegalArgumentException.class, () -> new StockValue(-1, 0));
        assertEquals(new StockValue(0, 0), new StockValue(0, -0.0));
    }
}
