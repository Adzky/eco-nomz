package id.co.economz.eco_nomz.payment;

public class MidtransIntegrationException extends RuntimeException {
    public MidtransIntegrationException(String message) {
        super(message);
    }

    public MidtransIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
