package org.jetbrains.compose.hotreload

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.execution.lineMarker.RunLineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.impl.source.tree.LeafPsiElement
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.symbols.isTopLevel
import org.jetbrains.kotlin.analysis.api.symbols.receiverType
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

private const val HOT_RELOAD_ANNOTATION_FQN = "org.jetbrains.compose.reload/DevelopmentEntryPoint"

class HotReloadLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: com.intellij.psi.PsiElement): LineMarkerInfo<*>? {
        if (element !is LeafPsiElement) return null
        if (element.node.elementType != KtTokens.IDENTIFIER) return null

        val ktFun = element.parent as? KtNamedFunction ?: return null
        if (!isValidDevEntryPoint(ktFun)) return null

        val module = ProjectFileIndex.getInstance(ktFun.project).getModuleForFile(ktFun.containingFile.virtualFile)
        if (module == null || module.isDisposed) return null
        val modulePath = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return null

        val entryPoint = DevEntryPoint(
            ktFun.containingKtFile.javaFileFacadeFqName.asString(),
            ktFun.name.orEmpty(),
            modulePath
        )

        return RunLineMarkerProvider.createLineMarker(
            element,
            AllIcons.Actions.RerunAutomatically,
            listOf(RunLineMarkerContributor.Info(RunDevEntryPointAction(entryPoint)))
        )
    }

    private fun isValidDevEntryPoint(ktFun: KtNamedFunction): Boolean = analyze(ktFun) {
        with(ktFun.symbol) {
            if (visibility == KaSymbolVisibility.PRIVATE) return false
            if (!isTopLevel) return false
            if (valueParameters.isNotEmpty()) return false
            if (receiverType != null) return false

            val classId = ClassId.fromString(HOT_RELOAD_ANNOTATION_FQN)
            return classId in annotations
        }
    }
}

private class RunDevEntryPointAction(
    private val entryPoint: DevEntryPoint
) : AnAction({ "Run Development App" }, AllIcons.Actions.RerunAutomatically) {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val service = project.service<HotReloadService>()
        val engine = service.engine

        if (
            engine.appStateFlow.value == AppState.IDLE ||
            ConfirmStopAppDialog().showAndGet()
        ) {
            service.coroutineScope.launch {
                if (engine.appStateFlow.value != AppState.IDLE) {
                    engine.stop()
                    engine.appStateFlow.first { it == AppState.IDLE }
                }
                engine.run(entryPoint)
            }
        }
    }

    private class ConfirmStopAppDialog : DialogWrapper(true) {
        init {
            title = "Stop Application?"
            setOKButtonText("Stop")
            setCancelButtonText("Cancel")
            init()
        }

        override fun createCenterPanel(): JComponent {
            return JPanel(BorderLayout()).apply {
                val label = JLabel("One development application is already running. Stop it?").apply {
                    preferredSize = Dimension(200, 80)
                }
                add(label, BorderLayout.CENTER)
            }
        }
    }
}