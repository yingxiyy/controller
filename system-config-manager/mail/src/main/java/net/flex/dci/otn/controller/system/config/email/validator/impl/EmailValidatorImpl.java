package net.flex.dci.otn.controller.system.config.email.validator.impl;

import cn.hutool.core.builder.CompareToBuilder;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.system.config.common.model.MailConstants;
import net.flex.dci.otn.controller.system.config.email.validator.EmailValidator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * @version 1.0
 * @date 10/11/2023 1:54 PM
 */
@Component
@Slf4j
public class EmailValidatorImpl implements EmailValidator {

    private static final int TIMEOUT = 6000;
    private static final int SLEEP_SECT = 50;


    @Override
    public boolean validEmailAddress(String emailAddress, String domain) {
        log.info("valid the email address :{} is reachable or not", emailAddress);
        if (StringUtils.isEmpty(emailAddress)) {
            log.error("the email address should not be null");
            return false;
        }

        boolean result = _validEmailAddress(emailAddress, domain);
        return result;
    }

    private boolean _validEmailAddress(String emailAddress, String domain) {
        String host = emailAddress.substring(emailAddress.indexOf(MailConstants.AT) + 1);
        log.debug("valid the email {},the host server is :{}", emailAddress, host);
        try {

            Record[] mxRecords = new Lookup(host, Type.MX).run();
            if (ArrayUtils.isEmpty(mxRecords)) {
                throw new CommonException(CommonExceptionType.CANNOT_FIND_COOPERATOR, String.format("the email address: %s host server:%s is not reachable", emailAddress, host));
            }
            String mxHost = getMxHostByPriority(mxRecords);
            boolean result = judgeBySmtp(mxHost, emailAddress, domain);
            return result;
        } catch (Exception e) {
            log.error("failed to valid the email address,the email address is:{},the reason is:{}", emailAddress, e.getMessage(), e);
            return false;
        }
    }

    private boolean judgeBySmtp(String mxHost, String emailAddress, String domain) throws IOException, InterruptedException {
        log.debug("judge by smtp the mx host is:{}", mxHost);
        Socket socket = new Socket();
        try {

            socket.connect(new InetSocketAddress(mxHost, 25));
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(socket.getInputStream())));
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            if (getResponseCode(bufferedReader) != 220) {
                return false;
            }
            bufferedWriter.write("EHLO " + domain + " \r\n");
            bufferedWriter.flush();
            if (getResponseCode(bufferedReader) != 250) {
                return false;
            }
            bufferedWriter.write("MAIL FROM:<" + domain + "> \r\n");
            bufferedWriter.flush();
            if (getResponseCode(bufferedReader) != 250) {
                return false;
            }
            bufferedWriter.write("RCPT TO:<" + emailAddress + "> \r\n");
            bufferedWriter.flush();
            if (getResponseCode(bufferedReader) != 250) {
                return false;
            }
            bufferedWriter.write("QUIT\r\n");
            bufferedWriter.flush();
            return true;
        } finally {
            socket.close();
        }

    }

    private int getResponseCode(BufferedReader bufferedReader) throws InterruptedException, IOException {
        int code = 0;
        for (long i = SLEEP_SECT; i < TIMEOUT; i += SLEEP_SECT) {
            Thread.sleep(SLEEP_SECT);
            if (bufferedReader.ready()) {
                String outline = bufferedReader.readLine();
                while (bufferedReader.ready())
                    bufferedReader.readLine();
                code = Integer.parseInt(outline.substring(0, 3));
                break;
            }
        }
        return code;
    }

    private String getMxHostByPriority(Record[] mxRecords) {
        int size = mxRecords.length;
        String mxHost = ((MXRecord) mxRecords[0]).getTarget().toString();
        if (size > 1) {
            List<Record> arrRecords = Arrays.asList(mxRecords);
            arrRecords.sort(new Comparator<Record>() {
                @Override
                public int compare(Record r1, Record r2) {
                    return new CompareToBuilder().append(((MXRecord) r1).getPriority(), ((MXRecord) r2).getPriority()).build();
                }
            });
            mxHost = ((MXRecord) arrRecords.get(0)).getTarget().toString();
        }
        return mxHost;
    }
}
