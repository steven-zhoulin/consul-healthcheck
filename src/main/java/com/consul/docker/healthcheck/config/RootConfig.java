package com.consul.docker.healthcheck.config;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 基础配置文件
 *
 * @author Steven
 * @date 2019-05-16
 */
@Configuration
@Slf4j
public class RootConfig {

    @Value("${consul.address}")
    private String address;

    @Value("${consul.token}")
    private String token;

    @Value("${zookeeper.url}")
    private String zkUrl;

    @Bean
    public Consul consul() {

        log.info("consul cluster address: " + address);
        log.info("consul token: " + token);

        String[] hostAndPort = StringUtils.split(address, ',');
        List<HostAndPort> hostAndPortList = new ArrayList<>();
        for (String hap : hostAndPort) {
            hostAndPortList.add(HostAndPort.fromString(hap));
        }

        Consul consul = null;
        Consul.Builder builder = Consul.builder();

        if (StringUtils.isNoneBlank(token)) {
            builder.withAclToken(token);
        }

        if (hostAndPortList.size() == 1) {
            consul = builder.withHostAndPort(HostAndPort.fromString(address)).build();
        } else {
            consul = builder.withMultipleHostAndPort(hostAndPortList, 5000).build();
        }

        return consul;
    }

    @Bean
    public KeyValueClient keyValueClient(Consul consul) {
        return consul.keyValueClient();
    }

    @Bean
    public CuratorFramework zkClient() {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(2000, 3);
        CuratorFramework client = CuratorFrameworkFactory.newClient(zkUrl, retryPolicy);
        client.start();
        return client;
    }
}
