package com.xiaohelab.guard.server.common.config;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.List;
import java.util.Set;

/**
 * 全局 Jackson 配置。
 *
 * <p>核心契约（SADD HC-ID-String / API V2.0 §1.1）：所有 ID 字段在 JSON 出站时必须是 string；
 * 入站允许 string/number 双形（Jackson 默认 ALLOW_COERCION_OF_SCALARS=true 已涵盖）。</p>
 *
 * <p>实现策略：注册 {@link BeanSerializerModifier}，对类型为 {@code Long}/{@code long} 且
 * 序列化后 JSON 键名命中 ID 规则的字段，强制使用 {@link ToStringSerializer} 输出为字符串。</p>
 *
 * <p>规则：JSON 键名以 {@code _id} 结尾（SNAKE_CASE 后），或在显式白名单 {@link #EXTRA_ID_NAMES}
 * 中（应对 reported_by / created_by / target_user_id 这类 Long 用户引用字段）。</p>
 */
@Configuration
public class JacksonConfig {

    /**
     * 显式白名单：JSON 键名（snake_case 后）不以 _id 结尾、但语义为 Long 实体引用的字段。
     * 不在白名单内的 Long 字段（如 profile_version / version / page_size）保持数值输出。
     */
    private static final Set<String> EXTRA_ID_NAMES = Set.of(
            "reported_by",
            "created_by",
            "updated_by",
            "operator_user_id",
            "target_user_id",
            "from_user_id",
            "to_user_id",
            "creator_user_id",
            "primary_guardian_user_id"
    );

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return (Jackson2ObjectMapperBuilder b) -> b
                .modules(new JavaTimeModule(), longIdToStringModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private SimpleModule longIdToStringModule() {
        SimpleModule m = new SimpleModule("LongIdToStringModule");
        m.setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                             BeanDescription beanDesc,
                                                             List<BeanPropertyWriter> beanProperties) {
                for (BeanPropertyWriter w : beanProperties) {
                    Class<?> raw = w.getType().getRawClass();
                    if ((raw == Long.class || raw == long.class) && shouldStringify(w.getName())) {
                        w.assignSerializer(ToStringSerializer.instance);
                    }
                }
                return beanProperties;
            }
        });
        return m;
    }

    private static boolean shouldStringify(String jsonName) {
        if (jsonName == null) return false;
        if (jsonName.endsWith("_id")) return true;
        return EXTRA_ID_NAMES.contains(jsonName);
    }
}
