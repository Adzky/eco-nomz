package id.co.economz.eco_nomz.auth;

public class PasswordTooLongException extends RuntimeException {
    public PasswordTooLongException() {
        super("Password must be no more than 72 UTF-8 bytes.");
    }
}
