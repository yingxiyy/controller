package net.flex.dci.otn.controller.auth.controller.oauth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0
 * @date 2022/4/19 10:02
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class KeyPairController {

    private final KeyPair keyPair;

    @RequestMapping(value = "/rsa/publicKey", method = RequestMethod.GET)
    public Map<String, Object> getKey() {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAKey rsaKey = new RSAKey.Builder(publicKey).build();
        return new JWKSet(rsaKey).toJSONObject();
    }

}
