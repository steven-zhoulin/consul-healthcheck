package com.consul.docker.healthcheck.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Steven
 * @date 2019-05-16
 */
@Getter
@Setter
@ToString
public class EndPoint implements Comparable {

	/**
	 * 模块名
	 */
	private String moduleName;

	/**
	 * IP 地址
	 */
	public String ip;

	/**
	 * 端口号
	 */
	private int port;

	/**
	 * 是否为根上下文
	 */
	private boolean rootContext;

	/**
	 * 探测失败次数
	 */
	private int failCheckCount;

	public int incrFailCheckCount() {
		return this.failCheckCount++;
	}

	@Override
	public boolean equals(Object o) {
		EndPoint oEndPoint = (EndPoint) o;
		if (this.moduleName.equals(oEndPoint.getModuleName()) &&
			this.ip.equals(oEndPoint.getIp()) &&
			this.port == oEndPoint.getPort()) {
			return true;
		}

		return false;
	}

	@Override
	public int hashCode() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.moduleName).append(this.ip).append(this.port).append(this.rootContext);
		return sb.toString().hashCode();
	}

	@Override
	public int compareTo(Object o) {
		EndPoint oEndPoint = (EndPoint) o;
		String a = this.moduleName + this.ip + this.port;
		String b = oEndPoint.moduleName + oEndPoint.ip + oEndPoint.port;
		return a.compareTo(b);
	}
}
