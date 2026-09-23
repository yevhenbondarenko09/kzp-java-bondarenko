# Project Rules and Cross-Platform Programming (KZP) Course Standards

1. Target Java 21+; explicitly ensure compatibility for any modern language features[cite: 1].
2. Do NOT concatenate strings using '+'; exclusively use 'formatted()', 'String.format()', or 'printf()'[cite: 1].
3. Always format numbers and strings using the neutral 'Locale.ROOT' without relying on the host OS locale[cite: 1].
4. Always explicitly specify 'StandardCharsets.UTF_8' for all file and stream I/O operations[cite: 1].
5. Construct file system paths strictly via 'Path.of()' instead of hardcoding '/' or '\\' delimiters[cite: 1].
6. Implement unit tests using JUnit 5 inside 'src/test/java'; use '@ParameterizedTest' for validating multiple input records and edge cases[cite: 1].
7. Source code must pass SpotBugs static analysis during the Maven 'verify' phase without unverified rule suppressions[cite: 1].
8. Organize changes into small, atomic Git commits linked directly to corresponding GitHub Issues (e.g., 'Closes #N')[cite: 1].
9. Remind the student to document AI assistance in the mandatory "Academic Integrity" section of 'REPORT.md'[cite: 1].