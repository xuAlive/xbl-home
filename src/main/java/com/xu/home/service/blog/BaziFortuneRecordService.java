package com.xu.home.service.blog;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.home.domain.blog.BaziFortuneRecord;

import java.util.List;

public interface BaziFortuneRecordService extends IService<BaziFortuneRecord> {

    List<BaziFortuneRecord> getRecentRecords(String account, int limit);

    BaziFortuneRecord getOwnedRecord(Long id, String account);
}
