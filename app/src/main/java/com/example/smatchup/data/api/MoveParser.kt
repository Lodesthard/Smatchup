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
            Move(
                id = id,
                displayName = name,
                category = inferCategory(id),
                frame = Frame(
                    startup = o.optInt("startup", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE },
                    totalFrames = o.optInt("totalFrames", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE },
                    landingLag = o.optInt("landingLag", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE },
                    baseDamage = if (o.has("baseDamage")) o.optDouble("baseDamage").toFloat() else null,
                ),
            )
        }
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
