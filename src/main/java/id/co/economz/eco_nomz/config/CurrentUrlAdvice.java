package id.co.economz.eco_nomz.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentUrlAdvice {
    @ModelAttribute
    void addCurrentUrl(HttpServletRequest request, Model model) {
        String query = request.getQueryString();
        model.addAttribute("currentUrl", request.getRequestURI() + (query == null ? "" : "?" + query));
    }
}
