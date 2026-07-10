package tacos.service;

public class TacoUnavailableException extends RuntimeException {

    public TacoUnavailableException() {
        super("One or more tacos are no longer available for ordering");
    }
}
