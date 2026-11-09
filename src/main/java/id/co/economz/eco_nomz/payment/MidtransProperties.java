package id.co.economz.eco_nomz.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.midtrans")
public class MidtransProperties {
    private String serverKey = "";
    private String clientKey = "";
    private String baseUrl = "https://app.sandbox.midtrans.com";
    private String publicBaseUrl = "http://localhost:8080";

    public String getServerKey() {
        return serverKey;
    }

    public void setServerKey(String serverKey) {
        this.serverKey = serverKey;
    }

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public void requireServerKey() {
        if (serverKey == null || serverKey.isBlank()) {
            throw new MidtransIntegrationException("Midtrans Server Key is not configured.");
        }
    }
}
