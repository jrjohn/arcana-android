package com.example.arcana.data.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for UserChange data class and ChangeType enum.
 */
class UserChangeTest {

    // ===================== ChangeType enum =====================

    @Test
    fun `ChangeType has CREATE value`() {
        assertNotNull(ChangeType.CREATE)
    }

    @Test
    fun `ChangeType has UPDATE value`() {
        assertNotNull(ChangeType.UPDATE)
    }

    @Test
    fun `ChangeType has DELETE value`() {
        assertNotNull(ChangeType.DELETE)
    }

    @Test
    fun `ChangeType has exactly 3 values`() {
        assertEquals(3, ChangeType.values().size)
    }

    @Test
    fun `ChangeType CREATE has correct name`() {
        assertEquals("CREATE", ChangeType.CREATE.name)
    }

    @Test
    fun `ChangeType UPDATE has correct name`() {
        assertEquals("UPDATE", ChangeType.UPDATE.name)
    }

    @Test
    fun `ChangeType DELETE has correct name`() {
        assertEquals("DELETE", ChangeType.DELETE.name)
    }

    @Test
    fun `ChangeType valueOf CREATE`() {
        assertEquals(ChangeType.CREATE, ChangeType.valueOf("CREATE"))
    }

    @Test
    fun `ChangeType valueOf UPDATE`() {
        assertEquals(ChangeType.UPDATE, ChangeType.valueOf("UPDATE"))
    }

    @Test
    fun `ChangeType valueOf DELETE`() {
        assertEquals(ChangeType.DELETE, ChangeType.valueOf("DELETE"))
    }

    @Test
    fun `ChangeType ordinals are sequential`() {
        val values = ChangeType.values()
        for (i in values.indices) {
            assertEquals(i, values[i].ordinal)
        }
    }

    @Test
    fun `ChangeType CREATE ordinal is 0`() {
        assertEquals(0, ChangeType.CREATE.ordinal)
    }

    @Test
    fun `ChangeType UPDATE ordinal is 1`() {
        assertEquals(1, ChangeType.UPDATE.ordinal)
    }

    @Test
    fun `ChangeType DELETE ordinal is 2`() {
        assertEquals(2, ChangeType.DELETE.ordinal)
    }

    // ===================== UserChange instantiation =====================

    private fun makeUserChange(
        id: Long = 0L,
        userId: Int = 1,
        type: ChangeType = ChangeType.CREATE,
        name: String? = null,
        job: String? = null
    ) = UserChange(id = id, userId = userId, type = type, name = name, job = job)

    @Test
    fun `UserChange can be instantiated`() {
        val change = makeUserChange()
        assertNotNull(change)
    }

    @Test
    fun `UserChange fields are accessible`() {
        val change = UserChange(
            id = 10L,
            userId = 42,
            type = ChangeType.UPDATE,
            name = "Alice",
            job = "Engineer"
        )
        assertEquals(10L, change.id)
        assertEquals(42, change.userId)
        assertEquals(ChangeType.UPDATE, change.type)
        assertEquals("Alice", change.name)
        assertEquals("Engineer", change.job)
    }

    @Test
    fun `UserChange name defaults to null`() {
        val change = makeUserChange()
        assertNull(change.name)
    }

    @Test
    fun `UserChange job defaults to null`() {
        val change = makeUserChange()
        assertNull(change.job)
    }

    @Test
    fun `UserChange id defaults to 0`() {
        val change = UserChange(userId = 1, type = ChangeType.CREATE)
        assertEquals(0L, change.id)
    }

    // ===================== Specific ChangeType instances =====================

    @Test
    fun `UserChange with CREATE type`() {
        val change = makeUserChange(type = ChangeType.CREATE, name = "NewUser", job = "Developer")
        assertEquals(ChangeType.CREATE, change.type)
        assertEquals("NewUser", change.name)
        assertEquals("Developer", change.job)
    }

    @Test
    fun `UserChange with UPDATE type`() {
        val change = makeUserChange(type = ChangeType.UPDATE, name = "UpdatedUser")
        assertEquals(ChangeType.UPDATE, change.type)
        assertEquals("UpdatedUser", change.name)
    }

    @Test
    fun `UserChange with DELETE type`() {
        val change = makeUserChange(type = ChangeType.DELETE)
        assertEquals(ChangeType.DELETE, change.type)
    }

    // ===================== equals & hashCode =====================

    @Test
    fun `UserChange equals works for identical objects`() {
        val c1 = makeUserChange(id = 1L, userId = 10, type = ChangeType.CREATE)
        val c2 = makeUserChange(id = 1L, userId = 10, type = ChangeType.CREATE)
        assertEquals(c1, c2)
    }

    @Test
    fun `UserChange not equals for different type`() {
        val c1 = makeUserChange(type = ChangeType.CREATE)
        val c2 = makeUserChange(type = ChangeType.DELETE)
        assertNotEquals(c1, c2)
    }

    @Test
    fun `UserChange not equals for different userId`() {
        val c1 = makeUserChange(userId = 1)
        val c2 = makeUserChange(userId = 2)
        assertNotEquals(c1, c2)
    }

    @Test
    fun `UserChange hashCode consistent`() {
        val c1 = makeUserChange()
        val c2 = makeUserChange()
        assertEquals(c1.hashCode(), c2.hashCode())
    }

    // ===================== copy =====================

    @Test
    fun `UserChange copy changes type`() {
        val original = makeUserChange(type = ChangeType.CREATE)
        val copy = original.copy(type = ChangeType.UPDATE)
        assertEquals(ChangeType.UPDATE, copy.type)
        assertEquals(original.userId, copy.userId)
    }

    @Test
    fun `UserChange copy changes name`() {
        val original = makeUserChange(name = "OldName")
        val copy = original.copy(name = "NewName")
        assertEquals("NewName", copy.name)
    }

    @Test
    fun `UserChange copy changes job`() {
        val original = makeUserChange(job = null)
        val copy = original.copy(job = "Manager")
        assertEquals("Manager", copy.job)
    }

    // ===================== toString =====================

    @Test
    fun `UserChange toString contains class name`() {
        val change = makeUserChange()
        assertTrue(change.toString().contains("UserChange"))
    }

    // ===================== component functions =====================

    @Test
    fun `UserChange component functions work`() {
        val change = UserChange(id = 5L, userId = 20, type = ChangeType.UPDATE, name = "Bob", job = "QA")
        val (id, userId, type, name, job) = change
        assertEquals(5L, id)
        assertEquals(20, userId)
        assertEquals(ChangeType.UPDATE, type)
        assertEquals("Bob", name)
        assertEquals("QA", job)
    }
}
