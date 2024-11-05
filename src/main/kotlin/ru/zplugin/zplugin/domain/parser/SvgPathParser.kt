package ru.zplugin.zplugin.domain.parser

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.merge
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import java.math.BigDecimal
import ru.zplugin.zplugin.domain.parser.SvgPathLexer.NumberMode
import ru.zplugin.zplugin.domain.parser.SvgPathLexer.Token

/**
 * Parses passed svg path data based on [SVG 1.1 spec](https://www.w3.org/TR/SVG11/paths.html) to commands
 * with its start and base positions in source string.
 */
class SvgPathParser(path: String) {
    private val lexer: SvgPathLexer = SvgPathLexer(path)
    private var state: State = State.Parsing()

    private sealed interface State {
        data class Error(val error: SvgPathError) : State
        data class Parsing(val lastCommand: SvgPathCommand? = null) : State
    }

    fun read(): Either<SvgPathError, SvgPathCommand> {
        state = state.let {
            when (it) {
                is State.Error -> it
                is State.Parsing -> processParsing(it)
            }
        }
        return state.let {
            when (it) {
                is State.Error -> it.error.left()
                is State.Parsing -> it.lastCommand!!.right()
            }
        }
    }

    private fun processParsing(state: State.Parsing): State {
        val command = parseCommand(state.lastCommand)
        return command
            .mapLeft { State.Error(it) }
            .map { State.Parsing(it) }
            .merge()
    }

    private fun parseCommand(lastCommand: SvgPathCommand?): Either<SvgPathError, SvgPathCommand> {
        var token = skipSpace(allowComma = false)
        if (token is Token.EOF) {
            return SvgPathError.EOF.left()
        }
        if (token !is Token.Command && (lastCommand == null || lastCommand.type == SvgCommandType.CLOSE)) {
            return SvgPathError.MissingCommand.left()
        }
        if (token is Token.Command && token.type == SvgCommandType.CLOSE) {
            lexer.read(NumberMode.GENERIC)
            val info = SvgPathCommand.Info(token.start until token.end, token.isRelative, false)
            return SvgPathCommand.Close(info).right()
        }

        val info: SvgPathCommand.Info
        val type: SvgCommandType
        if (token is Token.Command) {
            type = token.type
            info = SvgPathCommand.Info(token.start..token.start, token.isRelative, false)
            lexer.read(NumberMode.GENERIC)

            if (skipSpace(allowComma = false) is Token.Comma) {
                return SvgPathError.MissingArgs.left()
            }
        } else {
            check(lastCommand != null) { "Trying to reuse unknown command" }
            token = skipSpace()
            type = if (lastCommand.type == SvgCommandType.MOVE) SvgCommandType.LINE else lastCommand.type
            info = SvgPathCommand.Info(token.start..token.start, lastCommand.isRelative, true)
        }

        val command: Either<SvgPathError, SvgPathCommand> = when (type) {
            SvgCommandType.CLOSE -> {
                error("Trying to parse Close command args")
            }

            SvgCommandType.HORIZONTAL_LINE -> {
                readNumbers(1).map { SvgPathCommand.HorizontalLine(info.until(lexer.pos), it[0]) }
            }

            SvgCommandType.VERTICAL_LINE -> {
                readNumbers(1).map { SvgPathCommand.VerticalLine(info.until(lexer.pos), it[0]) }
            }

            SvgCommandType.LINE -> {
                readNumbers(2).map { SvgPathCommand.Line(info.until(lexer.pos), it[0], it[1]) }
            }

            SvgCommandType.MOVE -> {
                readNumbers(2).map { SvgPathCommand.Move(info.until(lexer.pos), it[0], it[1]) }
            }

            SvgCommandType.SMOOTH_QUADRATIC_CURVE -> {
                readNumbers(2).map { SvgPathCommand.SmoothQuadraticCurve(info.until(lexer.pos), it[0], it[1]) }
            }

            SvgCommandType.SMOOTH_CURVE -> {
                readNumbers(4).map {
                    SvgPathCommand.SmoothCurve(
                        info = info.until(lexer.pos),
                        endControlX = it[0], endControlY = it[1],
                        offsetX = it[2], offsetY = it[3],
                    )
                }
            }

            SvgCommandType.QUADRATIC_CURVE -> {
                readNumbers(4).map {
                    SvgPathCommand.QuadraticCurve(
                        info = info.until(lexer.pos),
                        controlX = it[0], controlY = it[1],
                        offsetX = it[2], offsetY = it[3],
                    )
                }
            }

            SvgCommandType.CURVE -> {
                readNumbers(6).map {
                    SvgPathCommand.Curve(
                        info = info.until(lexer.pos),
                        startControlX = it[0], startControlY = it[1],
                        endControlX = it[2], endControlY = it[3],
                        offsetX = it[4], offsetY = it[5],
                    )
                }
            }

            SvgCommandType.ARC -> {
                readNumbers(2, NumberMode.NON_NEGATIVE)
                    .flatMap { acc -> readNumbers(1).map { acc + it } }
                    .flatMap { acc -> readFlags().map { acc to it } }
                    .flatMap { acc ->
                        readNumbers(2).map {
                            SvgPathCommand.Arc(
                                info = info.until(lexer.pos),
                                radiusX = acc.first[0],
                                radiusY = acc.first[1],
                                xAxisRotation = acc.first[2],
                                isLargeArc = acc.second[0],
                                isSweep = acc.second[1],
                                offsetX = it[0], offsetY = it[1],
                            )
                        }
                    }
            }
        }

        return command
    }

    private fun readNumbers(
        count: Int,
        mode: NumberMode = NumberMode.GENERIC,
    ): Either<SvgPathError, List<BigDecimal>> = either {
        require(count > 0) { "Only positive count allowed." }
        val list = mutableListOf<BigDecimal>()
        for (i in 1..count) {
            val token = skipSpace(mode)
            ensure(token !is Token.BadNumber) { SvgPathError.BadNumber }
            ensure(token is Token.Number) { SvgPathError.MissingArgs }
            lexer.read(mode)
            list += token.value
        }
        return@either list
    }

    private fun readFlags(): Either<SvgPathError, List<Boolean>> = either {
        val list = mutableListOf<Boolean>()
        repeat(2) {
            val token = skipSpace(NumberMode.FLAG)
            ensure(token is Token.Flag) { SvgPathError.MissingArgs }
            lexer.read(NumberMode.FLAG)
            list += token.value
        }
        return@either list
    }

    private fun skipSpace(
        mode: NumberMode = NumberMode.GENERIC,
        allowComma: Boolean = true
    ): Token {
        if (lexer.peek(mode) is Token.Whitespace) lexer.read(mode)
        if (lexer.peek(mode) is Token.Comma && allowComma) lexer.read(mode)
        if (lexer.peek(mode) is Token.Whitespace) lexer.read(mode)
        return lexer.peek(mode)
    }
}

private fun SvgPathCommand.Info.until(last: Int) = this.copy(sourcePos = sourcePos.first until last)
