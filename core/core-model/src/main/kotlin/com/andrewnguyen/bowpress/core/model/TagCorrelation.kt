package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Mirrors iOS `TagCorrelation`. Unlike most DTOs this one uses snake_case on the wire —
 * see the iOS `CodingKeys` for `tag_correlations`.
 */
@Serializable
data class TagCorrelation(
    @SerialName("bow_id") val bowId: String,
    @SerialName("user_id") val userId: String,
    val tag: String,
    @SerialName("tagged_session_count") val taggedSessionCount: Int,
    @SerialName("untagged_session_count") val untaggedSessionCount: Int,
    @SerialName("avg_score_tagged") val avgScoreTagged: Double? = null,
    @SerialName("avg_score_untagged") val avgScoreUntagged: Double? = null,
    @SerialName("score_delta") val scoreDelta: Double? = null,
    val strength: TagStrength,
    @SerialName("updated_at")
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
) {
    val id: String get() = "$bowId-$tag"
}
