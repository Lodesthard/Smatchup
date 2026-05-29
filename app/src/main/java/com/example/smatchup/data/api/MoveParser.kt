package com.example.smatchup.data.api

import com.example.smatchup.domain.model.Frame
import com.example.smatchup.domain.model.Move
import com.example.smatchup.domain.model.MoveCategory
import org.json.JSONException
import org.json.JSONObject

object MoveParser {

    fun parse(json: String): List<Move> {
        val root = try { JSONObject(json) } catch (_: JSONException) { return emptyList() }
        val arr = root.optJSONArray("moves") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val name = o.optString("moveName", "").ifBlank { return@mapNotNull null }
            val id = name.lowercase().replace(Regex("\\s+"), "_")
            val startup = o.optInt("startup", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
            val total = o.optInt("totalFrames", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
            val damage = if (o.has("baseDamage")) o.optDouble("baseDamage").toFloat() else null
            Move(
                id = id,
                displayName = name,
                category = inferCategory(id),
                frame = Frame(
                    startup = startup,
                    totalFrames = total,
                    landingLag = o.optInt("landingLag", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE },
                    endLag = o.optInt("endLag", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
                        ?: computeEndLag(startup, total),
                    onShield = o.optInt("onShield", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
                        ?: computeOnShield(startup, total, damage),
                    baseDamage = damage,
                ),
            )
        }
    }

    /** End lag ≈ frames from the move's start until it is over (total − startup). */
    private fun computeEndLag(startup: Int?, total: Int?): Int? =
        if (startup != null && total != null) (total - startup).coerceAtLeast(0) else null

    /**
     * Estimated on-shield advantage: shieldstun − recovery after contact, assuming the move
     * connects on its startup frame. shieldstun ≈ floor(damage × 0.45 + 2) (SSBU approximation).
     * Negative = punishable on block.
     */
    private fun computeOnShield(startup: Int?, total: Int?, damage: Float?): Int? {
        if (startup == null || total == null || damage == null) return null
        val shieldstun = Math.floor(damage * 0.45 + 2.0).toInt()
        val recoveryAfterContact = (total - startup).coerceAtLeast(0)
        return shieldstun - recoveryAfterContact
    }

    private fun inferCategory(id: String): MoveCategory = when {
        id == "jab" || id.endsWith("_jab") -> MoveCategory.JAB
        id.contains("smash") -> MoveCategory.SMASH
        id.contains("aerial") || id == "fair" || id == "bair" || id == "uair" || id == "dair" || id == "nair" -> MoveCategory.AERIAL
        id.contains("special") || id == "neutral_b" || id == "side_b" || id == "up_b" || id == "down_b" -> MoveCategory.SPECIAL
        id.contains("throw") -> MoveCategory.THROW
        id.contains("grab") -> MoveCategory.GRAB
        id.contains("tilt") -> MoveCategory.TILT
        else -> MoveCategory.MOVEMENT
    }
}
