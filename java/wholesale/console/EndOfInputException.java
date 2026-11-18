package wholesale.console;

public class EndOfInputException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public EndOfInputException() {
        super("Ввод прерван");
    }
}
