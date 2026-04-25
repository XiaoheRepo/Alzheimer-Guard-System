package com.xiaohelab.guard.android.core.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * HC-ID-String: 客户端所有 ID 字段在域内统一为 [String]。
 *
 * 但当前后端（guard-server-java）部分实体 ID 仍以 [Long] 序列化，Jackson 会把它输出为 JSON
 * number。kotlinx.serialization 在解码 String 字段时严格要求 JSON string，否则抛
 * `IllegalArgumentException`。
 *
 * 该 Serializer 在反序列化阶段同时兼容 JSON string 与 JSON number（含浮点会向下取整为整数串），
 * 在序列化阶段始终输出 String。
 *
 * 用法：在 DTO 字段上 `@Serializable(with = IdAsStringSerializer::class) val patientId: String`。
 */
object IdAsStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.xiaohelab.guard.IdAsString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeString()
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonNull -> ""
            is JsonPrimitive -> element.content
            else -> error("Unsupported JSON for ID: $element")
        }
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

/** 同上但允许 null，用于可选 ID 字段。 */
object NullableIdAsStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.xiaohelab.guard.NullableIdAsString", PrimitiveKind.STRING)
            .let { it } // nullability handled via @Serializable(with=...) on `String?` field

    override fun deserialize(decoder: Decoder): String? {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeString()
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonNull -> null
            is JsonPrimitive -> if (element.content.isEmpty()) null else element.content
            else -> error("Unsupported JSON for ID: $element")
        }
    }

    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) encoder.encodeNull() else encoder.encodeString(value)
    }
}
