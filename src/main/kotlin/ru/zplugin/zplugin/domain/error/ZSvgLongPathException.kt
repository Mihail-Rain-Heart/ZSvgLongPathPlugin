package ru.zplugin.zplugin.domain.error

internal sealed class ZSvgLongPathException(
    override val message: String
) : Exception(message) {

    object UnsupportedTagOrData : ZSvgLongPathException(message = MESSAGE_UNSUPPORTED_TAG_OR_DATA)

    object NotCreateTwoPiecesFromTag : ZSvgLongPathException(message = MESSAGE_NOT_CREATE_TWO_PIECES_FROM_TAG)

    object NotFoundPathDataTag : ZSvgLongPathException(message = MESSAGE_NOT_FOUND_PATH_DATA_TAG)

    companion object {

        fun Throwable.getMessageText() = when(this) {
            is ZSvgLongPathException -> message
            else -> "Error: ${this::class.simpleName}"
        }
    }
}

private const val MESSAGE_UNSUPPORTED_TAG_OR_DATA = "UNSUPPORTED_TAG_OR_DATA"
private const val MESSAGE_NOT_CREATE_TWO_PIECES_FROM_TAG = "NOT_CREATE_TWO_PIECES_FROM_TAG"
private const val MESSAGE_NOT_FOUND_PATH_DATA_TAG = "NOT_FOUND_PATH_DATA_TAG"