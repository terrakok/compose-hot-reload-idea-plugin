package org.jetbrains.compose.hotreload

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.jewel.bridge.addComposeTab
import org.jetbrains.jewel.ui.component.IconActionButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import org.jetbrains.jewel.ui.icons.AllIconsKeys

class HotReloadToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun isDumbAware() = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val service = project.service<HotReloadService>()
        toolWindow.addComposeTab { HotReloadPanel(service) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HotReloadPanel(service: HotReloadService) {
    val status = service.engine.appStateFlow.collectAsState().value
    val compilerLogs = rememberTextFieldState("")
    val agentLogs = rememberTextFieldState("")
    val runtimeLogs = rememberTextFieldState("")

    LaunchedEffect(Unit) {
        service.engine.logsStateFlow.collect { messages ->
            compilerLogs.edit {
                val text = messages
                    .filter { it.tag == OrchestrationMessage.LogMessage.TAG_COMPILER }
                    .joinToString("\n") { it.message }
                this.replace(0, this.length, text)
            }
            agentLogs.edit {
                val text = messages
                    .filter { it.tag == OrchestrationMessage.LogMessage.TAG_AGENT }
                    .joinToString("\n") { it.message }
                this.replace(0, this.length, text)
            }
            runtimeLogs.edit {
                val text = messages
                    .filter { it.tag == OrchestrationMessage.LogMessage.TAG_RUNTIME }
                    .joinToString("\n") { it.message }
                this.replace(0, this.length, text)
            }
        }
    }

    AdaptiveContainer(
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        AdaptiveContainer(isHorizontal = false) {
            IconActionButton(
                key = AllIconsKeys.Run.Stop,
                contentDescription = "Stop The Application",
                tooltip = { Text("Stop The Application") },
                onClick = { service.engine.stop() },
                enabled = status == AppState.RUNNING,
                colorFilter = if (status == AppState.RUNNING) null else ColorFilter.tint(Color(0xFF808080))
            )
            IconActionButton(
                key = AllIconsKeys.Actions.Rerun,
                contentDescription = "Reload Changes",
                tooltip = { Text("Reload Changes") },
                onClick = { service.engine.reload() },
                enabled = status == AppState.RUNNING,
                colorFilter = if (status == AppState.RUNNING) null else ColorFilter.tint(Color(0xFF808080))
            )
            IconActionButton(
                key = AllIconsKeys.RunConfigurations.RerunFailedTests,
                contentDescription = "Retry Failed Composition",
                tooltip = { Text("Retry Failed Composition") },
                onClick = { service.engine.retryFailedComposition() },
                enabled = status == AppState.RUNNING,
                colorFilter = if (status == AppState.RUNNING) null else ColorFilter.tint(Color(0xFF808080))
            )
            IconActionButton(
                key = AllIconsKeys.Actions.ClearCash,
                contentDescription = "Clean Composition",
                tooltip = { Text("Clean Composition") },
                onClick = { service.engine.cleanComposition() },
                enabled = status == AppState.RUNNING,
                colorFilter = if (status == AppState.RUNNING) null else ColorFilter.tint(Color(0xFF808080))
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        AdaptiveContainer {
            LogField(
                modifier = Modifier.weight(1f).fillMaxSize(),
                title = "Compiler Logs",
                logs = compilerLogs
            )
            Spacer(modifier = Modifier.size(8.dp))
            LogField(
                modifier = Modifier.weight(1f).fillMaxSize(),
                title = "Agent Logs",
                logs = agentLogs
            )
            Spacer(modifier = Modifier.size(8.dp))
            LogField(
                modifier = Modifier.weight(1f).fillMaxSize(),
                title = "Runtime Logs",
                logs = runtimeLogs
            )
        }
    }
}

@Composable
private fun LogField(
    title: String,
    logs: TextFieldState,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(title)
        val scrollState = rememberScrollState()
        LaunchedEffect(scrollState.maxValue) {
            scrollState.scrollTo(scrollState.maxValue)
        }
        TextArea(
            state = logs,
            readOnly = true,
            scrollState = scrollState,
            modifier = Modifier.fillMaxSize()
        )
    }
}