package id.co.economz.eco_nomz.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ModelAndView handleFailure(
            Exception exception,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        if (exception instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            logFailure(request, status, exception.getClass().getSimpleName());
            return response(request, response, status);
        }
        LOGGER.error(
                "Unhandled request failure method={} path={} exception={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getName()
        );
        return response(request, response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ModelAndView response(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatusCode status
    ) throws IOException {
        String message = publicMessage(status);
        if (isJsonRequest(request)) {
            response.setStatus(status.value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"" + message + "\"}");
            return null;
        }
        return errorPage(status, message);
    }

    private void logFailure(HttpServletRequest request, HttpStatusCode status, String exceptionType) {
        if (status.is5xxServerError()) {
            LOGGER.warn(
                    "Request failed method={} path={} status={} exception={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    status.value(),
                    exceptionType
            );
        }
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return request.getRequestURI().startsWith("/payments/")
                || (accept != null && accept.contains("application/json"));
    }

    private ModelAndView errorPage(HttpStatusCode status, String message) {
        ModelAndView view = new ModelAndView("error/error");
        view.setStatus(status);
        view.addObject("status", status.value());
        view.addObject("message", message);
        return view;
    }

    private String publicMessage(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "The request could not be processed. Check the submitted information.";
            case 401, 403 -> "You are not authorized to access this page.";
            case 404 -> "The requested page or record was not found.";
            case 409 -> "This action conflicts with the current state. Refresh and try again.";
            default -> "The request could not be completed. Please try again later.";
        };
    }
}
