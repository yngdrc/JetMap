package app.aventurine.jetmapdemo.data.models.config.serializer

import androidx.datastore.core.Serializer
import app.aventurine.jetmapdemo.data.models.config.entities.MapConfigLocalEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

object MapConfigJsonSerializer : Serializer<MapConfigLocalEntity> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    override val defaultValue: MapConfigLocalEntity = MapConfigLocalEntity.Empty

    override suspend fun readFrom(input: InputStream): MapConfigLocalEntity {
        try {
            val bytes = input.readBytes()
            if (bytes.isEmpty()) return defaultValue
            val text = bytes.toString(StandardCharsets.UTF_8)
            return json.decodeFromString(
                deserializer = MapConfigLocalEntity.serializer(),
                string = text
            )
        } catch (e: SerializationException) {
            throw e
        }
    }

    override suspend fun writeTo(
        t: MapConfigLocalEntity,
        output: OutputStream
    ) {
        val text = json.encodeToString(
            serializer = MapConfigLocalEntity.serializer(),
            value = t
        )

        withContext(Dispatchers.IO) {
            output.write(text.toByteArray(StandardCharsets.UTF_8))
        }
    }
}