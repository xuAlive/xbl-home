package com.xu.home.service.blog.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.blog.BaziFortuneRecord;
import com.xu.home.mapper.blog.BaziFortuneRecordMapper;
import com.xu.home.service.blog.BaziFortuneRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BaziFortuneRecordServiceImpl extends ServiceImpl<BaziFortuneRecordMapper, BaziFortuneRecord>
        implements BaziFortuneRecordService {

    @Override
    public List<BaziFortuneRecord> getRecentRecords(String account, int limit) {
        return list(new LambdaQueryWrapper<BaziFortuneRecord>()
                .eq(BaziFortuneRecord::getAccount, account)
                .orderByDesc(BaziFortuneRecord::getCreateTime)
                .last("limit " + Math.max(limit, 1)));
    }

    @Override
    public BaziFortuneRecord getOwnedRecord(Long id, String account) {
        return getOne(new LambdaQueryWrapper<BaziFortuneRecord>()
                .eq(BaziFortuneRecord::getId, id)
                .eq(BaziFortuneRecord::getAccount, account)
                .last("limit 1"));
    }
}
