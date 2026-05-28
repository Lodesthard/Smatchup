package com.example.smatchup.data.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.smatchup.domain.model.MoveCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MoveParserTest {

    private val sample = """
        {"fighter":"Steve","moves":[
          {"moveName":"jab","startup":3,"totalFrames":12,"baseDamage":2.0},
          {"moveName":"fair","startup":8,"totalFrames":35,"baseDamage":7.5},
          {"moveName":"up smash","startup":11,"totalFrames":50,"baseDamage":15.0},
          {"moveName":"neutral b","startup":20,"totalFrames":60}
        ]}
    """.trimIndent()

    @Test fun parsesAllMovesWithFields() {
        val moves = MoveParser.parse(sample)
        assertEquals(4, moves.size)
        val fair = moves.first { it.id == "fair" }
        assertEquals("fair", fair.displayName)
        assertEquals(8, fair.frame.startup)
        assertEquals(35, fair.frame.totalFrames)
        assertEquals(7.5f, fair.frame.baseDamage!!, 0.0001f)
    }

    @Test fun inferCategoryFromName() {
        val moves = MoveParser.parse(sample)
        assertEquals(MoveCategory.JAB,     moves.first { it.id == "jab" }.category)
        assertEquals(MoveCategory.AERIAL,  moves.first { it.id == "fair" }.category)
        assertEquals(MoveCategory.SMASH,   moves.first { it.id == "up_smash" }.category)
        assertEquals(MoveCategory.SPECIAL, moves.first { it.id == "neutral_b" }.category)
    }

    @Test fun idIsLowercaseUnderscored() {
        val moves = MoveParser.parse(sample)
        assertTrue("up_smash" in moves.map { it.id })
        assertTrue("neutral_b" in moves.map { it.id })
    }

    @Test fun malformedJsonReturnsEmpty() {
        assertEquals(emptyList<Any>(), MoveParser.parse("not json"))
    }

    @Test fun emptyMovesArrayReturnsEmpty() {
        assertEquals(emptyList<Any>(), MoveParser.parse("""{"moves":[]}"""))
    }
}
