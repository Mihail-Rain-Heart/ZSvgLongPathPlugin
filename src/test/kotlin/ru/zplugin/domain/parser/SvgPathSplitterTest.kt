package ru.zplugin.domain.parser

import arrow.core.left
import arrow.core.right
import kotlin.test.Test
import kotlin.test.assertEquals
import ru.zplugin.zplugin.domain.parser.SvgPathError
import ru.zplugin.zplugin.domain.parser.SvgPathSplitter
import ru.zplugin.zplugin.domain.parser.SvgPathSplitter.Split

class SvgPathSplitterTest {
    @Test
    fun testNoSplitEOF() {
        val tests = listOf(
            "",
            "M10,10",
            "M10,10 Z",
            "M10,10 20,10 Z",
            "M10,10 L5,5 Z",
            "m1,1 l2,2 h1 v1 t1,1 q2,2,2,2 s1,1,1,1 c2,2,2,2,2,2 a1,1,1,1,1,1,1 z",
        )
        tests.forEach { input ->
            val splitter = SvgPathSplitter(input)
            val message = "Input: $input"
            assertEquals(SvgPathError.EOF.left(), splitter.next(), message)
            assertEquals(SvgPathError.EOF.left(), splitter.next(), message)
        }
    }

    @Test
    fun testNoSplitError() {
        class Test(
            val input: String,
            val error: SvgPathError,
        )
        val tests = listOf(
            Test(",", SvgPathError.MissingCommand),
            Test("5", SvgPathError.MissingCommand),
            Test("Z", SvgPathError.BadStart),
            Test("M", SvgPathError.MissingArgs),
            Test("M10,10,", SvgPathError.MissingArgs),
            Test("M10,10,20,20M", SvgPathError.MissingArgs),
        )
        tests.forEach { test ->
            val splitter = SvgPathSplitter(test.input)
            val message = "Input: ${test.input}"
            assertEquals(test.error.left(), splitter.next(), message)
            assertEquals(test.error.left(), splitter.next(), message)
        }
    }

    @Test
    fun testCloseSplit() {
        class Test(
            val input: String,
            val split: Split,
        )
        val tests = listOf(
            Test("M10,10Zv10", Split(7, 7, "M10,10")),
            Test(" M 10 10 Z v 10", Split(10, 11, "M10,10")),
            Test("M10,10 Z v10", Split(8, 9, "M10,10")),
            Test("M10,10 z v10", Split(8, 9, "M10,10")),
        )
        tests.forEach { test ->
            val splitter = SvgPathSplitter(test.input)
            val message = "Input: ${test.input}"
            assertEquals(test.split.right(), splitter.next(), message)
            assertEquals(SvgPathError.EOF.left(), splitter.next(), message)
        }
    }

    @Test
    fun testMoveSplit() {
        class Test(
            val input: String,
            val split: Split,
        )
        val tests = listOf(
            Test("M10,10 M20,20", Split(6, 7, "")),
            Test("M10,10 m20,20", Split(6, 13, "M30,30")),
            Test("M10,10 M20,20-5-5", Split(6, 7, "")),
            Test("M10,10 m20,20-5-5", Split(6, 13, "M30,30l"))
        )
        tests.forEach { test->
            val splitter = SvgPathSplitter(test.input)
            val message = "Input: ${test.input}"
            assertEquals(test.split.right(), splitter.next(), message)
            assertEquals(SvgPathError.EOF.left(), splitter.next(), message)
        }
    }

    @Test
    fun testCloseMoveSplit() {
        class Test(
            val input: String,
            val split: Split,
        )
        val tests = listOf(
            Test("M10,10 Z M20,20", Split(8, 9, "")),
            Test("M10,10 Z m20,20", Split(8, 15, "M30,30")),
            Test("M10,10 z M20,20", Split(8, 9, "")),
            Test("M10,10 z m20,20", Split(8, 15, "M30,30")),
            Test("M10,10 Z M20,20,-5,-5", Split(8, 9, "")),
            Test("M10,10 Z m20,20,-5,-5", Split(8, 16, "M30,30l")),
            Test("M10,10 z M20,20,-5,-5", Split(8, 9, "")),
            Test("M10,10 z m20,20,-5,-5", Split(8, 16, "M30,30l")),
            Test("M-10,-10 Z M-20,-20", Split(10, 11, "")),
            Test("M-10,-10 Z m-20,-20", Split(10, 19, "M-30-30")),
            Test("M-10,-10 z M-20,-20", Split(10, 11, "")),
            Test("M-10,-10 z m-20,-20", Split(10, 19, "M-30-30")),
            Test("M-10,-10 Z M-20,-20,5,5", Split(10, 11, "")),
            Test("M-10,-10 Z m-20,-20,5,5", Split(10, 20, "M-30-30l")),
            Test("M-10,-10 z M-20,-20,5,5", Split(10, 11, "")),
            Test("M-10,-10 z m-20,-20,5,5", Split(10, 20, "M-30-30l")),
        )
        tests.forEach { test->
            val splitter = SvgPathSplitter(test.input)
            val message = "Input: ${test.input}"
            assertEquals(test.split.right(), splitter.next(), message)
            assertEquals(SvgPathError.EOF.left(), splitter.next(), message)
        }
    }
}
