package com.consul.docker.healthcheck.task;

/**
 * App 健康检查接口
 *
 * @author Steven
 * @date 2019-10-08
 */
public interface IAppCheckTask {

    /**
     * 定义监控检查任务
     */
    void scheduled();
}
