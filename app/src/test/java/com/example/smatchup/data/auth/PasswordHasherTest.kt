package com.example.smatchup.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun `newSalt produces 16 bytes base64 different each call`() {
        val s1 = PasswordHasher.newSalt()
        val s2 = PasswordHasher.newSalt()
        assertNotEquals(s1, s2)
        // base64 of 16 bytes = 24 chars with padding
        assertEquals(24, s1.length)
    }

    @Test
    fun `hash is deterministic for same password and salt`() {
        val salt = PasswordHasher.newSalt()
        val a = PasswordHasher.hash("hunter2", salt)
        val b = PasswordHasher.hash("hunter2", salt)
        assertEquals(a, b)
    }

    @Test
    fun `verify returns true with correct password`() {
        val salt = PasswordHasher.newSalt()
        val h = PasswordHasher.hash("hunter2", salt)
        assertTrue(PasswordHasher.verify("hunter2", salt, h))
    }

    @Test
    fun `verify returns false with wrong password`() {
        val salt = PasswordHasher.newSalt()
        val h = PasswordHasher.hash("hunter2", salt)
        assertFalse(PasswordHasher.verify("hunter3", salt, h))
    }

    @Test
    fun `different salts give different hashes for same password`() {
        val a = PasswordHasher.hash("hunter2", PasswordHasher.newSalt())
        val b = PasswordHasher.hash("hunter2", PasswordHasher.newSalt())
        assertNotEquals(a, b)
    }
}
