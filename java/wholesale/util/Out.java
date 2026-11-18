package wholesale.util;

import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public final class Out {
    private static PrintStream target = new PrintStream(
            new BufferedOutputStream(new FileOutputStream(FileDescriptor.out)), false, StandardCharsets.UTF_8);

    private Out() {
    }

    public static void redirect(PrintStream stream) {
        target = stream;
    }

    public static void print(String text) {
        target.print(text);
    }

    public static void println(String text) {
        target.print(text);
        target.print("\n");
    }

    public static void println() {
        target.print("\n");
    }

    public static void flush() {
        target.flush();
    }
}
