package com.ticketflow.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginView {
    private final String accessToken;
    private final String tokenType;
    private final long expiresInSeconds;
    private final UserView user;

}
