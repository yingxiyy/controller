package net.flex.dci.otc.controller.ne.manager.components.locker;

import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.NeResourceLock.NE_RESOURCE_LOCK_PATH;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.zkclient4boot.lock.DciDistributeLock;
import net.flex.dci.otc.zkclient4boot.lock.DciLockFactory;
import net.flex.dci.otc.zkclient4boot.lock.DistributeLock;
import org.springframework.stereotype.Component;

/**
 * 2025/7/27
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class NeSourceLockerImpl implements NeSourceLocker {

    private final DciLockFactory dciLockFactory;


    @Override
    public DciDistributeLock getResourceLock(String neId) {
        log.debug("get ne resource lock,the ne is:{}", neId);
        String lockPath = String.format(NE_RESOURCE_LOCK_PATH, neId);
        log.debug("Acquiring resource lock for Ne:{} at lock path:{}", neId, lockPath);
        try {
            return createDistributeLock(lockPath);
        } catch (Exception e) {
            log.debug("Failed to create lock for Ne :{}", neId);
            throw new CommonException(CommonExceptionType.RESOURCE_LOCKED,
                    "Could not create lock for NE: " + neId, e);
        }

    }

    private DciDistributeLock createDistributeLock(String lockPath) {
        log.debug("create distribute lock,path :{}", lockPath);
        DistributeLock rawLock = dciLockFactory.newLock(lockPath);
        return (DciDistributeLock) rawLock;
    }
}
