package tacos.dto;

import lombok.Value;

import java.io.Serial;
import java.io.Serializable;

@Value
public class TacoSummary implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    Long id;
    String name;
}
