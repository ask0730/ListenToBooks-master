package com.youngqi.tingshu.user.strategy;

import com.youngqi.tingshu.common.execption.GuiguException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author YSQ
 * @PackageName:com.youngqi.tingshu.user.strategy
 * @className: StrategyFactory
 * @Description:
 * @date 2025/5/4 22:38
 */
@Component
@Slf4j
public class StrategyFactory {
    @Autowired
    Map<String,ItemTypeStrategy> strategyMap;

    public ItemTypeStrategy getItemTypeStrategy(String itemType){
        if (strategyMap.containsKey(itemType)){
            return strategyMap.get(itemType);
        }
        log.error("该策略不存在{}",itemType);
        throw new GuiguException(500,"策略"+itemType+"实现不存在");
    }
}
