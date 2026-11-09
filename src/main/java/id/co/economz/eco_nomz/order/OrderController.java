package id.co.economz.eco_nomz.order;

import jakarta.validation.Valid;
import id.co.economz.eco_nomz.payment.MidtransCheckoutService;
import id.co.economz.eco_nomz.payment.MidtransIntegrationException;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrderController {
    private final ReservationService reservationService;
    private final PaymentLifecycleService paymentLifecycleService;
    private final MidtransCheckoutService midtransCheckoutService;
    private final MessageSource messageSource;

    public OrderController(
            ReservationService reservationService,
            PaymentLifecycleService paymentLifecycleService,
            MidtransCheckoutService midtransCheckoutService,
            MessageSource messageSource
    ) {
        this.reservationService = reservationService;
        this.paymentLifecycleService = paymentLifecycleService;
        this.midtransCheckoutService = midtransCheckoutService;
        this.messageSource = messageSource;
    }

    @PostMapping("/customer/orders")
    public String reserve(
            @Valid @ModelAttribute("reservationRequest") ReservationRequest request,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors() || request.getOfferId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reservation details are invalid.");
        }
        try {
            CustomerOrder order = reservationService.reserve(
                    authentication.getName(),
                    request.getOfferId(),
                    request.getQuantity()
            );
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    message("orders.reserved", order.getQuantity())
            );
            return "redirect:/customer/orders";
        } catch (OrderConflictException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", conflictMessage(exception));
            return "redirect:/offers/" + request.getOfferId();
        }
    }

    @GetMapping("/customer/orders")
    public String customerOrders(
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model
    ) {
        model.addAttribute("orders", reservationService.findCustomerOrders(authentication.getName(), page));
        return "orders/customer-list";
    }

    @PostMapping("/customer/orders/{id}/cancel")
    public String cancel(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            reservationService.cancel(authentication.getName(), id);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    message("orders.cancelledSuccessfully")
            );
        } catch (OrderConflictException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", conflictMessage(exception));
        }
        return "redirect:/customer/orders";
    }

    @PostMapping("/customer/orders/{id}/checkout")
    public String checkout(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            return "redirect:" + midtransCheckoutService.createCheckout(authentication.getName(), id);
        } catch (MidtransIntegrationException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    message("orders.checkoutUnavailable")
            );
        } catch (OrderConflictException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", conflictMessage(exception));
        }
        return "redirect:/customer/orders";
    }

    @GetMapping("/owner/orders")
    public String ownerOrders(
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model
    ) {
        model.addAttribute("orders", reservationService.findOwnerOrders(authentication.getName(), page));
        return "orders/owner-list";
    }

    @PostMapping("/owner/orders/verify-pickup")
    public String verifyPickup(
            @Valid @ModelAttribute("pickupVerificationRequest") PickupVerificationRequest request,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pickup code is invalid.");
        }
        try {
            paymentLifecycleService.verifyPickup(authentication.getName(), request.getPickupCode().trim());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    message("orders.pickupVerified")
            );
        } catch (OrderConflictException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", conflictMessage(exception));
        }
        return "redirect:/owner/orders";
    }

    private String conflictMessage(OrderConflictException exception) {
        if (exception.getMessageCode() == null) {
            return exception.getMessage();
        }
        return message(exception.getMessageCode(), exception.getMessageArguments());
    }

    private String message(String code, Object... arguments) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, arguments, locale);
    }
}
