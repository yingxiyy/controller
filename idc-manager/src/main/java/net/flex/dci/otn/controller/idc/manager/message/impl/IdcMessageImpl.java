package net.flex.dci.otn.controller.idc.manager.message.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.idc.manager.message.IdcMessage;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/5/6 13:08
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class IdcMessageImpl implements IdcMessage {

    @Override
    public void notifyIdcInfoDelete() {
        
    }

    @Override
    public void notifyIdcCreate() {

    }

    @Override
    public void notifyIdcUpdate() {

    }

    @Override
    public void notifyIdcBatchCreate() {

    }
}
