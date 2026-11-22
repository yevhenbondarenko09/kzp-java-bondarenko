package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class RepositoryTest {
    @Test
    void snapshotIsUnmodifiableAndIndependentOfLaterAdds() {
        Repository<String> repository = new Repository<>();
        repository.add("A");
        List<String> snapshot = repository.all();
        repository.add("A");
        assertEquals(List.of("A"), snapshot);
        assertEquals(List.of("A", "A"), repository.all());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add("B"));
    }

    @Test
    void findsUsingSupertypePredicateAndKeepsDuplicates() {
        Repository<Integer> repository = new Repository<>();
        repository.add(1);
        repository.add(2);
        repository.add(2);
        Predicate<Number> condition = number -> number.intValue() > 1;
        assertEquals(List.of(2, 2), repository.find(condition));
        assertThrows(UnsupportedOperationException.class, () -> repository.find(condition).clear());
        assertEquals(List.of(1, 2, 2), repository.all());
    }

    @Test
    void handlesEmptyRepositoryAndRejectsNulls() {
        Repository<String> repository = new Repository<>();
        assertTrue(repository.all().isEmpty());
        assertTrue(repository.find(item -> true).isEmpty());
        assertThrows(NullPointerException.class, () -> repository.add(null));
        assertThrows(NullPointerException.class, () -> repository.find(null));
    }
}
