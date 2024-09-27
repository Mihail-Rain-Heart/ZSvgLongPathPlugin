package ru.zplugin.zplugin

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.registerOrReplaceServiceInstance
import com.intellij.util.ui.JBDimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JProgressBar

private val Log = logger<ZAction>()

class ZAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        // Set visibility only in the case of
        // existing project editor, and selection
        e.presentation.isEnabledAndVisible = project != null && editor != null && editor.selectionModel.hasSelection()
        Log.info("call update")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: run {
            Log.info("project is null")
            return
        }
        val editor = e.getData(CommonDataKeys.EDITOR) ?: run {
            Log.info("editor is null")
            return
        }

        val (builder, progress) = getDialogProgressBuilder()

        val zService = getSvgService(project = project)

        val document = editor.document
        val primaryCaret = editor.caretModel.primaryCaret
        val start = primaryCaret.selectionStart
        val end = primaryCaret.selectionEnd

        val text = document.getText(TextRange(start, end))

        zService.removeZ(
            project = project,
            text = text,
            progressConsumer = { currentProgress ->
               progress.value = currentProgress
            },
            hideDialog = {
                builder.dialogWrapper.doCancelAction()
            }
        ) { handledText ->
            WriteCommandAction.runWriteCommandAction(project) {
                document.replaceString(start, end, handledText)
            }
        }

        builder.addDisposable {
            zService.cancel()
        }
        Log.info("call actionPerformed")
        builder.show()
    }

    private fun getSvgService(project: Project): ISvgServiceEditor {
        return project.serviceOrNull<ISvgServiceEditor>() ?: run {
            Log.info("create new service!!!")
            SvgServiceEditor.getInstance(project).also { zService ->
                project.registerOrReplaceServiceInstance(ISvgServiceEditor::class.java, zService) {
                    Log.info("DISPOSE SERVICE")
                }
            }
        }
    }

    private fun getDialogProgressBuilder(): Pair<DialogBuilder, JProgressBar> {
        val builder = DialogBuilder()
        builder.addCancelAction()
        builder.title(buildString { append("Make more path`s by z split") })
        val dialogPanel = JPanel()
        dialogPanel.layout = BoxLayout(dialogPanel, BoxLayout.Y_AXIS)
        val progress = JProgressBar(0, 0, 100)
        dialogPanel.add(Box.createRigidArea(JBDimension(40, 30)))
        dialogPanel.add(progress)
        dialogPanel.add(Box.createRigidArea(JBDimension(40, 30)))
        builder.setCenterPanel(dialogPanel)
        return builder to progress
    }
}


//<path
//android:fillColor="#1a1a1a"
//android:fillType="evenOdd"
//android:pathData="M7.116,37.899c-0.066,-0.115 0.043,-0.547 0.243,-0.958 0.2,-0.412 0.424,-1.37 0.498,-2.13l0.135,-1.382 -0.511,0.551c-0.281,0.303 -0.511,0.678 -0.511,0.834 0,0.156 -0.147,0.222 -0.326,0.148 -0.18,-0.074 -0.446,-0.006 -0.592,0.152 -0.146,0.158 -0.267,0.177 -0.269,0.044 -0.002,-0.134 -0.089,-0.097 -0.194,0.081 -0.231,0.393 -1.696,0.883 -1.696,0.567 0,-0.123 0.199,-0.374 0.442,-0.557 0.243,-0.184 0.464,-0.449 0.49,-0.59 0.258,-1.356 0.657,-2.514 0.901,-2.615 0.164,-0.068 0.301,-0.255 0.304,-0.416 0.003,-0.161 0.214,-0.58 0.468,-0.931 0.529,-0.73 0.653,-1.493 0.178,-1.09 -0.163,0.138 -0.634,0.32 -1.046,0.404 -0.714,0.146 -0.737,0.126 -0.473,-0.411 0.152,-0.31 0.627,-1.109 1.055,-1.774 0.535,-0.833 0.653,-1.158 0.378,-1.044 -0.352,0.146 -0.38,0.032 -0.238,-0.929 0.136,-0.921 0.105,-1.071 -0.2,-0.945 -0.467,0.194 -0.461,0.143 0.156,-1.227 0.285,-0.633 0.455,-1.22 0.377,-1.304 -0.078,-0.084 -0.465,0.029 -0.862,0.25 -0.396,0.221 -0.791,0.402 -0.878,0.402 -0.2,0 0.507,-1.209 1.195,-2.045 0.29,-0.351 0.593,-0.842 0.673,-1.089 0.124,-0.379 0.045,-0.426 -0.498,-0.296 -0.614,0.147 -0.606,0.128 0.152,-0.394 0.692,-0.477 0.778,-0.649 0.655,-1.314 -0.1,-0.538 -0.062,-0.712 0.128,-0.585 0.175,0.117 0.204,0.066 0.084,-0.145 -0.497,-0.868 -0.557,-2.395 -0.232,-5.888 0.189,-2.038 0.345,-4.167 0.345,-4.731 0,-0.748 0.129,-1.111 0.474,-1.344 0.26,-0.176 0.474,-0.268 0.474,-0.206 0,0.502 0.775,3.908 1.052,4.624 0.191,0.495 0.362,0.984 0.379,1.087 0.089,0.546 0.534,0.803 0.962,0.556 0.353,-0.204 0.449,-0.173 0.449,0.148 0,0.224 -0.085,0.351 -0.189,0.281s-0.344,-0.02 -0.533,0.109c-0.257,0.175 -0.265,0.236 -0.034,0.239 0.17,0.002 0.254,0.102 0.185,0.222 -0.069,0.12 0.032,0.436 0.223,0.703 0.266,0.37 0.316,0.385 0.213,0.064 -0.074,-0.232 -0.021,-0.497 0.118,-0.59 0.139,-0.093 0.255,-0.018 0.257,0.166 0.003,0.247 0.061,0.236 0.224,-0.041 0.121,-0.207 0.275,-0.317 0.342,-0.244 0.194,0.209 -0.129,1.268 -0.343,1.124 -0.106,-0.071 -0.261,-0.011 -0.343,0.132 -0.082,0.144 0.017,0.331 0.221,0.415 0.204,0.084 0.37,0.315 0.37,0.513 0,0.243 0.231,0.341 0.71,0.302 0.391,-0.032 0.71,-0.151 0.71,-0.263 0,-0.113 0.346,-0.442 0.77,-0.732 0.641,-0.439 0.671,-0.496 0.179,-0.343 -0.531,0.166 -0.557,0.142 -0.262,-0.242 0.375,-0.487 0.341,-0.552 -0.459,-0.897 -0.581,-0.251 -0.581,-0.251 0.127,-0.285l0.71,-0.034 -0.592,-0.288c-0.326,-0.159 -0.642,-0.33 -0.704,-0.38 -0.248,-0.203 0.635,-0.567 1.384,-0.571 0.701,-0.003 0.761,-0.053 0.488,-0.408 -0.171,-0.222 -1.01,-0.803 -1.864,-1.29 -0.854,-0.487 -1.287,-0.813 -0.961,-0.725 0.326,0.088 0.799,0.189 1.052,0.224 0.253,0.035 0.502,0.198 0.553,0.362 0.051,0.165 0.34,0.299 0.643,0.299 0.809,0 0.568,-0.604 -1.175,-2.96 -0.824,-1.113 -1.455,-2.102 -1.401,-2.197 0.054,-0.095 1.189,1.014 2.522,2.463 2.583,2.809 3.236,3.2 5.356,3.203 0.643,0.001 1.363,0.213 1.894,0.557l0.857,0.556 -0.592,-0.725c-0.335,-0.41 -0.438,-0.662 -0.237,-0.58 0.195,0.079 0.568,0.234 0.829,0.344 0.421,0.178 0.438,0.152 0.149,-0.231 -0.229,-0.304 -0.243,-0.431 -0.045,-0.431 0.154,0 0.203,-0.134 0.109,-0.299 -0.115,-0.201 -0.042,-0.246 0.22,-0.137 0.233,0.096 0.368,0.044 0.335,-0.13 -0.031,-0.161 0.242,-0.411 0.607,-0.556 0.78,-0.31 1.319,-0.037 1.758,0.891 0.241,0.51 0.231,0.628 -0.059,0.697 -0.192,0.046 -0.349,0.18 -0.349,0.299 0,0.119 -0.065,0.398 -0.144,0.62 -0.09,0.253 -0.017,0.404 0.195,0.404 0.323,0 1.108,0.786 1.466,1.47 0.228,0.435 -0.677,0.423 -1.087,-0.014 -0.457,-0.487 -1.105,-0.703 -1.266,-0.422 -0.077,0.134 0.071,0.244 0.329,0.244 0.532,0 2.69,2.512 2.477,2.884 -0.077,0.135 -0.212,0.105 -0.312,-0.07 -0.113,-0.198 -0.271,-0.014 -0.441,0.513 -0.146,0.451 -0.211,0.915 -0.145,1.031 0.153,0.267 1.091,0.222 1.451,-0.071 0.385,-0.312 0.518,0.043 0.38,1.016 -0.099,0.701 -0.217,0.831 -0.756,0.831 -0.351,0 -0.888,-0.25 -1.192,-0.556l-0.553,-0.556 -0.426,0.568 -0.426,0.568 -0.263,-1.056c-0.345,-1.385 0.132,-2.291 1.206,-2.291 0.419,0 0.658,-0.107 0.573,-0.256 -0.425,-0.742 -2.547,0.016 -3.19,1.139 -0.926,1.617 -0.487,4.228 0.71,4.228 0.178,0 -0.234,0.3 -0.916,0.667 -0.995,0.535 -1.355,0.913 -1.825,1.915 -0.493,1.05 -0.821,1.376 -2.063,2.052 -1.561,0.85 -3.963,2.744 -3.963,3.125 0,0.124 0.453,-0.166 1.006,-0.645C17.141,26.369 18.238,25.585 18.473,25.585c0.084,0 0.093,0.167 0.02,0.37 -0.093,0.262 -0.018,0.323 0.258,0.209 0.308,-0.127 0.279,0.007 -0.134,0.634l-0.525,0.795 0.503,0.082c0.476,0.078 0.457,0.15 -0.358,1.36 -0.473,0.703 -0.921,1.471 -0.995,1.707 -0.074,0.236 -0.278,0.37 -0.453,0.298 -0.228,-0.095 -0.298,0.07 -0.247,0.584 0.089,0.898 -0.219,1.23 -1.743,1.878 -1.481,0.63 -1.838,0.659 -1.419,0.114 0.243,-0.316 0.247,-0.445 0.019,-0.597 -0.437,-0.291 -0.124,-1.751 0.675,-3.154 0.381,-0.668 0.594,-1.214 0.475,-1.214 -0.378,0 -1.461,2.259 -1.593,3.323 -0.114,0.921 -0.216,1.051 -1.03,1.314 -1.05,0.339 -2.74,1.748 -2.383,1.987 0.136,0.091 -0.28,0.746 -0.951,1.498 -1.279,1.432 -1.294,1.444 -1.476,1.126zM11.115,16.64c0.081,-0.141 0.04,-0.256 -0.09,-0.256 -0.13,0 -0.303,0.115 -0.383,0.256 -0.081,0.141 -0.04,0.256 0.09,0.256 0.13,0 0.303,-0.115 0.383,-0.256zM22.709,15.346c-0.085,-0.149 -0.025,-0.609 0.133,-1.022 0.205,-0.534 0.452,-0.751 0.852,-0.751 0.733,0 0.719,-0.352 -0.029,-0.72 -0.326,-0.16 -0.592,-0.393 -0.592,-0.517 0,-0.352 -1.269,-0.027 -1.588,0.406 -0.339,0.459 0.159,2.024 0.813,2.556 0.48,0.391 0.617,0.407 0.412,0.048zM24.02,11.92c0,-0.065 -0.24,-0.257 -0.533,-0.425 -0.483,-0.278 -0.503,-0.267 -0.208,0.119 0.303,0.396 0.741,0.577 0.741,0.306zM24.063,10.195c-0.17,-0.183 -0.28,-0.199 -0.28,-0.038 0,0.341 0.28,0.643 0.438,0.473 0.067,-0.073 -0.004,-0.268 -0.158,-0.435z"
//android:strokeWidth="0.061"
//android:strokeColor="#000" />
