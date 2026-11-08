package com.ticketflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ticketflow.dto.session.SessionRequest;
import com.ticketflow.entity.session.SessionEntity;
import com.ticketflow.vo.session.SessionView;
import java.util.List;

public interface ISessionService extends IService<SessionEntity> {
    SessionView create(long eventId, SessionRequest request);
    SessionView update(long sessionId, SessionRequest request);
    void delete(long sessionId);
    SessionView activate(long sessionId);
    SessionView stop(long sessionId);
    List<SessionView> mine(long eventId);
    List<SessionView> publicList(long eventId);
}
