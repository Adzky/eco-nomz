package id.co.economz.eco_nomz.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

class GlobalExceptionHandlerTests {
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void hidesInternalReasonForHtmlClientErrors() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/missing");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        ModelAndView response = handler.handleFailure(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "sensitive internal detail"),
                request,
                servletResponse
        );

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getModel().get("message")).isEqualTo(
                "The requested page or record was not found."
        );
        assertThat((String) response.getModel().get("message")).doesNotContain("sensitive");
    }

    @Test
    void retainsStatusAndGenericBodyForPaymentProviderErrors() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/payments/midtrans/notifications");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        ModelAndView response = handler.handleFailure(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid secret detail"),
                request,
                servletResponse
        );

        assertThat(response).isNull();
        assertThat(servletResponse.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(servletResponse.getContentType()).startsWith("application/json");
        assertThat(servletResponse.getContentAsString()).contains(
                "\"error\":\"You are not authorized to access this page.\""
        );
    }

    @Test
    void unexpectedFailuresReturnGenericServerErrorWithoutExceptionMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        ModelAndView response = handler.handleFailure(
                new IllegalStateException("database password must not appear"),
                request,
                servletResponse
        );

        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getModel().get("message")).isEqualTo(
                "The request could not be completed. Please try again later."
        );
    }
}
