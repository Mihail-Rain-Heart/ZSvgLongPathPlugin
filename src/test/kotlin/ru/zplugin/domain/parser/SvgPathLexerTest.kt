package ru.zplugin.domain.parser

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ru.zplugin.zplugin.domain.parser.SvgCommandType
import ru.zplugin.zplugin.domain.parser.SvgPathLexer
import ru.zplugin.zplugin.domain.parser.SvgPathLexer.NumberMode
import ru.zplugin.zplugin.domain.parser.SvgPathLexer.Token

class SvgPathLexerTest {
    @Test
    fun testEnd() {
        var lexer = SvgPathLexer("")
        assertEquals(Token.EOF(0, 0), lexer.read(NumberMode.GENERIC))
        assertEquals(Token.EOF(0, 0), lexer.read(NumberMode.NON_NEGATIVE))
        assertEquals(Token.EOF(0, 0), lexer.read(NumberMode.FLAG))

        lexer = SvgPathLexer("#")
        assertEquals(Token.Unknown(0, 1), lexer.read(NumberMode.GENERIC))
        assertEquals(Token.EOF(1, 1), lexer.read(NumberMode.GENERIC))
        assertEquals(Token.EOF(1, 1), lexer.read(NumberMode.NON_NEGATIVE))
        assertEquals(Token.EOF(1, 1), lexer.read(NumberMode.FLAG))
    }

    @Test
    fun testPeek() {
        NumberMode.values().forEach { mode1 ->
            NumberMode.values().forEach { mode2 ->
                NumberMode.values().forEach { mode3 ->
                    val lexer = SvgPathLexer(" ,")
                    assertEquals(Token.Whitespace(0, 1), lexer.peek(mode1))
                    assertEquals(Token.Whitespace(0, 1), lexer.peek(mode1))
                    assertEquals(Token.Whitespace(0, 1), lexer.read(mode2))
                    assertEquals(Token.Comma(1, 2), lexer.peek(mode3))
                    assertEquals(Token.Comma(1, 2), lexer.peek(mode3))
                }
            }
        }

        var lexer = SvgPathLexer("-1")
        assertEquals(Token.Number(0, 2, BigDecimal("-1")), lexer.peek(NumberMode.GENERIC))
        assertEquals(Token.Unknown(0, 1), lexer.peek(NumberMode.NON_NEGATIVE))
        assertEquals(Token.Unknown(0, 1), lexer.peek(NumberMode.FLAG))

        lexer = SvgPathLexer("1")
        assertEquals(Token.Flag(0, 1, true), lexer.peek(NumberMode.FLAG))
        assertEquals(Token.Number(0, 1, BigDecimal("1")), lexer.peek(NumberMode.NON_NEGATIVE))
        assertEquals(Token.Number(0, 1, BigDecimal("1")), lexer.peek(NumberMode.GENERIC))
    }

    @Test
    fun testInvalidNum() {
        val unexpectedEndTests = mutableListOf(
            "+",
            "-",
            ".",
            "+.",
            "-.",
        )
        unexpectedEndTests.forEach { input ->
            var lexer = SvgPathLexer(input)
            assertEquals(Token.Unknown(0, input.length), lexer.read(NumberMode.GENERIC))
            assertEquals(Token.EOF(input.length, input.length), lexer.read(NumberMode.GENERIC))

            if (input[0] != '-' && input[0] != '+') {
                lexer = SvgPathLexer(input)
                assertEquals(Token.Unknown(0, input.length), lexer.read(NumberMode.NON_NEGATIVE))
                assertEquals(Token.EOF(input.length, input.length), lexer.read(NumberMode.NON_NEGATIVE))
            }
        }

        val unknownTests = mutableListOf(
            'E' to NumberMode.GENERIC,
            'E' to NumberMode.NON_NEGATIVE,
            'E' to NumberMode.FLAG,
            '+' to NumberMode.NON_NEGATIVE,
            '+' to NumberMode.FLAG,
            '-' to NumberMode.NON_NEGATIVE,
            '-' to NumberMode.FLAG,
            '2' to NumberMode.FLAG,
        )
        unknownTests.forEach { (input, mode) ->
            val lexer = SvgPathLexer(input.toString())
            assertEquals(Token.Unknown(0, 1), lexer.read(mode))
            assertEquals(Token.EOF(1, 1), lexer.read(mode))
        }

        val numWithUnknownTest = mutableListOf(
            "1e" to listOf(Token.Number(0, 1, BigDecimal.ONE), Token.Unknown(1, 2)),
            "1E" to listOf(Token.Number(0, 1, BigDecimal.ONE), Token.Unknown(1, 2)),
            "1E+" to listOf(Token.Number(0, 1, BigDecimal.ONE), Token.Unknown(1, 2), Token.Unknown(2, 3)),
            "1E-" to listOf(Token.Number(0, 1, BigDecimal.ONE), Token.Unknown(1, 2), Token.Unknown(2, 3)),
        )
        numWithUnknownTest.forEach { (input, output) ->
            listOf(NumberMode.GENERIC, NumberMode.NON_NEGATIVE).forEach { mode ->
                val lexer = SvgPathLexer(input)
                output.forEach { token -> assertEquals(token, lexer.read(mode)) }
                assertEquals(Token.EOF(input.length, input.length), lexer.read(mode))
            }
        }

        val bigNum = "1E${Int.MAX_VALUE.toLong() + 1}"

        var lexer = SvgPathLexer(bigNum)
        assertEquals(Token.BadNumber(0, 12), lexer.read(NumberMode.GENERIC))
        assertEquals(Token.EOF(12, 12), lexer.read(NumberMode.GENERIC))

        lexer = SvgPathLexer(bigNum)
        assertEquals(Token.BadNumber(0, 12), lexer.read(NumberMode.NON_NEGATIVE))
        assertEquals(Token.EOF(12, 12), lexer.read(NumberMode.NON_NEGATIVE))
    }

    @Test
    fun testValidNum() {
        val singleTokenTests = mutableListOf(
            "2" to "2",
            "+2" to "2",
            "-2" to "-2",
            "2.3" to "2.3",
            "2." to "2.",
            ".3" to ".3",
            "+.3" to ".3",
            "-.3" to "-.3",
            "2e2" to "2e2",
            "2E2" to "2e2",
            "2E+2" to "2e2",
            "2E-2" to "2e-2",
        )
        singleTokenTests.forEach { (input, number) ->
            var lexer = SvgPathLexer(input)
            assertEquals(Token.Number(0, input.length, BigDecimal(number)), lexer.read(NumberMode.GENERIC))
            assertEquals(Token.EOF(input.length, input.length), lexer.read(NumberMode.GENERIC))

            if (input[0] != '-' && input[0] != '+') {
                lexer = SvgPathLexer(input)
                assertEquals(Token.Number(0, input.length, BigDecimal(number)), lexer.read(NumberMode.NON_NEGATIVE))
                assertEquals(Token.EOF(input.length, input.length), lexer.read(NumberMode.NON_NEGATIVE))
            }
        }

        var lexer = SvgPathLexer("1E-1.5")
        assertEquals(Token.Number(0, 4, BigDecimal("1e-1")), lexer.read(NumberMode.GENERIC))
        assertEquals(Token.Number(4, 6, BigDecimal(".5")), lexer.read(NumberMode.GENERIC))
        assertEquals(Token.EOF(6, 6), lexer.read(NumberMode.GENERIC))

        lexer = SvgPathLexer("10")
        assertEquals(Token.Flag(0, 1, true), lexer.read(NumberMode.FLAG))
        assertEquals(Token.Flag(1, 2, false), lexer.read(NumberMode.FLAG))
        assertEquals(Token.EOF(2, 2), lexer.read(NumberMode.FLAG))

        lexer = SvgPathLexer("25-13")
        assertEquals(Token.Number(0, 2, BigDecimal("25")), lexer.read(NumberMode.GENERIC))
        assertEquals(Token.Number(2, 5, BigDecimal("-13")), lexer.read(NumberMode.GENERIC))
        assertEquals(Token.EOF(5, 5), lexer.read(NumberMode.GENERIC))
    }

    @Test
    fun testWhitespace() {
        val tests = mutableListOf(
            " ",
            "\t",
            "\r",
            "\n",
            "\u000c",
            " \t\r\n\u000c\n\r\t ",
        )

        tests.forEach { input ->
            NumberMode.values().forEach { mode ->
                val lexer = SvgPathLexer(input)
                assertEquals(Token.Whitespace(0, input.length), lexer.read(mode))
                assertEquals(Token.EOF(input.length, input.length), lexer.read(mode))
            }
        }

        NumberMode.values().forEach { mode ->
            val wrongSpace = '\u00a0' // No-break space
            assertTrue(wrongSpace.isWhitespace(), "Wrong unicode whitespace in tests")
            assertEquals(Token.Unknown(0, 1), SvgPathLexer("$wrongSpace").read(mode))
        }

        NumberMode.values().forEach { mode ->
            val lexer = SvgPathLexer("  ,,  ")
            assertEquals(Token.Whitespace(0, 2), lexer.read(mode))
            assertEquals(Token.Comma(2, 3), lexer.read(mode))
            assertEquals(Token.Comma(3, 4), lexer.read(mode))
            assertEquals(Token.Whitespace(4, 6), lexer.read(mode))
            assertEquals(Token.EOF(6, 6), lexer.read(mode))
        }
    }

    @Test
    fun testCommand() {
        val singleTokenTests = mutableListOf(
            'M' to SvgCommandType.MOVE,
            'Z' to SvgCommandType.CLOSE,
            'L' to SvgCommandType.LINE,
            'H' to SvgCommandType.HORIZONTAL_LINE,
            'V' to SvgCommandType.VERTICAL_LINE,
            'C' to SvgCommandType.CURVE,
            'S' to SvgCommandType.SMOOTH_CURVE,
            'Q' to SvgCommandType.QUADRATIC_CURVE,
            'T' to SvgCommandType.SMOOTH_QUADRATIC_CURVE,
            'A' to SvgCommandType.ARC,
        )
        singleTokenTests.forEach { (input, type) ->
            NumberMode.values().forEach { mode ->
                assertEquals(Token.Command(0, 1, type, false), SvgPathLexer(input.uppercase()).read(mode))
                assertEquals(Token.Command(0, 1, type, true), SvgPathLexer(input.lowercase()).read(mode))
            }
        }

        NumberMode.values().forEach { mode ->
            val lexer = SvgPathLexer("Mm")
            assertEquals(Token.Command(0, 1, SvgCommandType.MOVE, false), lexer.read(mode))
            assertEquals(Token.Command(1, 2, SvgCommandType.MOVE, true), lexer.read(mode))
            assertEquals(Token.EOF(2, 2), lexer.read(mode))
        }
    }
}
