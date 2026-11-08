package com.ticketflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ticketflow.dto.tier.TierRequest;
import com.ticketflow.entity.tier.TierEntity;
import com.ticketflow.vo.tier.TierView;
import java.util.List;

public interface ITierService extends IService<TierEntity> {
    TierView create(long sessionId, TierRequest request);
    TierView update(long tierId, TierRequest request);
    void delete(long tierId);
    TierView activate(long tierId);
    TierView stop(long tierId);
    List<TierView> mine(long sessionId);
    List<TierView> publicList(long sessionId);
}
