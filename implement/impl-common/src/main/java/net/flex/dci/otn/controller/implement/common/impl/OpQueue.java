package net.flex.dci.otn.controller.implement.common.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.implement.common.service.MyExecutor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantReadWriteLock;


@Slf4j
@Component
public class OpQueue {
  private int loopInterval = 1000;
  private boolean loopStated = false;

  private List<Implementor> queue;
  private CountDownLatch latch;

  public OpQueue() {
    queue = new ArrayList<>();
  }

  synchronized public void add(Implementor implementor) {
    if (queue.isEmpty() && !loopStated) {
      loopStated = true;
      loop();
    }

    if (!hasSameOne(implementor.getLinkId(), implementor.getTargetState())) {
      queue.add(implementor);
    }
  }

  synchronized public Implementor fetchNext() {
    if (queue.isEmpty()) {
      return null;
    }
    log.debug("queue here again");
    return queue.remove(0);
  }

  synchronized public void requireDone() {
    log.debug("queue unlocked");
    latch.countDown();
  }

  private boolean hasSameOne(String linkId, ImplementState implementState) {
    Optional<Implementor> tmp = queue.stream().filter(t -> t.getLinkId().equals(linkId) && t.getTargetState().equals(implementState)).findAny();
    if (tmp.isPresent()) {
      return true;
    } else {
      return false;
    }
  }

  private void loop() {
    MyExecutor executor = new MyExecutor();
    executor.lazyDo(new Runnable() {
      @Override
      public void run() {
        while (true) {
          Implementor implementor = fetchNext();
          if (implementor != null) {
            log.debug("queue start working on {} to {} ", implementor.getLinkId(), implementor.getTargetState());
            implementor.startSyncAction();
            try {
              latch = new CountDownLatch(1);
              latch.await();
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }

          try {
            Thread.sleep(loopInterval);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    });
  }
}