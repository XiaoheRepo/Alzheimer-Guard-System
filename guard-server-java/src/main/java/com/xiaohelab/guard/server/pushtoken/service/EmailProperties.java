package com.xiaohelab.guard.server.pushtoken.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 邮件发送配置（backend_handbook §19.6）。 */
@ConfigurationProperties(prefix = "notification.email")
public class EmailProperties {

    private String fromAddress = "noreply@alzheimer-rescue.com";
    private String fromName = "阿尔兹海默症协同寻回系统";

    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
}
