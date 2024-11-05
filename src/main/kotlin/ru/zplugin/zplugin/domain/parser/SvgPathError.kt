package ru.zplugin.zplugin.domain.parser

sealed interface SvgPathError {
    object EOF : SvgPathError
    object BadStart : SvgPathError
    object BadNumber : SvgPathError
    object MissingArgs : SvgPathError
    object MissingCommand : SvgPathError
}