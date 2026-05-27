package com.example.smatchup.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiResultTest {
    @Test
    fun `map transforms Success payload`() {
        val r: ApiResult<Int> = ApiResult.Success(2)
        val mapped = r.map { it * 3 }
        assertEquals(ApiResult.Success(6), mapped)
    }

    @Test
    fun `map leaves Error untouched`() {
        val r: ApiResult<Int> = ApiResult.Unauthorized
        val mapped = r.map { it * 3 }
        assertTrue(mapped is ApiResult.Unauthorized)
    }

    @Test
    fun `getOrNull returns data on Success and null otherwise`() {
        assertEquals(7, ApiResult.Success(7).getOrNull())
        assertNull(ApiResult.NotFound.getOrNull())
        assertNull(ApiResult.RateLimited(null).getOrNull())
    }
}
