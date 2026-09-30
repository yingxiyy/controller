package net.flex.dci.otc.controller.rpc.client.httpclient;

import lombok.Getter;

/**
 * @version 1.0
 * @date 2022/9/14 17:10
 */
public class OdlRpcClientConfig {

    @Getter
    private String user;

    @Getter
    private String password;

    @Getter
    private Long connectionTimeout;

    @Getter
    private Long readTimeout;

    private OdlRpcClientConfig(OdlRpcClientConfig.Builder builder) {
        this.user = builder.user;
        this.password = builder.password;
        this.connectionTimeout = builder.connectionTimeout;
        this.readTimeout = builder.readTimeout;
    }

    public static class Builder {

        private String user;

        private String password;

        private Long connectionTimeout;
        private Long readTimeout;

        public Builder() {

        }

        public OdlRpcClientConfig.Builder user(String user) {
            this.user = user;
            return this;
        }

        public OdlRpcClientConfig.Builder password(String password) {
            this.password = password;
            return this;
        }

        public OdlRpcClientConfig.Builder connectionTimeout(Long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public OdlRpcClientConfig.Builder readTimeout(Long readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }


        public OdlRpcClientConfig build() {
            return new OdlRpcClientConfig(this);
        }
    }
}
