package tacos.service;

public class InvalidOrderFilterException extends RuntimeException {

    public InvalidOrderFilterException(String message) {
        super(message);
    }
}
