package ru.zplugin.domain.helper

import kotlin.test.Test
import kotlin.test.asserter
import kotlinx.coroutines.runBlocking
import ru.zplugin.zplugin.domain.helper.SvgLongPathHelper

class SvgLongPathHelperTest {
    private val helper = SvgLongPathHelper()

    @Test
    fun testNoSplit() {
        runBlocking {
            val input = """<path android:fillColor="#3DDC84" android:pathData="M30,30v-10h-10z"/>"""
            val actual = helper.getSplitText(input) { _, _, _ -> }
            assertEquals(null, actual, input)
        }
    }

    @Test
    fun testOnlySplit() {
        runBlocking {
            val input = """
                |    <path android:fillColor="#3DDC84" android:pathData="M30,30v-10h-10zh10v10z"/>
                """.trimMargin()
            val expected = """
                |    <path android:fillColor="#3DDC84" android:pathData="M30,30v-10h-10z"/>
                |    <path android:fillColor="#3DDC84" android:pathData="M30,30h10v10z"/>
            """.trimMargin()
            val actual = helper.getSplitText(input) { _, _, _ -> }
            assertEquals(expected, actual, input)
        }
    }

    @Test
    fun testSplitInsideNotSplit() {
        runBlocking {
            val input = """
                |    <path android:fillColor="#3DDC84" android:pathData="M30,30"/>
                |    <path android:fillColor="#3DDC84" android:pathData="M30,30v-10h-10zh10v10z"/>
                |    <path android:fillColor="#3DDC84" android:pathData="M30,30"/>
                """.trimMargin()
            val expected = """
                |    <path android:fillColor="#3DDC84" android:pathData="M30,30"/>
                |    <path android:fillColor="#3DDC84" android:pathData="M30,30v-10h-10z"/>
                |    <path android:fillColor="#3DDC84" android:pathData="M30,30h10v10z"/>
                |    <path android:fillColor="#3DDC84" android:pathData="M30,30"/>
            """.trimMargin()
            val actual = helper.getSplitText(input) { _, _, _ -> }
            assertEquals(expected, actual, input)
        }
    }

    private fun assertEquals(expected: String?, actual: String?, input: String) {
        val error = {
            """
                |Original:
                |  $input
                |Expected:
                |  ${expected?.replace("\n", "\n  ")}
                |Actual:
                |  ${actual?.replace("\n", "\n  ")}
            """.trimMargin()
        }
        asserter.assertTrue(error, expected == actual)
    }
}
