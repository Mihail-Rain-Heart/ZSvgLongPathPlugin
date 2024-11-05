package ru.zplugin.zplugin.domain.parser

import java.math.BigDecimal

/**
 * Tokenizes passed svg path data based (mostly) on [SVG 1.1 spec](https://www.w3.org/TR/SVG11/paths.html).
 */
class SvgPathLexer(private val path: String) {
    private val caches = Array<Token?>(NumberMode.values().size) { null }

    var pos: Int = 0
        private set

    enum class NumberMode {
        GENERIC,
        NON_NEGATIVE,
        FLAG,
    }

    sealed interface Token {
        val start: Int
        val end: Int

        data class EOF(override val start: Int, override val end: Int) : Token
        data class Unknown(override val start: Int, override val end: Int) : Token
        data class Comma(override val start: Int, override val end: Int) : Token
        data class Whitespace(override val start: Int, override val end: Int) : Token
        data class Flag(override val start: Int, override val end: Int, val value: Boolean) : Token
        data class Number(override val start: Int, override val end: Int, val value: BigDecimal) : Token
        data class BadNumber(override val start: Int, override val end: Int) : Token
        data class Command(
            override val start: Int,
            override val end: Int,
            val type: SvgCommandType,
            val isRelative: Boolean,
        ) : Token
    }

    fun peek(mode: NumberMode): Token {
        val start = pos
        val token = next(mode)
        caches[mode.ordinal] = token
        pos = start
        return token
    }

    fun read(mode: NumberMode): Token {
        val token = next(mode)
        caches.fill(null)
        return token
    }

    private fun next(mode: NumberMode): Token {
        if (pos == path.length) return Token.EOF(pos, pos)
        caches[mode.ordinal]?.let {
            pos = it.end
            return it
        }
        val token = when {
            path[pos].isAsciiWhitespace() -> readWhitespace()
            path[pos].isAsciiComma() -> readComma()
            mode == NumberMode.GENERIC && (path[pos].isAsciiDigit() || path[pos].isAsciiPoint() || path[pos].isAsciiSign()) -> readNumber()
            mode == NumberMode.NON_NEGATIVE && (path[pos].isAsciiDigit() || path[pos].isAsciiPoint()) -> readNumber()
            mode == NumberMode.FLAG && path[pos] in "01" -> readFlag()
            else -> readCommand()
        }
        return token
    }

    private fun readComma(): Token {
        check(path[pos].isAsciiComma()) { "Unexpected char instead of a comma." }
        return Token.Comma(pos, ++pos)
    }

    private fun readFlag(): Token {
        check(path[pos] in "01") { "Unexpected char instead of a flag." }
        return Token.Flag(pos, pos + 1, path[pos++] == '1')
    }

    private fun readWhitespace(): Token {
        check(path[pos].isAsciiWhitespace()) { "Unexpected char instead of a whitespace." }
        val start = pos
        while (pos < path.length && path[pos].isAsciiWhitespace()) pos++
        return Token.Whitespace(start, pos)
    }

    private fun readCommand(): Token {
        val isRelative = path[pos].isLowerCase()
        val type = when (path[pos].uppercaseChar()) {
            'M' -> SvgCommandType.MOVE
            'Z' -> SvgCommandType.CLOSE
            'L' -> SvgCommandType.LINE
            'H' -> SvgCommandType.HORIZONTAL_LINE
            'V' -> SvgCommandType.VERTICAL_LINE
            'C' -> SvgCommandType.CURVE
            'S' -> SvgCommandType.SMOOTH_CURVE
            'Q' -> SvgCommandType.QUADRATIC_CURVE
            'T' -> SvgCommandType.SMOOTH_QUADRATIC_CURVE
            'A' -> SvgCommandType.ARC
            else -> null
        }
        return when (type) {
            null -> Token.Unknown(pos, ++pos)
            else -> Token.Command(pos, ++pos, type, isRelative)
        }
    }

    private fun readNumber(): Token {
        val start = pos
        val hasSign = path[pos].isAsciiSign()
        if (hasSign) pos++

        val hasInteger = pos < path.length && path[pos].isAsciiDigit()
        while (pos < path.length && path[pos].isAsciiDigit()) pos++

        val hasPoint = pos < path.length && path[pos].isAsciiPoint()
        if (hasPoint) pos++

        val hasFraction = pos < path.length && path[pos].isAsciiDigit()
        while (pos < path.length && path[pos].isAsciiDigit()) pos++

        if (!hasInteger && (!hasPoint || !hasFraction)) {
            return Token.Unknown(start, pos)
        }

        if (pos < path.length && path[pos].uppercaseChar() == 'E') {
            val checkpoint = pos++

            if (pos < path.length && path[pos].isAsciiSign()) pos++

            val hasExponent = pos < path.length && path[pos].isAsciiDigit()
            while (pos < path.length && path[pos].isAsciiDigit()) pos++

            if (!hasExponent) pos = checkpoint
        }

        return try {
            Token.Number(start, pos, BigDecimal(path.substring(start, pos)))
        } catch (e: NumberFormatException) {
            Token.BadNumber(start, pos)
        }
    }

    private fun Char.isAsciiSign() = this == '+' || this == '-'
    private fun Char.isAsciiDigit() = this in '0'..'9'
    private fun Char.isAsciiPoint() = this == '.'
    private fun Char.isAsciiComma() = this == ','
    private fun Char.isAsciiWhitespace() = this in " \t\u000c\r\n" // \u000c is only present in SVG 2.0
}