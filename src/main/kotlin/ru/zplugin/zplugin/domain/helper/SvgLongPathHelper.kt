package ru.zplugin.zplugin.domain.helper

import ru.zplugin.zplugin.domain.error.ZSvgLongPathException

internal class SvgLongPathHelper {

    @Throws(ZSvgLongPathException::class)
    suspend fun getSplitByZText(
        text: String,
        progressConsumer: suspend (Double) -> Unit
    ): String {
        if (isUnsupported(text = text)) {
            throw ZSvgLongPathException.UnsupportedTagOrData
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

    @Throws(ZSvgLongPathException.NotCreateTwoPiecesFromTag::class)
    private fun getTwoPiecesFromTag(text: String): Pair<String, String> {
        val pieces = text.split(SPLIT_REGEX)
        return if (pieces.size == 2) {
            pieces.first() to pieces.last()
        } else {
            throw ZSvgLongPathException.NotCreateTwoPiecesFromTag
        }
    }

    @Throws(ZSvgLongPathException.NotFoundPathDataTag::class)
    private fun getPathData(text: String): List<String> {
        return SPLIT_REGEX.find(text)
            ?.value
            ?.drop(PATH_DATA.length)
            ?.split(DELIMITER, ignoreCase = true)
            ?: throw ZSvgLongPathException.NotFoundPathDataTag
    }

    private suspend fun getSplitByZText(
        startPieceOfTag: String,
        endPieceOfTag: String,
        pathData: List<String>,
        progressConsumer: suspend (Double) -> Unit
    ): String {
        val pathDataSize = pathData.size
            .toFloat()
            .coerceAtLeast(minimumValue = MAX_PERCENT_F)
        val builder = SplitZTagBuilder(
            startPieceOfTag = startPieceOfTag,
            endPieceOfTag = endPieceOfTag
        )

        val stringBuilder = StringBuilder()

        pathData.forEachIndexed { index, path ->
            if (index % MAX_PERCENT == 0 || index == pathData.size - 1) {
                progressConsumer(
                    (index / pathDataSize)
                        .coerceAtMost(maximumValue = MAX_PERCENT_F)
                        .toDouble()
                )
            }

            if (path.isNotBlank() && path != "\"") {
                stringBuilder.append(builder.build(path))
            }
        }
        return stringBuilder.toString()
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
            return StringBuilder().apply {
                append(startPieceOfTag)
                append(getAttributeName(path = path))
                append(path)
                append("$DELIMITER\"")
                append(endPieceOfTag)
                append("\n")
            }.toString()
        }
    }
}

private const val MAX_PERCENT = 100
private const val MAX_PERCENT_F = 1f

private const val DELIMITER = "z"

private const val PATH_DATA = "android:pathData="

private val TAG_REGEX = "<path\\s+[^>]*\\b$PATH_DATA\"[^\"]*\"\\s*[^>]*/>".toRegex()
private val SPLIT_REGEX = "$PATH_DATA(.+\")?".toRegex()
