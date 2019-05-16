package com.consul.docker.healthcheck.config;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Steven
 * @date 2019-05-16
 */
@Configuration
public class RootConfig {

	@Value("${consul.address}")
	private String address;

	@Bean
	public Consul consul() {
		Consul consul = Consul.builder().withHostAndPort(HostAndPort.fromString(address)).build();
		return consul;
	}

	@Bean
	public KeyValueClient keyValueClient(Consul consul) {
		return consul.keyValueClient();
	}

}
