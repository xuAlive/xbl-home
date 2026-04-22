package com.xu.home.service.blog;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.domain.blog.BaziMarriageRecord;

import java.util.List;

public interface BaziMarriageRecordService extends IService<BaziMarriageRecord> {

    List<BaziMarriageRecord> getRecentRecords(String account, int limit);

    BaziMarriageRecord getOwnedRecord(Long id, String account);
}
