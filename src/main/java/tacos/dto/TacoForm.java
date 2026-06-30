package tacos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TacoForm {

    @NotBlank(message = "Name is required")
    @Size(min = 5, message = "Name must be at least 5 characters long")
    private String name;

    @NotEmpty(message = "You must choose at least 1 ingredient")
    private List<String> ingredientIds = new ArrayList<>();
}
