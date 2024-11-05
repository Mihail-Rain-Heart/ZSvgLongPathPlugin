package ru.zplugin.zplugin.domain.parser

import java.math.BigDecimal

sealed interface SvgPathCommand {
    val info: Info
    val sourcePos: IntRange get() = info.sourcePos
    val start: Int get() = info.sourcePos.first
    val end: Int get() = info.sourcePos.last + 1
    val isSubsequent: Boolean get() = info.isSubsequent
    val isRelative: Boolean get() = info.isRelative
    val type: SvgCommandType

    data class Info(
        val sourcePos: IntRange,
        val isRelative: Boolean,
        val isSubsequent: Boolean,
    )

    sealed interface HorizontalOffset : SvgPathCommand {
        val offsetX: BigDecimal
    }

    sealed interface VerticalOffset : SvgPathCommand {
        val offsetY: BigDecimal
    }

    sealed interface PointOffset : HorizontalOffset, VerticalOffset

    data class Close(
        override val info: Info,
    ) : SvgPathCommand {
        override val type = SvgCommandType.CLOSE

        constructor(
            sourcePos: IntRange,
            isRelative: Boolean,
            isSubsequent: Boolean
        ) : this(Info(sourcePos, isRelative, isSubsequent))
    }

    data class HorizontalLine(
        override val info: Info,
        override val offsetX: BigDecimal,
    ) : HorizontalOffset {
        override val type = SvgCommandType.HORIZONTAL_LINE

        constructor(
            sourcePos: IntRange,
            isRelative: Boolean,
            isSubsequent: Boolean,
            offsetX: BigDecimal
        ) : this(Info(sourcePos, isRelative, isSubsequent), offsetX)
    }

    data class VerticalLine(
        override val info: Info,
        override val offsetY: BigDecimal,
    ) : VerticalOffset {
        override val type = SvgCommandType.VERTICAL_LINE

        constructor(
            sourcePos: IntRange,
            isRelative: Boolean,
            isSubsequent: Boolean,
            offsetY: BigDecimal
        ) : this(Info(sourcePos, isRelative, isSubsequent), offsetY)
    }

    data class Move(
        override val info: Info,
        override val offsetX: BigDecimal,
        override val offsetY: BigDecimal,
    ) : PointOffset {
        override val type = SvgCommandType.MOVE

        constructor(
            sourcePos: IntRange,
            isRelative: Boolean,
            isSubsequent: Boolean,
            offsetX: BigDecimal,
            offsetY: BigDecimal
        ) : this(Info(sourcePos, isRelative, isSubsequent), offsetX, offsetY)
    }

    data class Line(
        override val info: Info,
        override val offsetX: BigDecimal,
        override val offsetY: BigDecimal,
    ) : PointOffset {
        override val type = SvgCommandType.LINE

        constructor(
            sourcePos: IntRange,
            isRelative: Boolean,
            isSubsequent: Boolean,
            offsetX: BigDecimal,
            offsetY: BigDecimal
        ) : this(Info(sourcePos, isRelative, isSubsequent), offsetX, offsetY)
    }

    data class SmoothQuadraticCurve(
        override val info: Info,
        override val offsetX: BigDecimal,
        override val offsetY: BigDecimal,
    ) : PointOffset {
        override val type = SvgCommandType.SMOOTH_QUADRATIC_CURVE

        constructor(
            sourcePos: IntRange,
            isRelative: Boolean,
            isSubsequent: Boolean,
            offsetX: BigDecimal,
            offsetY: BigDecimal,
        ) : this(Info(sourcePos, isRelative, isSubsequent), offsetX, offsetY)
    }

    data class QuadraticCurve(
        override val info: Info,
        val controlX: BigDecimal,
        val controlY: BigDecimal,
        override val offsetX: BigDecimal,
        override val offsetY: BigDecimal,
    ) : PointOffset {
        override val type = SvgCommandType.QUADRATIC_CURVE

        constructor(
            sourcePos: IntRange,
            isRelative: Boolean,
            isSubsequent: Boolean,
            controlX: BigDecimal,
            controlY: BigDecimal,
            offsetX: BigDecimal,
            offsetY: BigDecimal,
        ) : this(Info(sourcePos, isRelative, isSubsequent), controlX, controlY, offsetX, offsetY)
    }

    data class SmoothCurve(
        override val info: Info,
        val endControlX: BigDecimal,
        val endControlY: BigDecimal,
        override val offsetX: BigDecimal,
        override val offsetY: BigDecimal,
    ) : PointOffset {
        override val type = SvgCommandType.SMOOTH_CURVE

        constructor(
            sourcePos: IntRange,
            isRelative: Boolean,
            isSubsequent: Boolean,
            endControlX: BigDecimal,
            endControlY: BigDecimal,
            offsetX: BigDecimal,
            offsetY: BigDecimal
        ) : this(Info(sourcePos, isRelative, isSubsequent), endControlX, endControlY, offsetX, offsetY)
    }

    data class Curve(
        override val info: Info,
        val startControlX: BigDecimal,
        val startControlY: BigDecimal,
        val endControlX: BigDecimal,
        val endControlY: BigDecimal,
        override val offsetX: BigDecimal,
        override val offsetY: BigDecimal,
    ) : PointOffset {
        override val type = SvgCommandType.CURVE

        constructor(
            sourcePos: IntRange,
            isRelative: Boolean,
            isSubsequent: Boolean,
            startControlX: BigDecimal,
            startControlY: BigDecimal,
            endControlX: BigDecimal,
            endControlY: BigDecimal,
            offsetX: BigDecimal,
            offsetY: BigDecimal
        ) : this(
            Info(sourcePos, isRelative, isSubsequent),
            startControlX,
            startControlY,
            endControlX,
            endControlY,
            offsetX,
            offsetY
        )
    }

    data class Arc(
        override val info: Info,
        val radiusX: BigDecimal,
        val radiusY: BigDecimal,
        val xAxisRotation: BigDecimal,
        val isLargeArc: Boolean,
        val isSweep: Boolean,
        override val offsetX: BigDecimal,
        override val offsetY: BigDecimal,
    ) : PointOffset {
        override val type = SvgCommandType.ARC

        constructor(
            sourcePos: IntRange,
            isRelative: Boolean,
            isSubsequent: Boolean,
            radiusX: BigDecimal,
            radiusY: BigDecimal,
            xAxisRotation: BigDecimal,
            isLargeArc: Boolean,
            isSweep: Boolean,
            offsetX: BigDecimal,
            offsetY: BigDecimal,
        ) : this(
            Info(sourcePos, isRelative, isSubsequent),
            radiusX,
            radiusY,
            xAxisRotation,
            isLargeArc,
            isSweep,
            offsetX,
            offsetY
        )
    }
}