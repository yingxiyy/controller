package net.flex.dci.otn.controller.idc.manager.message;

/**
 * @version 1.0
 * @date 2022/5/6 13:08
 */
public interface IdcMessage {

    void notifyIdcInfoDelete();

    void notifyIdcCreate();

    void notifyIdcUpdate();

    void notifyIdcBatchCreate();

}
