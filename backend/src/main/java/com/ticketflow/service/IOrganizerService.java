package com.ticketflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ticketflow.entity.organizer.OrganizerEntity;
import com.ticketflow.vo.organizer.OrganizerView;

public interface IOrganizerService extends IService<OrganizerEntity> {
    OrganizerView create(long ownerId, String name);
    OrganizerView current(long ownerId);
}
