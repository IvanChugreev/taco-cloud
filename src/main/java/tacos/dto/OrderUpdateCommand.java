package tacos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderUpdateCommand {

    @NotNull
    private Long version;

    @NotBlank
    private String deliveryName;

    @NotBlank
    private String deliveryStreet;

    @NotBlank
    private String deliveryCity;

    @NotBlank
    private String deliveryState;

    @NotBlank
    private String deliveryZip;

    @Size(max = 500)
    private String comment;
}
