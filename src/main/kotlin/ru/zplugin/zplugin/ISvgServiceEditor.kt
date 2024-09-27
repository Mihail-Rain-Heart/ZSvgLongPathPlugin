package ru.zplugin.zplugin

import com.intellij.openapi.project.Project
import java.util.function.Consumer

interface ISvgServiceEditor {

    fun removeZ(
        project: Project,
        text: String,
        progressConsumer: Consumer<Int>,
        hideDialog: Runnable,
        consumer: Consumer<String>
    )

    fun cancel()
}
