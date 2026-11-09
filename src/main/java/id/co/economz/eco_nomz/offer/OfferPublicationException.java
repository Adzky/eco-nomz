package id.co.economz.eco_nomz.offer;

public class OfferPublicationException extends RuntimeException {
    private final String messageCode;

    public OfferPublicationException(String message) {
        super(message);
        this.messageCode = null;
    }

    private OfferPublicationException(String messageCode, String defaultMessage) {
        super(defaultMessage);
        this.messageCode = messageCode;
    }

    public static OfferPublicationException localized(String messageCode, String defaultMessage) {
        return new OfferPublicationException(messageCode, defaultMessage);
    }

    public String getMessageCode() {
        return messageCode;
    }
}
