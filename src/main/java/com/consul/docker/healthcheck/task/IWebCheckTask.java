package com.consul.docker.healthcheck.task;

/**
 * Web 健康检查接口
 *
 * @author Steven
 * @date 2019-05-16
 */
public interface IWebCheckTask {

	/**
	 * 定义监控检查任务
	 */
	void scheduled();
}
