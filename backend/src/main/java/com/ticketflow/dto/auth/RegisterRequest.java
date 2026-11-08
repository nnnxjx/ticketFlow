package com.ticketflow.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$")
    private String phone;

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

}
