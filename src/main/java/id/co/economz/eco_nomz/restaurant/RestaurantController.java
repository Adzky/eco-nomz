package id.co.economz.eco_nomz.restaurant;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class RestaurantController {
    private final RestaurantService restaurantService;
    private final RestaurantLogoService restaurantLogoService;

    public RestaurantController(
            RestaurantService restaurantService,
            RestaurantLogoService restaurantLogoService
    ) {
        this.restaurantService = restaurantService;
        this.restaurantLogoService = restaurantLogoService;
    }

    @GetMapping("/restaurants")
    public String publicRestaurants(Model model) {
        model.addAttribute("restaurants", restaurantService.findPublishedRestaurants());
        return "restaurants/list";
    }

    @GetMapping("/restaurants/{id}")
    public String publicRestaurant(@PathVariable Long id, Model model) {
        model.addAttribute("restaurant", restaurantService.getPublishedRestaurant(id));
        return "restaurants/detail";
    }

    @GetMapping("/owner/restaurant")
    public String ownerRestaurants(Authentication authentication, Model model) {
        model.addAttribute("restaurants", restaurantService.findOwnerRestaurants(authentication.getName()));
        return "owner/restaurants";
    }

    @GetMapping("/owner/restaurant/edit")
    public String editRestaurantPage(
            @RequestParam(required = false) Long id,
            Authentication authentication,
            Model model
    ) {
        model.addAttribute(
                "restaurantForm",
                restaurantService.getOwnerRestaurantForm(id, authentication.getName())
        );
        model.addAttribute("restaurantId", id);
        return "owner/restaurant-form";
    }

    @GetMapping("/owner/restaurant/logo/crop")
    public String restaurantLogoCropPage() {
        return "redirect:/owner/restaurant";
    }

    @GetMapping("/owner/restaurant/{id}/logo/crop")
    public String restaurantLogoCropPage(
            @PathVariable Long id,
            Authentication authentication,
            Model model
    ) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(id, authentication.getName());
        model.addAttribute("restaurantId", restaurant.getId());
        model.addAttribute("logoUrl", restaurant.getLogoUrl());
        return "owner/logo-crop";
    }

    @PutMapping(
            path = "/owner/restaurant/{id}/logo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseBody
    public LogoResponse uploadRestaurantLogo(
            @PathVariable Long id,
            @RequestPart("logo") MultipartFile logo,
            Authentication authentication
    ) {
        StoredImage image = restaurantLogoService.uploadLogo(id, authentication.getName(), logo);
        return new LogoResponse(image.secureUrl());
    }

    @PostMapping("/owner/restaurant")
    public String createRestaurant(
            @Valid @ModelAttribute("restaurantForm") RestaurantForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (form.isPublished()) {
            bindingResult.reject("restaurant.logo.required");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("restaurantId", null);
            return "owner/restaurant-form";
        }
        Restaurant restaurant = restaurantService.createRestaurant(authentication.getName(), form);
        redirectAttributes.addFlashAttribute("successMessage", "restaurant.profileCreated");
        return "redirect:/owner/restaurant/" + restaurant.getId() + "/logo/crop";
    }

    @PostMapping("/owner/restaurant/update")
    public String updateRestaurant(
            @RequestParam Long id,
            @Valid @ModelAttribute("restaurantForm") RestaurantForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(id, authentication.getName());
        form.setLogoUrl(restaurant.getLogoUrl());
        if (form.isPublished() && restaurant.getLogoUrl() == null) {
            bindingResult.rejectValue("published", "restaurant.logo.required");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("restaurantId", id);
            return "owner/restaurant-form";
        }
        restaurantService.updateRestaurant(id, authentication.getName(), form);
        redirectAttributes.addFlashAttribute("successMessage", "restaurant.profileUpdated");
        return "redirect:/owner/restaurant";
    }

    @PostMapping("/owner/restaurant/{id}/delete")
    public String deleteRestaurant(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        String logoPublicId = restaurantService.getOwnedRestaurant(id, authentication.getName()).getLogoPublicId();
        restaurantService.deleteRestaurant(id, authentication.getName());
        if (logoPublicId != null) {
            restaurantLogoService.deleteStoredLogo(id, logoPublicId);
        }
        redirectAttributes.addFlashAttribute("successMessage", "restaurant.profileDeleted");
        return "redirect:/owner/restaurant";
    }

    public record LogoResponse(String logoUrl) {
    }
}
