package id.co.economz.eco_nomz.payment;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MidtransNotificationController {
    private final MidtransNotificationService notificationService;

    public MidtransNotificationController(MidtransNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping(
            path = "/payments/midtrans/notifications",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, String> notification(@RequestBody MidtransNotification notification) {
        notificationService.process(notification);
        return Map.of("status", "ok");
    }
}
