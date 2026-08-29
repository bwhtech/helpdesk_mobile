package io.github.kaulith.helpdeskanalytics.domain.model.report

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kotlinx.datetime.LocalDate

/**
 * Gson reflects into the `java.time.LocalDate` that kotlinx wraps and fails on
 * its private fields. ISO-8601 text is stable across builds and readable in the
 * stored template JSON.
 */
class LocalDateAdapter : TypeAdapter<LocalDate>() {

    override fun write(out: JsonWriter, value: LocalDate?) {
        out.value(value?.toString())
    }

    override fun read(reader: JsonReader): LocalDate? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        return runCatching { LocalDate.parse(reader.nextString()) }.getOrNull()
    }
}
