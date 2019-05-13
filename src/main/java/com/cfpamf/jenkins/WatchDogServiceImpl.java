package com.cfpamf.jenkins;

import com.alibaba.fastjson.JSON;
import hudson.ProxyConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * 通知器真正调度的服务，这里主要是向某个接口发送构建结果信息
 *
 * @author flavone
 * @date 2019/03/27
 */
public class WatchDogServiceImpl implements WatchDogService {
    private Logger logger = LoggerFactory.getLogger(WatchDogService.class);

    private TaskListener listener;

    private AbstractBuild build;

    private String apiUrl;

    private String microServiceId;

    private String signature;

    public WatchDogServiceImpl(
            TaskListener listener,
            AbstractBuild build,
            String apiUrl,
            String microServiceId, String signature) {
        this.listener = listener;
        this.build = build;
        this.apiUrl = apiUrl;
        this.microServiceId = microServiceId;
        this.signature = signature;
    }


    @Override
    public void send(Boolean result) {
        sendDataToReport(buildReportRequest(result));
    }

    private void sendDataToReport(ReportRequest request) {
        this.listener.getLogger().println("WatchDog: 开始推送构建消息到ARK...");
        HttpClient client = getHttpClient();
        PostMethod post = new PostMethod(apiUrl);
        String body = JSON.toJSONString(request);
        this.listener.getLogger().println("推送内容：\n" + body);
        try {
            post.setRequestEntity(new StringRequestEntity(body, "application/json", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            logger.error("WatchDog: build request error", e);
        }
        try {
            client.executeMethod(post);
            String responseBody = post.getResponseBodyAsString();
            logger.debug(responseBody);
            if (isSuccess(responseBody)) {
                this.listener.getLogger().println("WatchDog: 已成功推送构建消息到ARK！");
            } else {
                this.listener.getLogger().println("WatchDog: 推送构建消息到ARK失败！返回结果如下：\n" + responseBody);
            }
        } catch (IOException e) {
            logger.error("WatchDog: send msg error", e);
        }
        post.releaseConnection();
    }

    private boolean isSuccess(String response) {
        return JSON.parseObject(response).getBooleanValue("success");
    }

    /**
     * 获取当前构建的Url
     *
     * @return
     */
    private String getBuildUrl() {
        return build.getAbsoluteUrl();
    }

    /**
     * 根据当前构建数据拼接待发送的数据对象
     *
     * @param result
     * @return
     */
    private ReportRequest buildReportRequest(Boolean result) {
        Integer costTime = Integer.valueOf((int) build.getDuration());
        String detail = getBuildUrl();
        String jobName = build.getFullDisplayName();
        String context = build.getBuildStatusSummary().message;
        ReportRequest request = new ReportRequest();
        request.setContext(context);
        request.setCostTime(costTime);
        request.setDetail(detail);
        request.setJobName(jobName);
        request.setMicroServiceId(Integer.valueOf(microServiceId));
        request.setRequestId(UUID.randomUUID().toString());
        request.setResult(result);
        request.setSignature(signature);
        return request;
    }

    /**
     * 使用Jenkins代理
     *
     * @return
     */
    private HttpClient getHttpClient() {
        HttpClient client = new HttpClient();
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins.proxy != null) {
            ProxyConfiguration proxy = jenkins.proxy;
            if (proxy != null && client.getHostConfiguration() != null) {
                client.getHostConfiguration().setProxy(proxy.name, proxy.port);
                String username = proxy.getUserName();
                String password = proxy.getPassword();
                if (username != null && !"".equals(username.trim())) {
                    logger.info("Using proxy authentication (user=" + username + ")");
                    client.getState().setProxyCredentials(AuthScope.ANY,
                            new UsernamePasswordCredentials(username, password));
                }
            }
        }
        return client;
    }
}
