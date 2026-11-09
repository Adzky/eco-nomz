package id.co.economz.eco_nomz.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {
    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.email}")
    @Size(max = 320, message = "{validation.maxLength}")
    private String email;

    @NotBlank(message = "{validation.required}")
    @Size(max = 72, message = "{validation.maxLength}")
    private String password;

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
