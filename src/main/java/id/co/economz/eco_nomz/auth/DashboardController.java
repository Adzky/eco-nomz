package id.co.economz.eco_nomz.auth;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    @GetMapping({"/customer", "/owner"})
    public String dashboard(Authentication authentication, Model model) {
        model.addAttribute("email", authentication.getName());
        model.addAttribute("role", authentication.getAuthorities().iterator().next().getAuthority());
        return "account/dashboard";
    }
}
