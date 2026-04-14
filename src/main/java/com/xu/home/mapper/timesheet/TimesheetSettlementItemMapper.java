package com.xu.home.mapper.timesheet;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xu.home.domain.timesheet.TimesheetSettlementItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 结算明细 Mapper
 */
@Mapper
public interface TimesheetSettlementItemMapper extends BaseMapper<TimesheetSettlementItem> {
}
