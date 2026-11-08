package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ticketflow.common.CatalogAccess;
import com.ticketflow.common.Ids;
import com.ticketflow.dto.venue.VenueRequest;
import com.ticketflow.dto.venue.VenueSeatRequest;
import com.ticketflow.entity.venue.VenueEntity;
import com.ticketflow.entity.venue.VenueSeatEntity;
import com.ticketflow.mapper.venue.VenueMapper;
import com.ticketflow.mapper.venue.VenueSeatMapper;
import com.ticketflow.service.IVenueService;
import com.ticketflow.vo.venue.VenueSeatView;
import com.ticketflow.vo.venue.VenueView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class VenueServiceImpl extends ServiceImpl<VenueMapper, VenueEntity> implements IVenueService {
    @Autowired private CatalogAccess access;
    @Autowired private VenueSeatMapper seatMapper;
    @Autowired private Ids ids;

    @Override @Transactional
    public VenueView create(VenueRequest request) {
        long organizerId = access.currentOrganizer().getId();
        VenueEntity venue = new VenueEntity();
        venue.setId(ids.next());
        venue.setOrganizerId(organizerId);
        fill(venue, request);
        save(venue);
        return view(venue);
    }

    @Override @Transactional
    public VenueView update(long venueId, VenueRequest request) {
        VenueEntity venue = access.venueOwned(venueId);
        fill(venue, request);
        updateById(venue);
        return view(venue);
    }

    @Override
    public VenueView getOwned(long venueId) { return view(access.venueOwned(venueId)); }

    @Override
    public List<VenueView> mine() {
        long organizerId = access.currentOrganizer().getId();
        return list(new LambdaQueryWrapper<VenueEntity>().eq(VenueEntity::getOrganizerId, organizerId).orderByDesc(VenueEntity::getId))
                .stream().map(this::view).toList();
    }

    @Override @Transactional
    public VenueSeatView addSeat(long venueId, VenueSeatRequest request) {
        access.venueOwned(venueId);
        VenueSeatEntity seat = new VenueSeatEntity();
        seat.setId(ids.next());
        seat.setVenueId(venueId);
        seat.setArea(request.getArea().trim());
        seat.setRowNo(request.getRowNo().trim());
        seat.setSeatNo(request.getSeatNo().trim());
        try { seatMapper.insert(seat); }
        catch (DuplicateKeyException ex) { throw CatalogAccess.conflict("该座位位置已存在"); }
        return seatView(seat);
    }

    @Override
    public List<VenueSeatView> seats(long venueId) {
        access.venueOwned(venueId);
        return seatMapper.selectList(new LambdaQueryWrapper<VenueSeatEntity>().eq(VenueSeatEntity::getVenueId, venueId)
                        .orderByAsc(VenueSeatEntity::getArea, VenueSeatEntity::getRowNo, VenueSeatEntity::getSeatNo))
                .stream().map(this::seatView).toList();
    }

    private void fill(VenueEntity venue, VenueRequest request) {
        venue.setName(request.getName().trim());
        venue.setAddress(request.getAddress());
    }
    private VenueView view(VenueEntity v) { return new VenueView(v.getId(), v.getOrganizerId(), v.getName(), v.getAddress()); }
    private VenueSeatView seatView(VenueSeatEntity s) { return new VenueSeatView(s.getId(), s.getVenueId(), s.getArea(), s.getRowNo(), s.getSeatNo()); }
}
