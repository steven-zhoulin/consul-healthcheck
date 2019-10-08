package com.consul.docker.healthcheck.controller;

import com.consul.docker.healthcheck.entity.EndPoint;
import com.consul.docker.healthcheck.util.EndPointUtils;
import com.orbitz.consul.KeyValueClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 获取容器对外暴露的接入地址
 *
 * @author Steven
 * @since 2019-09-25
 */
@RestController
@Slf4j
@RequestMapping("/api/endpoint")
public class EndpointController {

	@Autowired
	private KeyValueClient keyValueClient;

	@Autowired
	private CuratorFramework zkClient;

	@Value("${healthcheck.root.context}")
	private Set<String> rootContexts;

	@Value("${healthcheck.web.prefix}")
	private String prefix;

	@Value("${zookeeper.bashPath}")
	private String basePath;

	@Value("${zookeeper.app.groupname}")
	private Set<String> appGroupnames;

	/**
	 * 获取某个 Web 模块对应的所有容器接入地址。
	 *
	 * @param moduleName 模块名
	 * @return 接入地址集合
	 */
	@GetMapping("/crm-web/{moduleName}")
	public List<String> webEndPoints(@PathVariable("moduleName") String moduleName) {

		List<String> address = new ArrayList<>(20);

		List<String> modules;
		try {
			modules = keyValueClient.getKeys(prefix);
		} catch (Exception e) {
			log.error(e.getMessage() + " " + prefix + " 下无任何可用实例!");
			return address;
		}

		List<EndPoint> endPoints = EndPointUtils.parse(modules, rootContexts);

		for (EndPoint endPoint : endPoints) {
			if (moduleName.equals(endPoint.getModuleName())) {
				address.add("http://" + endPoint.getIp() + ":" + endPoint.getPort() + "/" + endPoint.getModuleName());
			}
		}

		return address;
	}

	/**
	 * 获取 app 对应的所有容器接入地址。
	 *
	 * ls /wade-relax/center/base/instances
	 * ls /wade-relax/center/itf/instances
	 * ls /wade-relax/center/flow/instances
	 *
	 * @return 接入地址集合
	 */
	@GetMapping("/crm-app")
	public List<String> appEndPoints() {

		log.info("app.groupnames: {}", appGroupnames);
		List<String> address = new ArrayList<>(20);

		for (String appGroupname : appGroupnames) {
			try {
				String path = basePath + "/center/" + appGroupname + "/instances";
				log.info("path: " + path);
				List<String> endPoints = zkClient.getChildren().forPath(path);
				endPoints.forEach(endPoint -> address.add("http://" + endPoint));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		Collections.sort(address);

		return address;
	}

}
