package ru.zplugin.zplugin

import java.util.function.Consumer

interface ISvgServiceEditor {

    fun removeZ(
        text: String,
        progressConsumer: Consumer<Int>,
        hideDialog: Runnable,
        consumer: Consumer<String>
    )

    fun cancel()
}
