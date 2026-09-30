/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.common.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.frequency.Constant;
import net.flex.dci.otc.common.util.frequency.FrequencyAvailable;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WdmUtilService {

    public static final int MAX_MUX_C_PORT = 32;

    public WDM_Band getWdmBandByMdPort(String tpId) {
        Pattern pattern = Pattern.compile("\\d{1,2}$");
        Matcher matcher = pattern.matcher(tpId);

        int portNumber;
        if (matcher.find()) {
            portNumber = Integer.parseInt(matcher.group());
        } else {
            log.warn("Failed to get mux port number, because invalid tp:{}", tpId);
            return null;//这里先不抛出异常，留个日志

        }
        if (portNumber > MAX_MUX_C_PORT) {
            return WDM_Band.L;
        }
        return WDM_Band.C;
    }

    public WDM_Band getWdmBandByFre(String freeFrequency) {

        Pattern pattern = Pattern.compile("=(\\d+),(\\d+)");
        Matcher matcher = pattern.matcher(freeFrequency);

        if (matcher.find()) {
            // 直接组1和组2的值（索引从1开始）
            long firstNumber = Long.parseLong(matcher.group(1));
            long secondNumber = Long.parseLong(matcher.group(2));
            long freLowL = Long.parseLong(Constant.MaxLowerFrequency.MUX64_BD_L);
            long freHighL = Long.parseLong(Constant.MaxUpperFrequency.MUX64_BD_L);
            long freLowC = Long.parseLong(Constant.MaxLowerFrequency.MUX64_BD_C);
            long freHighC = Long.parseLong(Constant.MaxUpperFrequency.MUX64_BD_C);
            if (firstNumber >= freLowL && secondNumber <= freHighL) {
                return WDM_Band.L;
            }
            if (firstNumber >= freLowC && secondNumber <= freHighC) {
                return WDM_Band.C;
            }
        }
        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Not C or L band, invalid frequency:" + freeFrequency);
    }

    public WDM_Band getWdmBandByCentFre(Long centFreq, GridType grid) {
        long centFactor = FrequencyAvailable.getCentFactor(grid);
        long lowFre = centFreq - centFactor;
        long freLowL = Long.parseLong(Constant.MaxLowerFrequency.MUX64_BD_L);
        long freHighL = Long.parseLong(Constant.MaxUpperFrequency.MUX64_BD_L);
        long freLowC = Long.parseLong(Constant.MaxLowerFrequency.MUX64_BD_C);
        long freHighC = Long.parseLong(Constant.MaxUpperFrequency.MUX64_BD_C);

        if (lowFre >= freLowL && lowFre <= freHighL) {
            return WDM_Band.L;
        }
        if (lowFre >= freLowC && lowFre <= freHighC) {
            return WDM_Band.C;
        }
        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                String.format("Not C or L band, invalid central frequency:%s,gird:%s", centFreq, grid));
    }
}
