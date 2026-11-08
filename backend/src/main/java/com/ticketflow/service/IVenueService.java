package com.ticketflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ticketflow.dto.venue.VenueRequest;
import com.ticketflow.dto.venue.VenueSeatRequest;
import com.ticketflow.entity.venue.VenueEntity;
import com.ticketflow.vo.venue.VenueView;
import com.ticketflow.vo.venue.VenueSeatView;
import java.util.List;

public interface IVenueService extends IService<VenueEntity> {
    VenueView create(VenueRequest request);
    VenueView update(long venueId, VenueRequest request);
    VenueView getOwned(long venueId);
    List<VenueView> mine();
    VenueSeatView addSeat(long venueId, VenueSeatRequest request);
    List<VenueSeatView> seats(long venueId);
}
