package id.co.economz.eco_nomz.order;

public class OrderConflictException extends RuntimeException {
    private final String messageCode;
    private final Object[] messageArguments;

    public OrderConflictException(String message) {
        super(message);
        this.messageCode = null;
        this.messageArguments = new Object[0];
    }

    private OrderConflictException(String messageCode, String defaultMessage, Object[] messageArguments) {
        super(defaultMessage);
        this.messageCode = messageCode;
        this.messageArguments = messageArguments.clone();
    }

    public static OrderConflictException localized(
            String messageCode,
            String defaultMessage,
            Object... messageArguments
    ) {
        return new OrderConflictException(messageCode, defaultMessage, messageArguments);
    }

    public String getMessageCode() {
        return messageCode;
    }

    public Object[] getMessageArguments() {
        return messageArguments.clone();
    }
}
