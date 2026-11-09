package id.co.economz.eco_nomz.admin;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/admin")
    public String dashboard(Authentication authentication, Model model) {
        model.addAttribute("summary", adminService.dashboard(authentication.getName()));
        return "admin/dashboard";
    }

    @GetMapping("/admin/users")
    public String users(
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model
    ) {
        requireNonnegativePage(page);
        model.addAttribute("users", adminService.users(authentication.getName(), page));
        return "admin/users";
    }

    @PostMapping("/admin/users/{id}/status")
    public String setUserActive(
            @PathVariable Long id,
            @RequestParam boolean active,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        adminService.setUserActive(authentication.getName(), id, active);
        redirectAttributes.addFlashAttribute("successMessage", "admin.accountStatusUpdated");
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/restaurants")
    public String restaurants(
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model
    ) {
        requireNonnegativePage(page);
        model.addAttribute("restaurants", adminService.restaurants(authentication.getName(), page));
        return "admin/restaurants";
    }

    @PostMapping("/admin/restaurants/{id}/moderation")
    public String setRestaurantModerated(
            @PathVariable Long id,
            @RequestParam boolean moderated,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        adminService.setRestaurantModerated(authentication.getName(), id, moderated);
        redirectAttributes.addFlashAttribute("successMessage", "admin.restaurantModerationUpdated");
        return "redirect:/admin/restaurants";
    }

    @GetMapping("/admin/offers")
    public String offers(
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model
    ) {
        requireNonnegativePage(page);
        model.addAttribute("offers", adminService.offers(authentication.getName(), page));
        return "admin/offers";
    }

    @PostMapping("/admin/offers/{id}/moderation")
    public String setOfferModerated(
            @PathVariable Long id,
            @RequestParam boolean moderated,
            @RequestParam(required = false) String note,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        adminService.setOfferModerated(authentication.getName(), id, moderated, note);
        redirectAttributes.addFlashAttribute("successMessage", "admin.offerModerationUpdated");
        return "redirect:/admin/offers";
    }

    private void requireNonnegativePage(int page) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must not be negative.");
        }
    }
}
