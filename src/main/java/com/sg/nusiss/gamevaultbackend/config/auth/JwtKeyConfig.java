package com.sg.nusiss.gamevaultbackend.config.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Configuration
public class JwtKeyConfig {

    @Bean
    public RSAKey rsaJwk(RsaKeyProperties props) throws IOException {
        try (InputStream pubStream = props.getPublicKey().getInputStream();
             InputStream priStream = props.getPrivateKey().getInputStream()) {

            RSAPublicKey pub = (RSAPublicKey) RsaKeyConverters.x509().convert(pubStream);
            RSAPrivateKey pri = (RSAPrivateKey) RsaKeyConverters.pkcs8().convert(priStream);

            return new RSAKey.Builder(pub)
                    .privateKey(pri)
                    .keyID(UUID.randomUUID().toString())
                    .build();
        }
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAKey rsa) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsa)));
    }

    // Self-validation for this service (/me etc.)
    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsa) {
        try {
            return NimbusJwtDecoder.withPublicKey(rsa.toRSAPublicKey()).build();
        } catch (JOSEException e) {
            // Wrap as unchecked exception to avoid continuing throws in bean method
            throw new IllegalStateException("Failed to create JwtDecoder from RSAKey", e);
        }
    }
}
