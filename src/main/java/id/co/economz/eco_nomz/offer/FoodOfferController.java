package id.co.economz.eco_nomz.offer;

import java.math.BigDecimal;
import jakarta.validation.Valid;
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
public class FoodOfferController {
    private final FoodOfferService foodOfferService;

    public FoodOfferController(FoodOfferService foodOfferService) {
        this.foodOfferService = foodOfferService;
    }

    @GetMapping({"/", "/offers"})
    public String publicOffers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "NEWEST") OfferSort sort,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "1") int minQuantity,
            Model model
    ) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must not be negative.");
        }
        if (minPrice != null && minPrice.signum() < 0
                || maxPrice != null && maxPrice.signum() < 0
                || minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price filters are invalid.");
        }
        if (minPrice != null && minPrice.precision() - minPrice.scale() > 10
                || maxPrice != null && maxPrice.precision() - maxPrice.scale() > 10
                || minPrice != null && minPrice.scale() > 2
                || maxPrice != null && maxPrice.scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prices must fit the supported IDR range.");
        }
        if (minQuantity < 1 || minQuantity > 1_000_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum quantity is out of range.");
        }
        if (q != null && q.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search text must be 100 characters or fewer.");
        }
        var offers = foodOfferService.findAvailableOffers(page, sort, q, minPrice, maxPrice, minQuantity);
        model.addAttribute("offers", offers);
        model.addAttribute("sort", sort);
        model.addAttribute("q", q);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("minQuantity", minQuantity);
        return "offers/list";
    }

    @GetMapping("/offers/{id}")
    public String publicOffer(@PathVariable Long id, Model model) {
        model.addAttribute("offer", foodOfferService.getAvailableOffer(id));
        return "offers/detail";
    }

    @GetMapping("/owner/offers")
    public String ownerOffers(
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model
    ) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must not be negative.");
        }
        model.addAttribute("offers", foodOfferService.findOwnerOffers(authentication.getName(), page));
        return "owner/offers";
    }

    @GetMapping("/owner/offers/new")
    public String newOffer(Authentication authentication, Model model) {
        prepareOfferForm(model, authentication.getName(), null);
        return "owner/offer-form";
    }

    @PostMapping("/owner/offers")
    public String createOffer(
            @Valid @ModelAttribute("offerForm") FoodOfferForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepareInvalidOfferForm(model, authentication.getName(), null);
            return "owner/offer-form";
        }
        try {
            foodOfferService.createOffer(authentication.getName(), form);
        } catch (OfferPublicationException exception) {
            bindingResult.reject(exception.getMessageCode() == null
                    ? "offer.publication"
                    : exception.getMessageCode());
            prepareInvalidOfferForm(model, authentication.getName(), null);
            return "owner/offer-form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "offer.created");
        return "redirect:/owner/offers";
    }

    @GetMapping("/owner/offers/{id}/edit")
    public String editOffer(
            @PathVariable Long id,
            Authentication authentication,
            Model model
    ) {
        prepareOfferForm(model, authentication.getName(), id);
        return "owner/offer-form";
    }

    @PostMapping("/owner/offers/{id}/update")
    public String updateOffer(
            @PathVariable Long id,
            @Valid @ModelAttribute("offerForm") FoodOfferForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepareInvalidOfferForm(model, authentication.getName(), id);
            return "owner/offer-form";
        }
        try {
            foodOfferService.updateOffer(id, authentication.getName(), form);
        } catch (OfferPublicationException exception) {
            bindingResult.reject(exception.getMessageCode() == null
                    ? "offer.publication"
                    : exception.getMessageCode());
            prepareInvalidOfferForm(model, authentication.getName(), id);
            return "owner/offer-form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "offer.updated");
        return "redirect:/owner/offers";
    }

    private void prepareOfferForm(Model model, String ownerEmail, Long id) {
        model.addAttribute("offerForm", foodOfferService.getOwnerOfferForm(id, ownerEmail));
        model.addAttribute("restaurants", foodOfferService.findOwnerRestaurants(ownerEmail));
        model.addAttribute("offerId", id);
    }

    private void prepareInvalidOfferForm(Model model, String ownerEmail, Long id) {
        model.addAttribute("restaurants", foodOfferService.findOwnerRestaurants(ownerEmail));
        model.addAttribute("offerId", id);
    }
}
