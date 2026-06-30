package com.ykis.ykismobkmp.ui.screens.help

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import com.ykis.ykismobkmp.domain.services.FirebaseService
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.*

/**
 * [ManualScreen] — Екран відображення інструкції користувача.
 */
class ManualScreen(
    private val role: UserRole,
    private val onBackClick: () -> Unit
) : Screen {

    @Composable
    override fun Content() {
        val firebaseService = koinInject<FirebaseService>()
        val scope = rememberCoroutineScope()
        val loadingText = stringResource(Res.string.instruction_loading)
        val unavailableText = stringResource(Res.string.instruction_unavailable)
        
        var manualText by remember { mutableStateOf(loadingText) }
        var isLoading by remember { mutableStateOf(true) }

        // Функція завантаження
        val loadData = {
            isLoading = true
            scope.launch {
                val text = firebaseService.getManualText(role)
                manualText = if (text.isBlank()) unavailableText else text
                isLoading = false
            }
        }

        LaunchedEffect(Unit) { loadData() }

        Scaffold(
            topBar = {
                DefaultAppBar(
                    title = stringResource(Res.string.instruction_title),
                    onBackClick = onBackClick,
                    canNavigateBack = true,
                    actionButton = {
                        IconButton(onClick = { 
                            scope.launch {
                                firebaseService.fetchConfiguration()
                                loadData()
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Оновити", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    val styledText = parseMarkdown(manualText)
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (role == UserRole.StandardUser) stringResource(Res.string.manual_resident_title) else stringResource(Res.string.manual_admin_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        SelectionContainer {
                            Text(
                                text = styledText,
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = 24.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    private fun parseMarkdown(text: String): AnnotatedString {
        val cleanText = text
            .replace("\\n", "\n")
            .replace(" #", "\n#")
            .replace(" *", "\n*")
            .replace("\r", "")
        
        return buildAnnotatedString {
            val lines = cleanText.split("\n")
            val primaryColor = Color(0xFF0056D2)

            lines.forEachIndexed { index, rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty()) {
                    append("\n")
                    return@forEachIndexed
                }

                when {
                    line.startsWith("#") -> {
                        val level = line.takeWhile { it == '#' }.length
                        val headerContent = line.replace("#", "").trim()
                        val fontSize = if (level == 1) 24.sp else 20.sp
                        
                        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = fontSize, color = primaryColor)) {
                            appendInlineFormatted(headerContent)
                        }
                        append("\n")
                    }
                    line.startsWith("*") -> {
                        append("  • ")
                        appendInlineFormatted(line.removePrefix("*").trim())
                    }
                    else -> {
                        appendInlineFormatted(rawLine)
                    }
                }
                if (index < lines.size - 1) append("\n")
            }
        }
    }

    private fun AnnotatedString.Builder.appendInlineFormatted(text: String) {
        val parts = text.split("**")
        parts.forEachIndexed { pIndex, part ->
            if (pIndex % 2 != 0) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(part)
                }
            } else {
                append(part)
            }
        }
    }
}
