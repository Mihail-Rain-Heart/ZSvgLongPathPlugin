package ru.zplugin.zplugin.domain

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.diagnostic.Logger

interface ISvgLongPathServiceEditor {

    fun splitSvgLongPathByZ(editor: Editor, logger: Logger)

    fun cancel()
}
