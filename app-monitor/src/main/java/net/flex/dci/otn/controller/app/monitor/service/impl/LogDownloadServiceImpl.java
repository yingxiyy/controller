package net.flex.dci.otn.controller.app.monitor.service.impl;

import static net.flex.dci.otc.common.util.CommonUtil.isIPv6Address;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.servlet.http.HttpServletResponse;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zkclient4boot.refactor.utils.DciInstancesUtils;
import net.flex.dci.otn.controller.app.monitor.service.LogDownloadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LogDownloadServiceImpl implements LogDownloadService {


    @Value("${log.download.connect.timeout:5000}")
    private int connectTimeout;

    @Value("${log.download.read.timeout:0}")
    private int readTimeout;

    private static final int BUFFER_SIZE = 8192;

    private static final String LOG_DOWNLOAD_URL_PATTERN_IPV4 = "http://%s:%d/log/downloadZip?service=%s";
    private static final String LOG_DOWNLOAD_URL_PATTERN_IPV6 = "http://[%s]:%d/log/downloadZip?service=%s";


    private static final String AUTH_URL_PREFIX = "%s:%s@";


    public void forward(String service, HttpServletResponse response) {

        if (service == null || service.trim().isEmpty()) {
            String error = "log download failed,service name can not be null";
            log.error(error);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, error);
        }
        String serviceTrim = service.trim();
        log.info("begin forward download log request,target service:{}", serviceTrim);
        HttpURLConnection conn = null;
        try {
            ServiceRequestInfo requestInfo = getTargetUrl(serviceTrim);
            log.debug("download log url construct finished:{}", requestInfo.getTargetUrl());
            conn = createHttpConnection(requestInfo);
            int status = conn.getResponseCode();
            log.info("success connect target service with http,the http status code :{}", status);
            forwardResponse(conn, response);
//            conn = (HttpURLConnection) new URL(targetUrl).openConnection();
//            conn.setRequestMethod("GET");
//            conn.setConnectTimeout(5000);
//            conn.setReadTimeout(0); // allow long zip streaming
//            conn.setDoInput(true);
//            conn.connect();

//            int status = conn.getResponseCode();
//            response.setStatus(status);
//
//            // copy headers
//            if (conn.getContentType() != null) {
//                response.setContentType(conn.getContentType());
//            }
//
//            String disposition =
//                    conn.getHeaderField(HttpHeaders.CONTENT_DISPOSITION);
//            if (disposition != null) {
//                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, disposition);
//            }
//
//            // IMPORTANT PART
//            InputStream in = (status >= 200 && status < 300)
//                    ? conn.getInputStream()
//                    : conn.getErrorStream();
//
//            if (in == null) {
//                return; // nothing to forward
//            }
//
//            OutputStream out = response.getOutputStream();
//
//            byte[] buffer = new byte[8192];
//            int len;
//            while ((len = in.read(buffer)) != -1) {
//                out.write(buffer, 0, len);
//            }
//            out.flush();

        } catch (CommonException ex) {
            throw ex;
        } catch (Exception e) {
            String errorMsg = String.format(
                    "log download failed,service [%s] forward request with unknown error",
                    serviceTrim);
            log.error(errorMsg, e);
            throw new CommonException(
                    CommonExceptionType.COMMAND_EXECUTION_ERROR,
                    errorMsg,
                    e
            );
        } finally {
            if (conn != null) {
                conn.disconnect();
                log.debug("target http connection disposed,service:{}", serviceTrim);
            }
        }
    }

    private void forwardResponse(HttpURLConnection conn, HttpServletResponse response)
            throws IOException {
        log.debug("forward response");
        int statusCode = response.getStatus();
        response.setStatus(statusCode);
        log.debug("set response status code:{}", statusCode);
        copyResponseHeaders(conn, response);
        try (InputStream in = getResponseInputStream(conn, statusCode);
                OutputStream out = response.getOutputStream()) {
            if (null == in) {
                log.warn(
                        "target service data stream is null，no data forward，response status code：{}",
                        statusCode);
                return;
            }
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            int totalBytes = 0;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                totalBytes += len;
            }
            out.flush();
            log.info(
                    "Response stream transfer completed, total transferred bytes: {} bytes, response status code: {}",
                    totalBytes, statusCode);
        } catch (IOException e) {
            log.error("IO exception occurred while copying response stream", e);
            throw e;
        }
    }

    private InputStream getResponseInputStream(HttpURLConnection conn, int statusCode)
            throws IOException {
        InputStream in = null;
        if (statusCode >= 200 && statusCode < 300) {
            in = conn.getInputStream();
            log.debug("Get normal response stream from target service, status code: {}",
                    statusCode);
        } else {
            in = conn.getErrorStream();
            log.warn("Get error response stream from target service, status code: {}", statusCode);
        }
        return in;
    }

    private void copyResponseHeaders(HttpURLConnection conn, HttpServletResponse response) {
        String contentType = conn.getContentType();
        if (contentType != null) {
            response.setContentType(contentType);
            log.debug("set response content type is:{}", contentType);
        }
        String contentDisposition = conn.getHeaderField(HttpHeaders.CONTENT_DISPOSITION);
        if (contentDisposition != null) {
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
            log.debug("set response header content disposition is:{}", contentDisposition);
        }
        String contentLength = conn.getHeaderField(HttpHeaders.CONTENT_LENGTH);
        if (contentLength != null) {
            response.setHeader(HttpHeaders.CONTENT_LENGTH, contentLength);
            log.debug("set response header content length is:{}", contentLength);
        }
    }

    private HttpURLConnection createHttpConnection(ServiceRequestInfo requestInfo)
            throws IOException {
        String targetUrl = requestInfo.getTargetUrl();
        String username = requestInfo.getUser();
        String password = requestInfo.getPassword();
        log.debug("build target url :{} http connection", targetUrl);
        URL url = new URL(targetUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        log.debug(
                "http connection parameter configuration：connectTimeout{}ms，readTimeout{}ms，http request method:GET",
                connectTimeout, readTimeout);
        if (username != null && password != null) {
            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder()
                    .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
            log.debug("Added Basic Authentication header for user: {}", username);
        }
        conn.connect();
        return conn;
    }


    private ServiceRequestInfo getTargetUrl(String service) throws UnsupportedEncodingException {
        InstanceDetails instanceDetail = DciInstancesUtils.getInstancesByInstanceId(service);
        if (instanceDetail == null) {
            String errorMsg = String.format(
                    "service log download failed：can not found current service ，current service is：%s",
                    service);
            log.error(errorMsg);
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR, errorMsg);
        }
        int port = instanceDetail.getPort();
        if (instanceDetail.getHttpPort() != null) {
            port = instanceDetail.getHttpPort();
        }
        String instanceIp = instanceDetail.getMyIp();
        String targetUrl;
        String username = instanceDetail.getUser();
        String password = instanceDetail.getPasswd();
        try {
            if (isIPv6Address(instanceIp)) {
                log.debug("target service ip is ipv6 format address：{}，use IPV6 build target url",
                        instanceIp);
                String encodedService = URLEncoder.encode(instanceDetail.getId(),
                        String.valueOf(StandardCharsets.UTF_8));
                targetUrl = String.format(LOG_DOWNLOAD_URL_PATTERN_IPV6, instanceIp, port,
                        encodedService);
            } else {
                log.debug("target service ip is IPv4 format address：{}，use IPV4 build target url",
                        instanceIp);
                String encodedService = URLEncoder.encode(instanceDetail.getId(),
                        String.valueOf(StandardCharsets.UTF_8));
                targetUrl = String.format(LOG_DOWNLOAD_URL_PATTERN_IPV4, instanceIp, port,
                        encodedService);
            }
            return ServiceRequestInfo.builder().targetUrl(targetUrl).user(username)
                    .password(password).build();
        } catch (Exception e) {
            String errorMsg = String.format("Failed to build log download URL for service=%s",
                    service);
            log.error(errorMsg, e);
            throw new CommonException(CommonExceptionType.CANNOT_FIND_COOPERATOR, errorMsg);
        }
    }

    @Builder
    @Data
    private static class ServiceRequestInfo implements Serializable {

        private String targetUrl;
        private String user;
        private String password;
    }
}
