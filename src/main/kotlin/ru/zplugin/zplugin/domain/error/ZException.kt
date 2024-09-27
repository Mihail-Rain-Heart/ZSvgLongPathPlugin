package ru.zplugin.zplugin.domain.error

internal sealed class ZException(
    override val message: String
) : Exception(message) {

    object UnsupportedTagOrData : ZException(message = MESSAGE_UNSUPPORTED_TAG_OR_DATA)

    object NotCreateTwoPiecesFromTag : ZException(message = MESSAGE_NOT_CREATE_TWO_PIECES_FROM_TAG)

    object NotFoundPathDataTag : ZException(message = MESSAGE_NOT_FOUND_PATH_DATA_TAG)


}

private const val MESSAGE_UNSUPPORTED_TAG_OR_DATA = "UNSUPPORTED_TAG_OR_DATA"
private const val MESSAGE_NOT_CREATE_TWO_PIECES_FROM_TAG = "NOT_CREATE_TWO_PIECES_FROM_TAG"
private const val MESSAGE_NOT_FOUND_PATH_DATA_TAG = "NOT_FOUND_PATH_DATA_TAG"