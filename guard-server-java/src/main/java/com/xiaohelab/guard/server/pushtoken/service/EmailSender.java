package com.xiaohelab.guard.server.pushtoken.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 邮件发送器（SMTP / 阿里云 DirectMail）。
 * <p>由 {@code spring-boot-starter-mail} 自动装配 {@link JavaMailSender}；
 * 当 {@code notification.email-enabled=false} 或 SMTP 用户名为空时走日志降级。</p>
 */
@Component
@EnableConfigurationProperties(EmailProperties.class)
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final EmailProperties props;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${notification.email-enabled:false}")
    private boolean featureEnabled;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    public EmailSender(EmailProperties props, ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.props = props;
        this.mailSenderProvider = mailSenderProvider;
    }

    public boolean isEnabled() {
        return featureEnabled
                && smtpUsername != null && !smtpUsername.isBlank()
                && mailSenderProvider.getIfAvailable() != null;
    }

    /**
     * 发送 HTML 邮件。
     * @return true=已交付 SMTP；false=降级日志
     */
    public boolean send(String to, String subject, String htmlBody) {
        if (!isEnabled()) {
            log.info("[Email] 降级（未启用）：to={} subject={}", to, subject);
            return false;
        }
        try {
            JavaMailSender sender = mailSenderProvider.getObject();
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.setFrom(new InternetAddress(props.getFromAddress(), props.getFromName(),
                    StandardCharsets.UTF_8.name()));
            sender.send(msg);
            log.info("[Email] sent to={} subject={}", to, subject);
            return true;
        } catch (Exception e) {
            log.error("[Email] 发送失败 to={} subject={} err={}", to, subject, e.getMessage());
            return false;
        }
    }
}
