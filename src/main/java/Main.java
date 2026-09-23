package ua.lpnu.kzp;

/**
 * Entry point for application infrastructure and build verification.
 */
public final class Main {

    private Main() {
    }

    /**
     * Main application entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        if (args.length > 0 && "--version".equals(args[0])) {
            System.out.printf("1.0.0%n");
            return;
        }
        System.out.printf("Hello from KZP project!%n");
    }
}