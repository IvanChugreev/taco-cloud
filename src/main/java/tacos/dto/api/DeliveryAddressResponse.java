package tacos.dto.api;

public record DeliveryAddressResponse(
        String recipientName,
        String street,
        String city,
        String state,
        String zip) {
}
