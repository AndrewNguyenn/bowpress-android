package com.andrewnguyen.bowpress.core.database

import androidx.room.TypeConverter
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.model.DeliveryType
import com.andrewnguyen.bowpress.core.model.FletchingType
import com.andrewnguyen.bowpress.core.model.RearStabSide
import com.andrewnguyen.bowpress.core.model.ShootingDistance
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.Zone
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * Room type converters. Strings for enums (mirrors iOS `*Str` columns in `PersistentModels`),
 * epoch-millis for `Instant`, JSON for `List<String>`.
 */
class Converters {

    // ---- Instant -------------------------------------------------------------

    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? =
        value?.let { Instant.ofEpochMilli(it) }

    // ---- Enums ---------------------------------------------------------------

    @TypeConverter
    fun bowTypeToString(value: BowType?): String? = value?.name

    @TypeConverter
    fun stringToBowType(value: String?): BowType? =
        value?.let { runCatching { BowType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun rearStabSideToString(value: RearStabSide?): String? = value?.name

    @TypeConverter
    fun stringToRearStabSide(value: String?): RearStabSide? =
        value?.let { runCatching { RearStabSide.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun zoneToString(value: Zone?): String? = value?.name

    @TypeConverter
    fun stringToZone(value: String?): Zone? =
        value?.let { runCatching { Zone.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fletchingTypeToString(value: FletchingType?): String? = value?.name

    @TypeConverter
    fun stringToFletchingType(value: String?): FletchingType? =
        value?.let { runCatching { FletchingType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun deliveryTypeToString(value: DeliveryType?): String? = value?.name

    @TypeConverter
    fun stringToDeliveryType(value: String?): DeliveryType? =
        value?.let { runCatching { DeliveryType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun targetFaceTypeToString(value: TargetFaceType?): String? = value?.name

    @TypeConverter
    fun stringToTargetFaceType(value: String?): TargetFaceType? =
        value?.let { runCatching { TargetFaceType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun shootingDistanceToString(value: ShootingDistance?): String? = value?.name

    @TypeConverter
    fun stringToShootingDistance(value: String?): ShootingDistance? =
        value?.let { runCatching { ShootingDistance.valueOf(it) }.getOrNull() }

    // ---- List<String> -------------------------------------------------------

    @TypeConverter
    fun stringListToJson(value: List<String>?): String =
        if (value == null) "[]" else json.encodeToString(ListSerializer(String.serializer()), value)

    @TypeConverter
    fun jsonToStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), value)
        }.getOrElse { emptyList() }
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
