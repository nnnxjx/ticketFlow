package com.ticketflow.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserView {
    private final long id;
    private final String phone;
    private final String status;

}
