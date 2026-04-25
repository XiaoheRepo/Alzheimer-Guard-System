package com.xiaohelab.guard.server.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 高德地图 Web 服务客户端（backend_handbook §3.4.5、§19.7）。
 * <ul>
 *   <li>{@link #convert(double, double, String)} — 坐标系转换 GCJ-02 ↔ WGS-84</li>
 *   <li>{@link #regeo(double, double)} — 逆地理编码：坐标 → 文字描述</li>
 * </ul>
 * <p>Redis 自保限流：{@code amap:quota:yyyyMMdd} 每日上限（amap.daily-limit）。</p>
 */
@Component
@EnableConfigurationProperties(AmapProperties.class)
public class AmapClient {

    private static final Logger log = LoggerFactory.getLogger(AmapClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AmapProperties props;
    private final StringRedisTemplate redis;
    private final HttpClient http;

    public AmapClient(AmapProperties props, StringRedisTemplate redis) {
        this.props = props;
        this.redis = redis;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getTimeoutMs()))
                .build();
        if (!props.isEnabled()) {
            log.warn("[AMap] amap.api-key 未配置，地图功能将走降级（直接返回原坐标 / 空地址）");
        } else {
            log.info("[AMap] 已就绪 base={} daily-limit={}", props.getBaseUrl(), props.getDailyLimit());
        }
    }

    public boolean isEnabled() { return props.isEnabled(); }

    /**
     * 坐标转换。
     * @param lng       经度
     * @param lat       纬度
     * @param fromType  源坐标系：gps（WGS-84）、mapbar、baidu；高德目标固定为 GCJ-02
     * @return double[]{newLng, newLat}；失败/降级返回原坐标
     */
    public double[] convert(double lng, double lat, String fromType) {
        if (!isEnabled() || !tryAcquireQuota()) {
            return new double[]{lng, lat};
        }
        try {
            String url = props.getBaseUrl() + "/assistant/coordinate/convert"
                    + "?key=" + enc(props.getApiKey())
                    + "&locations=" + lng + "," + lat
                    + "&coordsys=" + (fromType == null ? "gps" : fromType);
            JsonNode root = doGet(url);
            if (root != null && "1".equals(root.path("status").asText())) {
                String locs = root.path("locations").asText();
                String[] xy = locs.split(",");
                if (xy.length == 2) {
                    return new double[]{Double.parseDouble(xy[0]), Double.parseDouble(xy[1])};
                }
            }
            log.warn("[AMap] convert 返回异常：{}", root);
        } catch (Exception e) {
            log.error("[AMap] convert 失败 lng={} lat={} err={}", lng, lat, e.getMessage());
        }
        return new double[]{lng, lat};
    }

    /**
     * 逆地理编码：坐标 → 结构化地址描述。
     * @return formatted_address；失败返回 empty
     */
    public Optional<String> regeo(double lng, double lat) {
        if (!isEnabled() || !tryAcquireQuota()) return Optional.empty();
        try {
            String url = props.getBaseUrl() + "/geocode/regeo"
                    + "?key=" + enc(props.getApiKey())
                    + "&location=" + lng + "," + lat
                    + "&extensions=base&output=json";
            JsonNode root = doGet(url);
            if (root != null && "1".equals(root.path("status").asText())) {
                String addr = root.path("regeocode").path("formatted_address").asText(null);
                if (addr != null && !addr.isBlank()) return Optional.of(addr);
            }
            log.warn("[AMap] regeo 返回异常：{}", root);
        } catch (Exception e) {
            log.error("[AMap] regeo 失败 lng={} lat={} err={}", lng, lat, e.getMessage());
        }
        return Optional.empty();
    }

    private JsonNode doGet(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(props.getTimeoutMs()))
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            log.warn("[AMap] HTTP {} url={}", resp.statusCode(), url);
            return null;
        }
        return MAPPER.readTree(resp.body());
    }

    /** 基于 Redis 的每日额度自保（key: amap:quota:yyyyMMdd）。 */
    private boolean tryAcquireQuota() {
        try {
            String key = "amap:quota:" + LocalDate.now().toString().replace("-", "");
            Long n = redis.opsForValue().increment(key);
            if (n != null && n == 1L) {
                redis.expire(key, Duration.ofDays(2));
            }
            if (n != null && n > props.getDailyLimit()) {
                log.warn("[AMap] 已达每日限额 {}，本次拒绝调用", props.getDailyLimit());
                return false;
            }
            return true;
        } catch (Exception e) {
            // Redis 异常不阻塞业务，放行
            return true;
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
