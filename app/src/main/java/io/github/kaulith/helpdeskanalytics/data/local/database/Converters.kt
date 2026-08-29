package io.github.kaulith.helpdeskanalytics.data.local.database

import androidx.room.TypeConverter
import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.datetime.Instant
import java.lang.reflect.Type

private val STRING_LIST_TYPE: Type = object : TypeToken<List<String>>() {}.type

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilliseconds()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun fromStringList(value: List<String>?): String = gson.toJson(value ?: emptyList<String>())

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank() || value == "[]") return emptyList()
        return gson.fromJson(value, STRING_LIST_TYPE) ?: emptyList()
    }

    @TypeConverter
    fun fromStatus(value: Status): String = value.value

    @TypeConverter
    fun toStatus(value: String): Status = try {
        Status.fromValue(value)
    } catch (_: IllegalArgumentException) {
        Status.OPEN
    }

    @TypeConverter
    fun fromPriority(value: Priority): String = value.value

    @TypeConverter
    fun toPriority(value: String): Priority = try {
        Priority.fromValue(value)
    } catch (_: IllegalArgumentException) {
        Priority.MEDIUM
    }
}
