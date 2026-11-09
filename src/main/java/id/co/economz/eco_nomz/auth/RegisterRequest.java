package id.co.economz.eco_nomz.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotNull(message = "{validation.required}")
    private RegistrationType accountType = RegistrationType.CUSTOMER;

    @NotBlank(message = "{validation.required}")
    @Size(max = 120, message = "{validation.maxLength}")
    private String name;

    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.email}")
    @Size(max = 320, message = "{validation.maxLength}")
    private String email;

    @NotBlank(message = "{validation.required}")
    @Size(min = 12, max = 72, message = "{validation.sizeRange}")
    private String password;

    public RegistrationType getAccountType() {
        return accountType;
    }

    public void setAccountType(RegistrationType accountType) {
        this.accountType = accountType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
