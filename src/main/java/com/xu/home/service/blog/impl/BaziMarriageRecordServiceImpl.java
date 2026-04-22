package com.xu.home.service.blog.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.blog.BaziMarriageRecord;
import com.xu.home.mapper.blog.BaziMarriageRecordMapper;
import com.xu.home.service.blog.BaziMarriageRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BaziMarriageRecordServiceImpl extends ServiceImpl<BaziMarriageRecordMapper, BaziMarriageRecord>
        implements BaziMarriageRecordService {

    @Override
    public List<BaziMarriageRecord> getRecentRecords(String account, int limit) {
        return list(new LambdaQueryWrapper<BaziMarriageRecord>()
                .eq(BaziMarriageRecord::getAccount, account)
                .orderByDesc(BaziMarriageRecord::getCreateTime)
                .last("limit " + Math.max(limit, 1)));
    }

    @Override
    public BaziMarriageRecord getOwnedRecord(Long id, String account) {
        return getOne(new LambdaQueryWrapper<BaziMarriageRecord>()
                .eq(BaziMarriageRecord::getId, id)
                .eq(BaziMarriageRecord::getAccount, account)
                .last("limit 1"));
    }
}
