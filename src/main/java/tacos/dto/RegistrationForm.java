package tacos.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistrationForm {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    @NotBlank(message = "Password confirmation is required")
    private String confirm;

    @NotBlank(message = "Full name is required")
    private String fullname;

    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Zip is required")
    private String zip;

    @NotBlank(message = "Phone is required")
    private String phone;

    @AssertTrue(message = "Passwords must match")
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirm);
    }
}
