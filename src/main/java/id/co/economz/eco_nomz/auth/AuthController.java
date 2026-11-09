package id.co.economz.eco_nomz.auth;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseCookie;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class AuthController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final String cookieName;
    private final boolean cookieSecure;

    public AuthController(
            AuthService authService,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            @Value("${app.auth.jwt.cookie-name:ECO_NOMZ_AUTH}") String cookieName,
            @Value("${app.auth.jwt.cookie-secure:true}") boolean cookieSecure
    ) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            authService.registerAccount(request);
        } catch (DuplicateEmailException exception) {
            bindingResult.rejectValue("email", "auth.register.emailTaken");
            return "auth/register";
        } catch (PasswordTooLongException exception) {
            bindingResult.rejectValue("password", "auth.register.passwordTooLong");
            return "auth/register";
        }
        redirectAttributes.addAttribute("registered", true);
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(
            Model model,
            @RequestParam(defaultValue = "false") boolean registered,
            @RequestParam(defaultValue = "false") boolean logout
    ) {
        model.addAttribute("loginRequest", new LoginRequest());
        model.addAttribute("registered", registered);
        model.addAttribute("loggedOut", logout);
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(
            @Valid @ModelAttribute("loginRequest") LoginRequest request,
            BindingResult bindingResult,
            HttpServletResponse response,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("registered", false);
            model.addAttribute("loggedOut", false);
            return "auth/login";
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            authService.normalizeEmail(request.getEmail()),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException exception) {
            LOGGER.info("Authentication rejected.");
            bindingResult.reject("auth.login.failed");
            model.addAttribute("registered", false);
            model.addAttribute("loggedOut", false);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return "auth/login";
        }

        String token = jwtService.generateToken(authentication.getName());
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie(token, jwtService.getExpiration()).toString());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return "redirect:" + landingPage(authentication);
    }

    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("", Duration.ZERO).toString());
        SecurityContextHolder.clearContext();
        return "redirect:/login?logout=true";
    }

    private ResponseCookie createCookie(String value, Duration maxAge) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private String landingPage(Authentication authentication) {
        var authorities = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
        if (authorities.contains("ROLE_ADMIN")) {
            return "/admin";
        }
        if (authorities.contains("ROLE_RESTAURANT_OWNER")) {
            return "/owner";
        }
        if (authorities.contains("ROLE_CUSTOMER")) {
            return "/customer";
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account has no supported role.");
    }
}
