package id.co.economz.eco_nomz.config;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.LocaleResolver;

@Controller
public class LanguageController {
    private final LocaleResolver localeResolver;

    public LanguageController(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    @GetMapping("/language")
    String changeLanguage(
            @RequestParam String lang,
            @RequestParam(defaultValue = "/") String returnTo,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Locale locale = switch (lang) {
            case "en" -> Locale.ENGLISH;
            case "id" -> Locale.forLanguageTag("id");
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported language");
        };
        if (!returnTo.startsWith("/") || returnTo.startsWith("//") || returnTo.contains("\\")
                || returnTo.contains("\r") || returnTo.contains("\n")) {
            returnTo = "/";
        }
        localeResolver.setLocale(request, response, locale);
        return "redirect:" + returnTo;
    }
}
