package com.springAI.cloudConfig;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.identity.IdentityClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class OCIConfig {
    @Bean
    public ConfigFileAuthenticationDetailsProvider provider() throws IOException {
        return new ConfigFileAuthenticationDetailsProvider(ConfigFileReader.parseDefault());
    }

    @Bean
    public IdentityClient IdentityClient(ConfigFileAuthenticationDetailsProvider provider) {
        return IdentityClient.builder().build(provider);
    }
}
