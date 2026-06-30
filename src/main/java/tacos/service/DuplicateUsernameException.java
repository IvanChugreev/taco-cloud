package tacos.service;

public class DuplicateUsernameException extends RuntimeException {

    public DuplicateUsernameException(String username) {
        super("Username '" + username + "' is already registered");
    }
}
