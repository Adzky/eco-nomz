package id.co.economz.eco_nomz.payment;

public interface MidtransGateway {
    MidtransSnapResponse createTransaction(Payment payment);
}
