package ru.zplugin.zplugin.domain.parser

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.merge
import arrow.core.right
import java.math.BigDecimal

/**
 * Parses passed svg path data based on [SVG 1.1 spec](https://www.w3.org/TR/SVG11/paths.html) to find possible split points.
 *
 * Possible split points are after "Zz" or before "Mm" commands.
 */
class SvgPathSplitter(path: String) {
    private val parser: SvgPathParser = SvgPathParser(path)
    private var state: State = State.Init

    private data class PainterState(
        val command: SvgPathCommand,
        val startX: BigDecimal,
        val startY: BigDecimal,
        val currentX: BigDecimal,
        val currentY: BigDecimal,
    )

    private sealed interface State {
        object Init : State
        data class Parsing(val history: List<PainterState>) : State
        data class End(val reason: SvgPathError) : State
    }

    data class Split(
        val skipStart: Int,
        val skipEnd: Int,
        val newPrefix: String,
    )

    fun next(): Either<SvgPathError, Split> {
        while (true) {
            val result = state.let {
                when (it) {
                    State.Init -> processInit() to null
                    is State.Parsing -> processParsing(it)
                    is State.End -> it to null
                }
            }
            state = result.first
            result.second?.let { return it.right() }
            (result.first as? State.End)?.reason?.let { return it.left() }
        }
    }

    private fun processInit(): State {
        return parser.read()
            .flatMap { if (it !is SvgPathCommand.Move) SvgPathError.BadStart.left() else it.right() }
            .mapLeft { State.End(it) }
            .map { State.Parsing(listOf(PainterState(it, it.offsetX, it.offsetY, it.offsetX, it.offsetY))) }
            .merge()
    }

    private fun processParsing(state: State.Parsing): Pair<State, Split?> {
        val command = parser.read().getOrElse { return State.End(it) to trySplit(state.history, null) }
        val last = state.history.last()
        val curr = last.merge(command)

        val newState = state.copy(history = state.history.takeLast(2) + curr)
        return newState to trySplit(state.history, curr)
    }

    private fun PainterState.merge(command: SvgPathCommand): PainterState {
        if (command is SvgPathCommand.Close) {
            return copy(command = command, currentX = startX, currentY = startY)
        }

        val x: BigDecimal
        val y: BigDecimal
        if (command.isRelative) {
            x = (command as? SvgPathCommand.HorizontalOffset)?.offsetX?.let { it + currentX } ?: currentX
            y = (command as? SvgPathCommand.VerticalOffset)?.offsetY?.let { it + currentY } ?: currentY
        } else {
            x = (command as? SvgPathCommand.HorizontalOffset)?.offsetX ?: currentX
            y = (command as? SvgPathCommand.VerticalOffset)?.offsetY ?: currentY
        }
        return if (command is SvgPathCommand.Move) {
            copy(command = command, startX = x, startY = y, currentX = x, currentY = y)
        } else {
            copy(command = command, currentX = x, currentY = y)
        }
    }

    private fun trySplit(history: List<PainterState>, curr: PainterState?): Split? {
        if (history.size < 2) return null

        val last = history[history.size - 1]
        val secondLast = history[history.size - 2]

        if (last.command is SvgPathCommand.Move) {
            return when {
                !last.command.isRelative -> {
                    Split(secondLast.command.end, last.command.start, "")
                }
                curr?.command?.isSubsequent == true -> {
                    createPrefixedSplit(last.startX, last.startY, secondLast.command.end, curr.command.start, "l")
                }
                else -> {
                    createPrefixedSplit(last.startX, last.startY, secondLast.command.end, last.command.end, "")
                }
            }
        }

        if (secondLast.command is SvgPathCommand.Close) {
            return createPrefixedSplit(secondLast.startX, secondLast.startY, secondLast.command.end, last.command.start)
        }

        return null
    }

    private fun createPrefixedSplit(x: BigDecimal, y: BigDecimal, start: Int, end: Int, suffix: String = ""): Split {
        val prefix = if (y.signum() < 0) "M$x$y$suffix" else "M$x,$y$suffix"
        return Split(start, end, prefix)
    }
}
