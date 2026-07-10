package tacos.dto.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.CreditCardNumber;

import java.util.List;

public record CreateOrderRequest(
        @NotEmpty List<@NotNull Long> tacoIds,
        @NotNull @Valid DeliveryAddressRequest deliveryAddress,
        @Size(max = 500) String comment,
        @NotBlank @CreditCardNumber String ccNumber,
        @NotBlank
        @Pattern(regexp = "^(0[1-9]|1[0-2])/([2-9][0-9])$", message = "must be formatted MM/YY")
        String ccExpiration,
        @NotBlank @Pattern(regexp = "^[0-9]{3}$", message = "must contain exactly 3 digits") String ccCvv) {
}
