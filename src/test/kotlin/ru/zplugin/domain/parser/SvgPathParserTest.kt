package ru.zplugin.domain.parser

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import ru.zplugin.zplugin.domain.parser.SvgPathCommand
import ru.zplugin.zplugin.domain.parser.SvgPathError
import ru.zplugin.zplugin.domain.parser.SvgPathParser

class SvgPathParserTest {
    @Test
    fun testEmpty() {
        val tests = listOf(
            "",
            "    ",
            " \n \r \t ",
        )
        tests.forEach { input ->
            val parser = SvgPathParser(input)
            val message = "Input: $input"
            assertEquals(SvgPathError.EOF.left(), parser.read(), message)
            assertEquals(SvgPathError.EOF.left(), parser.read(), message)
        }
    }

    @Test
    fun testCommands() {
        class Test(
            val input: String,
            val output: SvgPathCommand,
        )

        val tests = listOf(
            Test("Z", close(0..0, false)),
            Test("z", close(0..0, true)),
            Test("H10", hLine(0..2, false, "10")),
            Test("h10", hLine(0..2, true, "10")),
            Test("V10", vLine(0..2, false, "10")),
            Test("v10", vLine(0..2, true, "10")),
            Test("M10,20", move(0..5, false, "10", "20")),
            Test("m10,20", move(0..5, true, "10", "20")),
            Test("L10,20", line(0..5, false, "10", "20")),
            Test("l10,20", line(0..5, true, "10", "20")),
            Test("T10,20", sqCurve(0..5, false, "10", "20")),
            Test("t10,20", sqCurve(0..5, true, "10", "20")),
            Test("S10,20,30,40", sCurve(0..11, false, "10", "20", "30", "40")),
            Test("s10,20,30,40", sCurve(0..11, true, "10", "20", "30", "40")),
            Test("Q10,20,30,40", qCurve(0..11, false, "10", "20", "30", "40")),
            Test("q10,20,30,40", qCurve(0..11, true, "10", "20", "30", "40")),
            Test("C10,20,30,40,50,60", curve(0..17, false, "10", "20", "30", "40", "50", "60")),
            Test("c10,20,30,40,50,60", curve(0..17, true, "10", "20", "30", "40", "50", "60")),
            Test("A10,20,30,1,0,60,70", arc(0..18, false, "10", "20", "30", true, false, "60", "70")),
            Test("a10,20,30,0,1,60,70", arc(0..18, true, "10", "20", "30", false, true, "60", "70")),
        )
        tests.forEach { test ->
            val parser = SvgPathParser(test.input)
            val message = "Input: ${test.input}"
            assertEquals(test.output.right(), parser.read(), message)
            assertEquals(SvgPathError.EOF.left(), parser.read(), message)
            assertEquals(SvgPathError.EOF.left(), parser.read(), message)
        }
    }

    @Test
    fun testSingleMissing() {
        class Test(
            val input: String,
            val error: SvgPathError,
        )

        val tests = listOf(
            Test(",", SvgPathError.MissingCommand),
            Test(",  ", SvgPathError.MissingCommand),
            Test("1  ", SvgPathError.MissingCommand),
            Test("M1  ", SvgPathError.MissingArgs),
            Test("m1  ", SvgPathError.MissingArgs),
            Test("M1,  ", SvgPathError.MissingArgs),
            Test("m1,  ", SvgPathError.MissingArgs),
            Test("M,1,1,  ", SvgPathError.MissingArgs),
            Test("m,1,1,  ", SvgPathError.MissingArgs),
            Test("H", SvgPathError.MissingArgs),
            Test("h", SvgPathError.MissingArgs),
            Test("V", SvgPathError.MissingArgs),
            Test("v", SvgPathError.MissingArgs),
            Test("L1", SvgPathError.MissingArgs),
            Test("l1", SvgPathError.MissingArgs),
            Test("T1", SvgPathError.MissingArgs),
            Test("T1", SvgPathError.MissingArgs),
            Test("Q1,1,1", SvgPathError.MissingArgs),
            Test("q1,1,1", SvgPathError.MissingArgs),
            Test("S1,1,1", SvgPathError.MissingArgs),
            Test("S1,1,1", SvgPathError.MissingArgs),
            Test("C1,1,1,1,1", SvgPathError.MissingArgs),
            Test("C1,1,1,1,1", SvgPathError.MissingArgs),
            Test("A+1,1,1,1,1,1,1", SvgPathError.MissingArgs),
            Test("a+1,1,1,1,1,1,1", SvgPathError.MissingArgs),
            Test("A1,+1,1,1,1,1,1", SvgPathError.MissingArgs),
            Test("a1,+1,1,1,1,1,1", SvgPathError.MissingArgs),
            Test("A1,1,1,+1,1,1,1", SvgPathError.MissingArgs),
            Test("a1,1,1,+1,1,1,1", SvgPathError.MissingArgs),
            Test("A1,1,1,1,+1,1,1", SvgPathError.MissingArgs),
            Test("a1,1,1,1,+1,1,1", SvgPathError.MissingArgs),
            Test("A1,1,1,2,1,1,1", SvgPathError.MissingArgs),
            Test("a1,1,1,2,1,1,1", SvgPathError.MissingArgs),
            Test("A1,1,1,1,2,1,1", SvgPathError.MissingArgs),
            Test("a1,1,1,1,2,1,1", SvgPathError.MissingArgs),
            Test("A1,1,1,1,1,1", SvgPathError.MissingArgs),
            Test("a1,1,1,1,1,1", SvgPathError.MissingArgs),
        )
        tests.forEach { test ->
            val parser = SvgPathParser(test.input)
            val message = "Input: ${test.input}"
            assertEquals(test.error.left(), parser.read(), message)
            assertEquals(test.error.left(), parser.read(), message)
        }
    }

    @Test
    fun testSubsequent() {
        class Test(
            val input: String,
            val output: List<SvgPathCommand>,
        )

        val tests = listOf(
            Test(
                "H10,20",
                listOf(
                    hLine(0..2, false, "10"),
                    hLine(4..5, false, "20", true),
                )
            ),
            Test(
                "h10,20",
                listOf(
                    hLine(0..2, true, "10"),
                    hLine(4..5, true, "20", true),
                )
            ),
            Test(
                "V10,20",
                listOf(
                    vLine(0..2, false, "10"),
                    vLine(4..5, false, "20", true),
                )
            ),
            Test(
                "v10,20",
                listOf(
                    vLine(0..2, true, "10"),
                    vLine(4..5, true, "20", true),
                )
            ),
            Test(
                "M10,20,30,40",
                listOf(
                    move(0..5, false, "10", "20"),
                    line(7..11, false, "30", "40", true),
                )
            ),
            Test(
                "m10,20,30,40",
                listOf(
                    move(0..5, true, "10", "20"),
                    line(7..11, true, "30", "40", true),
                )
            ),
            Test(
                "L10,20,30,40",
                listOf(
                    line(0..5, false, "10", "20"),
                    line(7..11, false, "30", "40", true),
                )
            ),
            Test(
                "l10,20,30,40",
                listOf(
                    line(0..5, true, "10", "20"),
                    line(7..11, true, "30", "40", true),
                )
            ),
            Test(
                "T10,20,30,40",
                listOf(
                    sqCurve(0..5, false, "10", "20"),
                    sqCurve(7..11, false, "30", "40", true),
                )
            ),
            Test(
                "t10,20,30,40",
                listOf(
                    sqCurve(0..5, true, "10", "20"),
                    sqCurve(7..11, true, "30", "40", true),
                )
            ),
            Test(
                "S10,20,30,40,50,60,70,80",
                listOf(
                    sCurve(0..11, false, "10", "20", "30", "40"),
                    sCurve(13..23, false, "50", "60", "70", "80", true),
                )
            ),
            Test(
                "s10,20,30,40,50,60,70,80",
                listOf(
                    sCurve(0..11, true, "10", "20", "30", "40"),
                    sCurve(13..23, true, "50", "60", "70", "80", true),
                )
            ),
            Test(
                "Q10,20,30,40,50,60,70,80",
                listOf(
                    qCurve(0..11, false, "10", "20", "30", "40"),
                    qCurve(13..23, false, "50", "60", "70", "80", true),
                )
            ),
            Test(
                "q10,20,30,40,50,60,70,80",
                listOf(
                    qCurve(0..11, true, "10", "20", "30", "40"),
                    qCurve(13..23, true, "50", "60", "70", "80", true),
                )
            ),
            Test(
                "C10,20,30,40,50,60,70,80,90,100,110,120",
                listOf(
                    curve(0..17, false, "10", "20", "30", "40", "50", "60"),
                    curve(19..38, false, "70", "80", "90", "100", "110", "120", true),
                )
            ),
            Test(
                "c10,20,30,40,50,60,70,80,90,100,110,120",
                listOf(
                    curve(0..17, true, "10", "20", "30", "40", "50", "60"),
                    curve(19..38, true, "70", "80", "90", "100", "110", "120", true),
                )
            ),
            Test(
                "A10,20,30,1,0,60,70,80,90,100,0,1,130,140",
                listOf(
                    arc(0..18, false, "10", "20", "30", true, false, "60", "70"),
                    arc(20..40, false, "80", "90", "100", false, true, "130", "140", true),
                )
            ),
            Test(
                "a10,20,30,0,1,60,70,80,90,100,1,0,130,140",
                listOf(
                    arc(0..18, true, "10", "20", "30", false, true, "60", "70"),
                    arc(20..40, true, "80", "90", "100", true, false, "130", "140", true),
                )
            ),
        )
        tests.forEach { test ->
            val parser = SvgPathParser(test.input)
            val error = "Input: ${test.input}"
            test.output.forEach { command ->
                assertEquals(command.right(), parser.read(), error)
            }
            assertEquals(SvgPathError.EOF.left(), parser.read(), error)
            assertEquals(SvgPathError.EOF.left(), parser.read(), error)
        }
    }

    @Test
    fun testSubsequentMissing() {
        class Test(
            val input: String,
            val output: List<Either<SvgPathError, SvgPathCommand>>,
        )

        val tests = listOf(
            Test(
                "Z10",
                listOf(
                    close(0..0, false).right(),
                    SvgPathError.MissingCommand.left(),
                )
            ),
            Test(
                "z10",
                listOf(
                    close(0..0, true).right(),
                    SvgPathError.MissingCommand.left(),
                )
            ),
            Test(
                "Z,",
                listOf(
                    close(0..0, false).right(),
                    SvgPathError.MissingCommand.left(),
                )
            ),
            Test(
                "z,",
                listOf(
                    close(0..0, true).right(),
                    SvgPathError.MissingCommand.left(),
                )
            ),
            Test(
                "H10,",
                listOf(
                    hLine(0..2, false, "10").right(),
                    SvgPathError.MissingArgs.left(),
                )
            ),
            Test(
                "h10,",
                listOf(
                    hLine(0..2, true, "10").right(),
                    SvgPathError.MissingArgs.left(),
                )
            ),
            Test(
                "V10,",
                listOf(
                    vLine(0..2, false, "10").right(),
                    SvgPathError.MissingArgs.left(),
                )
            ),
            Test(
                "v10,",
                listOf(
                    vLine(0..2, true, "10").right(),
                    SvgPathError.MissingArgs.left(),
                )
            ),
            Test(
                "M10,20,",
                listOf(
                    move(0..5, false, "10", "20").right(),
                    SvgPathError.MissingArgs.left(),
                )
            ),
            Test(
                "m10,20,",
                listOf(
                    move(0..5, true, "10", "20").right(),
                    SvgPathError.MissingArgs.left(),
                )
            ),
            Test(
                "L10,20,",
                listOf(
                    line(0..5, false, "10", "20").right(),
                    SvgPathError.MissingArgs.left(),
                )
            ),
            Test(
                "l10,20,",
                listOf(
                    line(0..5, true, "10", "20").right(),
                    SvgPathError.MissingArgs.left(),
                )
            ),
            Test(
                "T10,20,",
                listOf(
                    sqCurve(0..5, false, "10", "20").right(),
                    SvgPathError.MissingArgs.left(),
                )
            ),
            Test(
                "t10,20,",
                listOf(
                    sqCurve(0..5, true, "10", "20").right(),
                    SvgPathError.MissingArgs.left(),
                )
            ),
            Test(
                "S10,20,30,40,",
                listOf(
                    sCurve(0..11, false, "10", "20", "30", "40").right(),
                    SvgPathError.MissingArgs.left(),
                )
            ),
            Test(
                "s10,20,30,40,",
                listOf(
                    sCurve(0..11, true, "10", "20", "30", "40").right(),
                    SvgPathError.MissingArgs.left(),
                )
            ),
            Test(
                "Q10,20,30,40,",
                listOf(
                    qCurve(0..11, false, "10", "20", "30", "40").right(),
                    SvgPathError.MissingArgs.left(),
                )
            ),
            Test(
                "q10,20,30,40,",
                listOf(
                    qCurve(0..11, true, "10", "20", "30", "40").right(),
                    SvgPathError.MissingArgs.left(),
                )
            ),
            Test(
                "C10,20,30,40,50,60,",
                listOf(
                    curve(0..17, false, "10", "20", "30", "40", "50", "60").right(),
                    SvgPathError.MissingArgs.left(),
                )
            ),
            Test(
                "c10,20,30,40,50,60,",
                listOf(
                    curve(0..17, true, "10", "20", "30", "40", "50", "60").right(),
                    SvgPathError.MissingArgs.left(),
                )
            ),
            Test(
                "A10,20,30,1,0,60,70,",
                listOf(
                    arc(0..18, false, "10", "20", "30", true, false, "60", "70").right(),
                    SvgPathError.MissingArgs.left(),
                )
            ),
            Test(
                "a10,20,30,0,1,60,70,",
                listOf(
                    arc(0..18, true, "10", "20", "30", false, true, "60", "70").right(),
                    SvgPathError.MissingArgs.left(),
                )
            ),
        )

        tests.forEach { test ->
            val parser = SvgPathParser(test.input)
            val message = "Input: ${test.input}"
            test.output.forEach {
                assertEquals(it, parser.read(), message)
            }
            assertEquals(test.output.last(), parser.read(), message)
        }
    }

    // command constructors

    private fun close(
        sourcePos: IntRange,
        isRelative: Boolean,
    ) = SvgPathCommand.Close(SvgPathCommand.Info(sourcePos, isRelative, false))

    private fun move(
        sourcePos: IntRange,
        isRelative: Boolean,
        x: String,
        y: String,
        isSubsequent: Boolean = false,
    ) = SvgPathCommand.Move(
        SvgPathCommand.Info(sourcePos, isRelative, isSubsequent),
        BigDecimal(x), BigDecimal(y),
    )

    private fun line(
        sourcePos: IntRange,
        isRelative: Boolean,
        x: String,
        y: String,
        isSubsequent: Boolean = false,
    ) = SvgPathCommand.Line(
        SvgPathCommand.Info(sourcePos, isRelative, isSubsequent),
        BigDecimal(x), BigDecimal(y),
    )

    private fun hLine(
        sourcePos: IntRange,
        isRelative: Boolean,
        x: String,
        isSubsequent: Boolean = false,
    ) = SvgPathCommand.HorizontalLine(
        SvgPathCommand.Info(sourcePos, isRelative, isSubsequent),
        BigDecimal(x),
    )

    private fun vLine(
        sourcePos: IntRange,
        isRelative: Boolean,
        y: String,
        isSubsequent: Boolean = false,
    ) = SvgPathCommand.VerticalLine(
        SvgPathCommand.Info(sourcePos, isRelative, isSubsequent),
        BigDecimal(y),
    )

    private fun curve(
        sourcePos: IntRange,
        isRelative: Boolean,
        cx1: String,
        cy1: String,
        cx2: String,
        cy2: String,
        x: String,
        y: String,
        isSubsequent: Boolean = false,
    ) = SvgPathCommand.Curve(
        SvgPathCommand.Info(sourcePos, isRelative, isSubsequent),
        BigDecimal(cx1), BigDecimal(cy1),
        BigDecimal(cx2), BigDecimal(cy2),
        BigDecimal(x), BigDecimal(y),
    )

    private fun sCurve(
        sourcePos: IntRange,
        isRelative: Boolean,
        cx2: String,
        cy2: String,
        x: String,
        y: String,
        isSubsequent: Boolean = false,
    ) = SvgPathCommand.SmoothCurve(
        SvgPathCommand.Info(sourcePos, isRelative, isSubsequent),
        BigDecimal(cx2), BigDecimal(cy2),
        BigDecimal(x), BigDecimal(y),
    )

    private fun qCurve(
        sourcePos: IntRange,
        isRelative: Boolean,
        cx: String,
        cy: String,
        x: String,
        y: String,
        isSubsequent: Boolean = false,
    ) = SvgPathCommand.QuadraticCurve(
        SvgPathCommand.Info(sourcePos, isRelative, isSubsequent),
        BigDecimal(cx), BigDecimal(cy),
        BigDecimal(x), BigDecimal(y),
    )

    private fun sqCurve(
        sourcePos: IntRange,
        isRelative: Boolean,
        x: String,
        y: String,
        isSubsequent: Boolean = false,
    ) = SvgPathCommand.SmoothQuadraticCurve(
        SvgPathCommand.Info(sourcePos, isRelative, isSubsequent),
        BigDecimal(x), BigDecimal(y),
    )

    private fun arc(
        sourcePos: IntRange,
        isRelative: Boolean,
        rx: String,
        ry: String,
        xAxisRotation: String,
        largeArc: Boolean,
        sweep: Boolean,
        x: String,
        y: String,
        isSubsequent: Boolean = false,
    ) = SvgPathCommand.Arc(
        SvgPathCommand.Info(sourcePos, isRelative, isSubsequent),
        BigDecimal(rx), BigDecimal(ry),
        BigDecimal(xAxisRotation),
        largeArc, sweep,
        BigDecimal(x), BigDecimal(y),
    )
}
