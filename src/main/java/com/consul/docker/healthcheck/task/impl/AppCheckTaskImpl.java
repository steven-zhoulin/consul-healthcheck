package com.consul.docker.healthcheck.task.impl;

import com.consul.docker.healthcheck.entity.EndPoint;
import com.consul.docker.healthcheck.task.IAppCheckTask;
import com.consul.docker.healthcheck.util.EndPointUtils;
import com.orbitz.consul.KeyValueClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * App 实例健康检查
 *
 * @author Steven
 * @date 2019-10-08
 */
@Slf4j
@Component
public class AppCheckTaskImpl implements IAppCheckTask {

    @Autowired
    private KeyValueClient keyValueClient;

    @Value("${healthcheck.app.prefix}")
    private String prefix;

    @Value("${healthcheck.retryCount}")
    private int retryCount;

    @Value("${healthcheck.root.context}")
    private Set<String> rootContexts;

    private Set<EndPoint> endPointSet = new TreeSet<>();

    /**
     * 上一次开始执行时间点之后多长时间再执行
     */
    @Override
    @Scheduled(initialDelayString = "${healthcheck.initialDelay}", fixedRateString = "${healthcheck.fixedRate}")
    public void scheduled() {
        List<String> modules;
        try {
            modules = keyValueClient.getKeys(prefix);
        } catch (Exception e) {
            log.error(e.getMessage() + " " + prefix + " 下无任何可用实例!");
            // 直接返回
            return;
        }

        List<EndPoint> endPoints = EndPointUtils.parse(modules, rootContexts);
        for (EndPoint endPoint : endPoints) {
            if (endPointSet.contains(endPoint)) {
                // 上一个周期，探测失败遗留下来的接入端点，沿用上一次的对象，因保有探测失败次数。
                continue;
            } else {
                endPointSet.add(endPoint);
            }
        }

        Iterator<EndPoint> iterator = endPointSet.iterator();
        while (iterator.hasNext()) {
            EndPoint endPoint = iterator.next();
            if (auok(endPoint)) {
                // 探测正常
                iterator.remove();
                continue;
            } else {
                int count = endPoint.incrFailCheckCount();
                if (count >= retryCount) {
                    // 探测失败次数 >= 阀值时
                    EndPointUtils.delete(endPoint, keyValueClient, prefix);
                    iterator.remove();
                }
            }
        }
    }

    /**
     * 检查端点的健康状况
     *
     * @param endPoint 接入端点
     * @return 返回健康状态 true: 正常；false: 异常
     */
    public boolean auok(EndPoint endPoint) {

        String ip = endPoint.getIp();
        int port = endPoint.getPort();

        String url = String.format("http://%s:%s/probe.jsp", ip, port);
        boolean status = EndPointUtils.doGet(url);
        log.info(String.format("%-50s  status: %s", url, status ? "up" : "down"));
        return status;

    }
}
