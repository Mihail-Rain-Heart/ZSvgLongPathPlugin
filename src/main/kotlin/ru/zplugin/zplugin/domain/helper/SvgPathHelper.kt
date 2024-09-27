package ru.zplugin.zplugin.domain.helper

import ru.zplugin.zplugin.domain.error.ZException
import java.util.function.Consumer
import kotlin.math.roundToInt

internal class SvgPathHelper {

    @Throws(ZException::class)
    fun getSplitByZText(text: String, progressConsumer: Consumer<Int>): String {
        if (isUnsupported(text = text)) {
            throw ZException.UnsupportedTagOrData
        }
        // cut PATH_DATA from tag
        val (startPieceOfTag, endPieceOfTag) = getTwoPiecesFromTag(text = text)
        // get content of PATH_DATA attribute
        val pathData = getPathData(text = text)
        return getSplitByZText(
            startPieceOfTag = startPieceOfTag,
            endPieceOfTag = endPieceOfTag,
            pathData = pathData,
            progressConsumer = progressConsumer
        )
    }

    @Throws(ZException.NotCreateTwoPiecesFromTag::class)
    private fun getTwoPiecesFromTag(text: String): Pair<String, String> {
        val pieces = text.split(SPLIT_REGEX)
        return if (pieces.size == 2) {
            pieces.first() to pieces.last()
        } else {
            throw ZException.NotCreateTwoPiecesFromTag
        }
    }

    @Throws(ZException.NotFoundPathDataTag::class)
    private fun getPathData(text: String): List<String> {
        return SPLIT_REGEX.find(text)
            ?.value
            ?.drop(PATH_DATA.length)
            ?.split(DELIMITER, ignoreCase = true)
            ?: throw ZException.NotFoundPathDataTag
    }

    private fun getSplitByZText(
        startPieceOfTag: String,
        endPieceOfTag: String,
        pathData: List<String>,
        progressConsumer: Consumer<Int>
    ): String {
        val pathDataSize = pathData.size
            .toFloat()
            .coerceAtLeast(minimumValue = 1f)
        val builder = SplitZTagBuilder(
            startPieceOfTag = startPieceOfTag,
            endPieceOfTag = endPieceOfTag
        )
        return pathData.foldIndexed(initial = "") { index, acc, path ->
            progressConsumer.accept(
                ((index / pathDataSize) * MAX_PERCENT)
                    .roundToInt()
                    .coerceAtMost(MAX_PERCENT)
            )
            if (path.isNotBlank() && path != "\"") {
                acc + builder.build(path = path)
            } else {
                acc
            }
        }
    }

    private fun isUnsupported(text: String) = !TAG_REGEX.containsMatchIn(text)

    private class SplitZTagBuilder(
        private val startPieceOfTag: String,
        private val endPieceOfTag: String
    ) {

        private fun getAttributeName(path: String): String {
            return "$PATH_DATA${"\"".takeIf { path.first() != '\"' }.orEmpty()}"
        }

        fun build(path: String): String {
            return startPieceOfTag +
                    getAttributeName(path = path) +
                    path +
                    "$DELIMITER\"" +
                    endPieceOfTag +
                    "\n"
        }
    }
}

private const val MAX_PERCENT = 100

private const val DELIMITER = "z"

private const val PATH_DATA = "android:pathData="

private val TAG_REGEX = "<path\\s+[^>]*\\b$PATH_DATA\"[^\"]*\"\\s*[^>]*/>".toRegex()
private val SPLIT_REGEX = "$PATH_DATA(.+\")?".toRegex()
