package id.co.economz.eco_nomz.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import id.co.economz.eco_nomz.auth.RegisterRequest;

class LocaleConfigTests {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MessageSourceAutoConfiguration.class))
            .withUserConfiguration(LocaleConfig.class)
            .withPropertyValues("spring.messages.basename=messages", "spring.messages.encoding=UTF-8");

    @Test
    void defaultsToIndonesianAndResolvesBothMessageBundles() {
        contextRunner.run(context -> {
            MessageSource messageSource = context.getBean(MessageSource.class);
            LocaleResolver localeResolver = context.getBean(LocaleResolver.class);

            assertThat(messageSource.getMessage("market.title", null, Locale.forLanguageTag("id")))
                    .isEqualTo("Temukan makanan lezat dengan harga hemat");
            assertThat(messageSource.getMessage("market.title", null, Locale.ENGLISH))
                    .isEqualTo("Rescue a delicious deal");

            SpringTemplateEngine templateEngine = new SpringTemplateEngine();
            StringTemplateResolver templateResolver = new StringTemplateResolver();
            templateResolver.setTemplateMode(TemplateMode.HTML);
            templateEngine.setTemplateResolver(templateResolver);
            templateEngine.setMessageSource(messageSource);

            Context indonesianContext = new Context(Locale.forLanguageTag("id"));
            indonesianContext.setVariable("successMessage", "offer.created");
            assertThat(templateEngine.process(
                    "<p th:text=\"${#messages.msgOrNull(successMessage) != null ? #messages.msg(successMessage) : successMessage}\"></p>",
                    indonesianContext
            )).contains("Promo makanan berhasil dibuat.");

            LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
            validator.setValidationMessageSource(messageSource);
            validator.afterPropertiesSet();
            try {
                LocaleContextHolder.setLocale(Locale.forLanguageTag("id"));
                assertThat(validator.validate(new RegisterRequest()))
                        .extracting("message")
                        .contains("Bagian ini wajib diisi.");
                LocaleContextHolder.setLocale(Locale.ENGLISH);
                assertThat(validator.validate(new RegisterRequest()))
                        .extracting("message")
                        .contains("This field is required.");
            } finally {
                LocaleContextHolder.resetLocaleContext();
                validator.close();
            }

            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            assertThat(localeResolver.resolveLocale(request)).isEqualTo(Locale.forLanguageTag("id"));

            localeResolver.setLocale(request, response, Locale.ENGLISH);
            assertThat(localeResolver.resolveLocale(request)).isEqualTo(Locale.ENGLISH);
        });
    }
}
