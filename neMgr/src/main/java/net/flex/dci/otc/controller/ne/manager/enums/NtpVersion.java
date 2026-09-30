package net.flex.dci.otc.controller.ne.manager.enums;


import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpVersion.Ipv4;
import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpVersion.Ipv6;

import lombok.Getter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpVersion;

/**
 * special for the vendor name huawei
 *
 * @version 1.0
 * @date 12/23/2025 1:07 PM
 */
@Getter
public enum NtpVersion {
    NTP_V3("3", Ipv4),
    NTP_V4("4", Ipv6);

    private final String value;

    private final IpVersion ipVersion;

    NtpVersion(String value, IpVersion ipVersion) {
        this.value = value;
        this.ipVersion = ipVersion;
    }
}
