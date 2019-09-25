package com.consul.docker.healthcheck.task.impl;

import com.consul.docker.healthcheck.entity.EndPoint;
import com.consul.docker.healthcheck.task.IWebCheckTask;
import com.consul.docker.healthcheck.util.EndPointUtils;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Web 实例健康检查
 *
 * @author Steven
 * @date 2019-05-16
 */
@Slf4j
@Component
public class WebCheckTaskImpl implements IWebCheckTask {

	@Autowired
	private KeyValueClient keyValueClient;

	@Value("${healthcheck.web.prefix}")
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
		log.info("----------------------------------------------------------------");
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
					delete(endPoint);
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
		String moduleName = endPoint.getModuleName();
		String contextPath = "";
		if (!endPoint.isRootContext()) {
			contextPath = moduleName + "/";
		}

		String url = String.format("http://%s:%s/%sprobe.jsp", ip, port, contextPath);
		boolean status = doGet(url);
		log.info(String.format("%-50s  status: %s", url, status ? "up" : "down"));
		return status;

	}

	/**
	 * 不带参数的 GET 请求，如果状态码为 200，则返回 true，如果不为 200，则返回 false
	 *
	 * @param url
	 * @return
	 */
	private boolean doGet(String url) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(url);
		RequestConfig requestConfig = RequestConfig.custom()
			.setSocketTimeout(5000)
			.setConnectTimeout(2000)
			.setConnectionRequestTimeout(10000).build();
		httpGet.setConfig(requestConfig);

		CloseableHttpResponse response = null;
		try {
			response = httpClient.execute(httpGet);
			if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
				return true;
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			try {
				response.close();
				httpGet.abort();
				httpClient.close();
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
		return false;

	}

	/**
	 * 摘除检查不通过的访问端点
	 *
	 * @param endPoint
	 */
	private void delete(EndPoint endPoint) {
		String ip = endPoint.getIp();
		int port = endPoint.getPort();
		String moduleName = endPoint.getModuleName();
		String key = String.format("%s/%s/%s:%s", prefix, moduleName, ip, port);
		keyValueClient.deleteKey(key);
	}


}