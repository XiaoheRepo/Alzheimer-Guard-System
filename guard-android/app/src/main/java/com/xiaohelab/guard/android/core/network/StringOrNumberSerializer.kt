package com.xiaohelab.guard.android.core.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/**
 * Deserializes a field that the server may return as either a JSON string OR a JSON number,
 * and normalises it to [String].
 *
 * The API spec (§1.1) mandates all ID fields as strings.  However the current server build
 * returns some IDs (e.g. `user_id`) as bare integers.  This adapter bridges the gap without
 * requiring a server-side fix, and is applied to every `*_id` / `*_code` field that is
 * observed to deviate (RFC-001).
 *
 * Serialization always writes a proper JSON string.
 */
object StringOrNumberSerializer : KSerializer<String> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringOrNumber", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeString()          // non-JSON back-end (e.g. mock)
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> element.content       // works for both "3" and 3
            else -> element.toString()
        }
    }

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}
