package com.cfpamf.jenkins;

import lombok.Data;

/**
 * 推送给报表服务的信息
 *
 * @author flavone
 * @date 2019/03/27
 */
@Data
public class ReportRequest {
    /**
     * 具体信息
     */
    private String context;

    /**
     * 构建耗时
     */
    private Integer costTime;

    /**
     * 构建详情链接
     */
    private String detail;

    /**
     * 构建Job名称
     */
    private String jobName;

    /**
     * 微服务ID
     */
    private Integer microServiceId;

    /**
     * 请求ID，UUID唯一
     */
    private String requestId;

    /**
     * 构建结果
     */
    private Boolean result;

    /**
     * 微服务对应的签名信息
     */
    private String signature;
}
