package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ticketflow.dto.auth.LoginRequest;
import com.ticketflow.dto.auth.RegisterRequest;
import com.ticketflow.entity.auth.UserEntity;
import com.ticketflow.vo.auth.LoginView;
import com.ticketflow.vo.auth.UserView;
import com.ticketflow.mapper.auth.UserMapper;
import com.ticketflow.service.IAuthService;
import com.ticketflow.common.AppException;
import com.ticketflow.common.Ids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements IAuthService {
    @Autowired private PasswordEncoder encoder;
    @Autowired private JwtEncoder jwtEncoder;
    @Autowired private Ids ids;
    @Value("${ticketflow.jwt.ttl-minutes}") private long ttlMinutes;
    @Value("${ticketflow.jwt.expire-enabled:true}") private boolean expireEnabled;

    @Override
    @Transactional
    public UserView register(RegisterRequest body) {
        // 手机号唯一约束由数据库兜底，避免并发注册时先查后插的竞争。
        UserEntity user = new UserEntity();
        user.setId(ids.next());
        user.setPhone(body.getPhone());
        user.setPasswordHash(encoder.encode(body.getPassword()));
        user.setStatus("ACTIVE");
        try {
            save(user);
        } catch (DuplicateKeyException ex) {
            throw new AppException(HttpStatus.CONFLICT, "PHONE_EXISTS", "手机号已注册");
        }
        return view(user);
    }

    @Override
    public LoginView login(LoginRequest body) {
        UserEntity user = getOne(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getPhone, body.getPhone()));
        if (user == null || !encoder.matches(body.getPassword(), user.getPasswordHash())) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "手机号或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new AppException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "账号已禁用");
        }
        Instant now = Instant.now();
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder().issuer("ticketflow").subject(Long.toString(user.getId()))
                .issuedAt(now);
        // 本地 dev profile 可关闭过期时间，其他环境继续使用有限期令牌。
        if (expireEnabled) claimsBuilder.expiresAt(now.plus(ttlMinutes, ChronoUnit.MINUTES));
        JwtClaimsSet claims = claimsBuilder.build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginView(token, "JWT", expireEnabled ? ttlMinutes * 60 : 0, view(user));
    }

    @Override
    public UserView me(long id) {
        UserEntity user = getById(id);
        if (user == null) throw new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "账号不存在");
        return view(user);
    }

    private UserView view(UserEntity user) {
        return new UserView(user.getId(), user.getPhone(), user.getStatus());
    }
}
