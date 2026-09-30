package net.flex.otc.dci.toop.base.batchlock.lib;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.batchlock.core.BatchLockTransaction;
import net.flex.dci.otn.controller.batchlock.lib.ZooKeeperToolset;

@Slf4j
public class ProcessLockThread {
	public static void main(String[] args) {
        try {
            ZooKeeperToolset.instance().setNameSpace("AAAAA");
            final BatchLockTransaction t = ZooKeeperToolset.instance().newTransaction();
            log.info("transaction ok");
            t.lock("test4");
            t.require();
            log.info("transaction {} locked", t);
            
            try {
                Thread.sleep(3000);
            } catch (Exception e) {
                log.error("can't sleep 3 s");
            }
            
            new Thread() {
            	public void run() {
            		t.dismiss();
            		log.info("transaction unlocked in thread");
            	}
            }.start();
            try {
                Thread.sleep(60000);
            } catch (Exception e) {
                log.error("can't sleep");
            }
        } catch (Exception e) {
        	log.error("exception caught", e);
        }
    }
}
