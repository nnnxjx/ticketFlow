package com.ticketflow.mapper.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketflow.entity.order.QuotaEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface QuotaMapper extends BaseMapper<QuotaEntity> {
    // 唯一键插入让并发请求在同一用户、票档上排队；已有记录不改变占用量。
    @Insert("INSERT IGNORE INTO user_tier_quota(user_id,tier_id,active_qty) VALUES(#{userId},#{tierId},0)")
    int ensure(@Param("userId") long userId, @Param("tierId") long tierId);

}
