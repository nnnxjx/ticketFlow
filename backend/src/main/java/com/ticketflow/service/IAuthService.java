package com.ticketflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ticketflow.dto.auth.LoginRequest;
import com.ticketflow.dto.auth.RegisterRequest;
import com.ticketflow.entity.auth.UserEntity;
import com.ticketflow.vo.auth.LoginView;
import com.ticketflow.vo.auth.UserView;

public interface IAuthService extends IService<UserEntity> {
    UserView register(RegisterRequest body);
    LoginView login(LoginRequest body);
    UserView me(long id);
}
