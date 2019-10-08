package com.consul.docker.healthcheck.util;

import com.consul.docker.healthcheck.entity.EndPoint;
import com.orbitz.consul.KeyValueClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Steven
 */
@Slf4j
public final class EndPointUtils {

    /**
     * 路径示例:
     * <p>
     * crm-web/touchframe/
     * crm-web/touchframe/192.168.114.132:10001
     * crm-web/touchframe/192.168.114.132:10002
     * crm-web/touchframe/192.168.114.132:10003
     * crm-web/touchframe/192.168.114.132:10004
     *
     * @param modules
     */
    public static List<EndPoint> parse(List<String> modules, Set<String> rootContexts) {

        List<EndPoint> endPoints = new ArrayList<>(20);

        for (String line : modules) {

            String[] part = StringUtils.split(line, "/");
            if (3 == part.length) {
                String[] address = StringUtils.split(part[2], ":");
                if (2 == address.length) {

                    String moduleName = part[1];
                    String ip = address[0];
                    int port = Integer.parseInt(address[1]);

                    EndPoint endPoint = new EndPoint();
                    endPoint.setModuleName(moduleName);
                    endPoint.setIp(ip);
                    endPoint.setPort(port);
                    if (rootContexts.contains(moduleName)) {
                        endPoint.setRootContext(true);
                    } else {
                        endPoint.setRootContext(false);
                    }

                    endPoints.add(endPoint);
                }
            }
        }

        return endPoints;
    }

    /**
     * 不带参数的 GET 请求，如果状态码为 200，则返回 true，如果不为 200，则返回 false
     *
     * @param url
     * @return
     */
    public static boolean doGet(String url) {
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
    public static void delete(EndPoint endPoint, KeyValueClient keyValueClient, String prefix) {
        String ip = endPoint.getIp();
        int port = endPoint.getPort();
        String moduleName = endPoint.getModuleName();
        String key = String.format("%s/%s/%s:%s", prefix, moduleName, ip, port);
        keyValueClient.deleteKey(key);
    }
}
