package tacos.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.CreditCardNumber;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrderForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Delivery name is required")
    private String deliveryName;

    @NotBlank(message = "Street is required")
    private String deliveryStreet;

    @NotBlank(message = "City is required")
    private String deliveryCity;

    @NotBlank(message = "State is required")
    private String deliveryState;

    @NotBlank(message = "Zip code is required")
    private String deliveryZip;

    @Size(max = 500, message = "Comment must not exceed 500 characters")
    private String comment;

    @CreditCardNumber(message = "Not a valid credit card number")
    @NotBlank(message = "Credit card number is required")
    private String ccNumber;

    @NotBlank(message = "Expiration is required")
    @Pattern(regexp = "^(0[1-9]|1[0-2])/([2-9][0-9])$", message = "Must be formatted MM/YY")
    private String ccExpiration;

    @NotBlank(message = "CVV is required")
    @Digits(integer = 3, fraction = 0, message = "Invalid CVV")
    private String ccCVV;

    @NotEmpty(message = "Order must contain at least one taco")
    private List<TacoSummary> tacos = new ArrayList<>();

    public void addTaco(TacoSummary taco) {
        tacos.add(taco);
    }

    public List<Long> tacoIds() {
        return tacos.stream().map(TacoSummary::getId).toList();
    }
}
