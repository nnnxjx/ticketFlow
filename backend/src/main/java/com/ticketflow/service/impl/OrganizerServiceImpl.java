package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ticketflow.entity.organizer.OrganizerEntity;
import com.ticketflow.vo.organizer.OrganizerView;
import com.ticketflow.mapper.organizer.OrganizerMapper;
import com.ticketflow.service.IOrganizerService;
import com.ticketflow.common.AppException;
import com.ticketflow.common.Ids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class OrganizerServiceImpl extends ServiceImpl<OrganizerMapper, OrganizerEntity> implements IOrganizerService {
    @Autowired private Ids ids;

    @Override
    @Transactional
    public OrganizerView create(long ownerId, String name) {
        if (getOne(new LambdaQueryWrapper<OrganizerEntity>().eq(OrganizerEntity::getOwnerUserId, ownerId)) != null) {
            throw new AppException(HttpStatus.CONFLICT, "ORGANIZER_EXISTS", "当前账号已有主办方");
        }
        OrganizerEntity organizer = new OrganizerEntity();
        organizer.setId(ids.next());
        organizer.setOwnerUserId(ownerId);
        organizer.setName(name.trim());
        organizer.setStatus("ACTIVE");
        try { save(organizer); }
        catch (DuplicateKeyException ex) { throw new AppException(HttpStatus.CONFLICT, "ORGANIZER_EXISTS", "当前账号已有主办方"); }
        return view(organizer);
    }

    @Override
    public OrganizerView current(long ownerId) {
        OrganizerEntity organizer = getOne(new LambdaQueryWrapper<OrganizerEntity>().eq(OrganizerEntity::getOwnerUserId, ownerId));
        if (organizer == null) throw new AppException(HttpStatus.NOT_FOUND, "ORGANIZER_NOT_FOUND", "主办方不存在");
        return view(organizer);
    }

    private OrganizerView view(OrganizerEntity organizer) {
        return new OrganizerView(organizer.getId(), organizer.getName(), organizer.getStatus());
    }
}
