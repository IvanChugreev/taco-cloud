package tacos.dto.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeliveryAddressRequest(
        @NotBlank @Size(max = 128) String recipientName,
        @NotBlank @Size(max = 255) String street,
        @NotBlank @Size(max = 128) String city,
        @NotBlank @Size(max = 64) String state,
        @NotBlank @Size(max = 32) String zip) {
}
