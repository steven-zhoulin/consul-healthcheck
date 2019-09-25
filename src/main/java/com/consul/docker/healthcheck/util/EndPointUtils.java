package com.consul.docker.healthcheck.util;

import com.consul.docker.healthcheck.entity.EndPoint;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Steven
 */
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
}
