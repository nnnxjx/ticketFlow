package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ticketflow.common.AppException;
import com.ticketflow.entity.order.QuotaEntity;
import com.ticketflow.mapper.order.QuotaMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuotaServiceImplTest {
    @Test
    void existingQuantityPlusRequestCannotExceedLimit() {
        QuotaMapper mapper = mock(QuotaMapper.class);
        QuotaServiceImpl service = new QuotaServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "quotas", mapper);
        when(mapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

        AppException error = assertThrows(AppException.class, () -> service.reserve(7L, 3L, 2, 4));

        assertEquals("LIMIT_EXCEEDED", error.code());
        verify(mapper).ensure(7L, 3L);
        verify(mapper).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(mapper, never()).selectOne(any());
    }
}
