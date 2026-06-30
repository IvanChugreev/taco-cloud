package tacos.dto;

import lombok.Value;

@Value
public class UserProfile {
    String fullname;
    String street;
    String city;
    String state;
    String zip;
}
