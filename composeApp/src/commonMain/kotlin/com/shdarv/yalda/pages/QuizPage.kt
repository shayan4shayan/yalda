package com.shdarv.yalda.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.collectAsState
import com.shdarv.yalda.audit.QuizAuditSession
import com.shdarv.yalda.audit.AuditTrail
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shdarv.yalda.db.Category
import com.shdarv.yalda.db.WordEntry
import com.shdarv.yalda.localization.LocalAppStrings
import com.shdarv.yalda.platform.rememberSpeechEngine
import com.shdarv.yalda.platform.rememberTtsEngine
import com.shdarv.yalda.viewmodel.WordsViewModel

enum class QuizType(val label: String) {
    MultipleChoice("Choice"),
    Spelling("Spelling"),
    Speaking("Speaking")
}

private data class MultipleChoiceQuestion(
    val word: WordEntry,
    val options: List<String>,
    val correct: String
)

private val SuccessColor = Color(0xFF16A34A)
private val ErrorColor = Color(0xFFDC2626)

@Composable
fun QuizPage(
    profileId: Long?,
    categories: List<Category>,
    wordsViewModel: WordsViewModel
) {
    val strings = LocalAppStrings.current
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedQuizType by remember { mutableStateOf(QuizType.MultipleChoice) }
    var categoryMenuOpen by remember { mutableStateOf(false) }

    val profileCategories = categories.filter { it.profileIdFk == profileId }
    val scopedCategoryId = selectedCategoryId?.takeIf { categoryId ->
        profileCategories.any { it.id == categoryId }
    }
    val selectedCategory = profileCategories.firstOrNull { it.id == scopedCategoryId }
    var wordPool by remember(profileId, scopedCategoryId) { mutableStateOf<List<WordEntry>>(emptyList()) }
    val auditWriteFailed by AuditTrail.writeFailed.collectAsState()
    val categoryLabel = selectedCategory?.name ?: strings("All categories")

    LaunchedEffect(profileId) {
        selectedCategoryId = null
        categoryMenuOpen = false
    }

    LaunchedEffect(profileId, scopedCategoryId) {
        wordPool = if (profileId == null) {
            emptyList()
        } else {
            wordsViewModel.getWordsForQuiz(profileId, scopedCategoryId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = strings("Quiz"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${strings(selectedQuizType.label)} · $categoryLabel · ${strings("{0} words", wordPool.size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                OutlinedButton(
                    onClick = { categoryMenuOpen = true },
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Category,
                        contentDescription = strings("Category"),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = categoryLabel,
                        modifier = Modifier.widthIn(max = 132.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DropdownMenu(
                    expanded = categoryMenuOpen,
                    onDismissRequest = { categoryMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(strings("All categories")) },
                        onClick = {
                            selectedCategoryId = null
                            categoryMenuOpen = false
                        }
                    )
                    profileCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                selectedCategoryId = category.id
                                categoryMenuOpen = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        QuizTypeSelector(
            selected = selectedQuizType,
            onSelect = { selectedQuizType = it }
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (profileId == null) {
            QuizNotice(
                title = strings("Select a profile"),
                message = strings("Choose or create a profile before starting a quiz.")
            )
            return@Column
        }

        if (auditWriteFailed) {
            Text(strings("History could not be saved. Retrying…"), color = MaterialTheme.colorScheme.error)
        }
        key(profileId, scopedCategoryId, selectedQuizType) {
            val audit = remember {
                QuizAuditSession(profileId, scopedCategoryId, selectedQuizType.name,
                    profileCategories.associate { it.id to it.name })
            }
            DisposableEffect(audit) { onDispose { audit.close() } }
            when (selectedQuizType) {
                QuizType.MultipleChoice -> MultipleChoiceQuiz(wordPool, audit)
                QuizType.Spelling -> SpellingQuiz(wordPool, audit)
                QuizType.Speaking -> SpeakingQuiz(wordPool, audit)
            }
        }
    }
}

@Composable
private fun QuizTypeSelector(
    selected: QuizType,
    onSelect: (QuizType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuizType.entries.forEach { type ->
            val isSelected = selected == type
            val shape = RoundedCornerShape(12.dp)
            val contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
            val modifier = Modifier.weight(1f)

            if (isSelected) {
                Button(
                    onClick = { onSelect(type) },
                    modifier = modifier,
                    shape = shape,
                    contentPadding = contentPadding
                ) {
                    QuizTypeLabel(type = type)
                }
            } else {
                OutlinedButton(
                    onClick = { onSelect(type) },
                    modifier = modifier,
                    shape = shape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = contentPadding,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    QuizTypeLabel(type = type)
                }
            }
        }
    }
}

@Composable
private fun QuizTypeLabel(type: QuizType) {
    val strings = LocalAppStrings.current
    Icon(
        type.icon,
        contentDescription = null,
        modifier = Modifier.size(16.dp)
    )
    Spacer(modifier = Modifier.width(5.dp))
    Text(
        text = strings(type.label),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        fontSize = 12.sp
    )
}

private val QuizType.icon: ImageVector
    get() = when (this) {
        QuizType.MultipleChoice -> Icons.Outlined.Quiz
        QuizType.Spelling -> Icons.Outlined.Edit
        QuizType.Speaking -> Icons.Outlined.Mic
    }

@Composable
private fun MultipleChoiceQuiz(wordPool: List<WordEntry>, audit: QuizAuditSession) {
    val strings = LocalAppStrings.current
    var question by remember { mutableStateOf<MultipleChoiceQuestion?>(null) }
    var feedback by remember { mutableStateOf("") }
    var selectedOption by remember { mutableStateOf<String?>(null) }

    fun nextQuestion() {
        if (wordPool.size < 4) {
            question = null
            return
        }
        val correctWord = wordPool.random()
        val distractors = wordPool.shuffled().filter { it.wordId != correctWord.wordId }.take(3)
        val options = (distractors.map { it.meaning } + correctWord.meaning).shuffled()
        question = MultipleChoiceQuestion(correctWord, options, correctWord.meaning)
        audit.showQuestion(correctWord)
        feedback = ""
        selectedOption = null
    }

    LaunchedEffect(wordPool) {
        nextQuestion()
    }

    if (wordPool.size < 4) {
        QuizNotice(
            title = strings("More words needed"),
            message = strings("Add at least 4 words in this category to start multiple choice.")
        )
        return
    }

    question?.let { q ->
        QuizCard {
            QuizPrompt(
                label = strings("Multiple choice"),
                title = q.word.word,
                message = strings("Choose the matching meaning.")
            )

            Spacer(modifier = Modifier.height(18.dp))

            q.options.forEach { option ->
                QuizOptionButton(
                    option = option,
                    isSelected = selectedOption == option,
                    isCorrect = selectedOption == option && option == q.correct,
                    onClick = {
                        if (!audit.answer(q.word, option, option == q.correct)) return@QuizOptionButton
                        selectedOption = option
                        feedback = if (option == q.correct) {
                            strings("Correct.")
                        } else {
                            strings("Not quite. Try another answer.")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (feedback.isNotBlank()) {
                FeedbackMessage(
                    text = feedback,
                    isSuccess = selectedOption == q.correct
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = { nextQuestion() }) {
                    Text(strings("Next"))
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.AutoMirrored.Outlined.NavigateNext,
                        contentDescription = strings("Next"),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpellingQuiz(wordPool: List<WordEntry>, audit: QuizAuditSession) {
    val strings = LocalAppStrings.current
    val ttsEngine = rememberTtsEngine()
    var currentWord by remember { mutableStateOf<WordEntry?>(null) }
    var input by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }

    fun nextWord() {
        if (wordPool.isNotEmpty()) {
            currentWord = wordPool.random()
            audit.showQuestion(checkNotNull(currentWord))
            input = ""
            feedback = ""
        }
    }

    LaunchedEffect(wordPool) {
        nextWord()
    }

    if (wordPool.isEmpty()) {
        QuizNotice(
            title = strings("No words yet"),
            message = strings("Add words to this category before starting spelling practice.")
        )
        return
    }

    currentWord?.let { word ->
        QuizCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = strings("Spelling"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings("Listen and type"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings("Meaning: {0}", word.meaning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
                Button(
                    onClick = { ttsEngine.speak(word.word) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = strings("Play word"),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(strings("Play"), maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(strings("Type the word")) },
                enabled = !audit.isSolved,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val correct = input.trim().equals(word.word, ignoreCase = true)
                        if (!audit.answer(word, input, correct)) return@Button
                        feedback = if (correct) {
                            strings("Correct.")
                        } else {
                            strings("Not quite. It was {0}.", word.word)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = input.isNotBlank() && !audit.isSolved
                ) {
                    Text(strings("Check"))
                }
                TextButton(onClick = { nextWord() }) {
                    Text(strings("Next"))
                    Icon(
                        Icons.AutoMirrored.Outlined.NavigateNext,
                        contentDescription = strings("Next"),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (feedback.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                FeedbackMessage(
                    text = feedback,
                    isSuccess = input.trim().equals(word.word, ignoreCase = true)
                )
            }
        }
    }
}

@Composable
private fun SpeakingQuiz(wordPool: List<WordEntry>, audit: QuizAuditSession) {
    val strings = LocalAppStrings.current
    val speechEngine = rememberSpeechEngine()
    val ttsEngine = rememberTtsEngine()
    var currentWord by remember { mutableStateOf<WordEntry?>(null) }
    var heard by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var speechGeneration by remember { mutableStateOf(0) }
    DisposableEffect(speechEngine) {
        onDispose { speechGeneration++; speechEngine.stop() }
    }

    fun nextWord() {
        if (wordPool.isNotEmpty()) {
            speechGeneration++
            speechEngine.stop()
            currentWord = wordPool.random()
            audit.showQuestion(checkNotNull(currentWord))
            heard = ""
            feedback = ""
            isListening = false
        }
    }

    LaunchedEffect(wordPool) {
        if (speechEngine.isSupported) nextWord()
    }

    if (wordPool.isEmpty()) {
        QuizNotice(
            title = strings("No words yet"),
            message = strings("Add words to this category before starting speaking practice.")
        )
        return
    }

    if (!speechEngine.isSupported) {
        QuizNotice(
            title = strings("Speaking unavailable"),
            message = strings("Speech recognition is not supported on this device.")
        )
        return
    }

    currentWord?.let { word ->
        QuizCard {
            QuizPrompt(
                label = strings("Speaking"),
                title = strings("Say what you hear"),
                message = strings("Listen first, then record your pronunciation.")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { ttsEngine.speak(word.word) },
                    enabled = !isListening,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = strings("Play word"),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings("Play"), maxLines = 1)
                }
                Button(
                    onClick = {
                        heard = ""
                        feedback = ""
                        val generation = ++speechGeneration
                        speechEngine.startListening(
                            onResult = result@{
                                if (generation != speechGeneration) return@result
                                if (!audit.answer(word, it, it.trim().equals(word.word, ignoreCase = true))) {
                                    isListening = false
                                    return@result
                                }
                                isListening = false
                                heard = it
                                feedback = if (it.trim().equals(word.word, ignoreCase = true)) {
                                    strings("Great pronunciation.")
                                } else {
                                    strings("Heard “{0}”. Try again.", it)
                                }
                            },
                            onError = error@{ error ->
                                if (generation != speechGeneration) return@error
                                isListening = false
                                feedback = strings(error)
                            },
                            onListeningChanged = { listening ->
                                if (generation == speechGeneration) isListening = listening
                            }
                        )
                    },
                    enabled = !isListening && !audit.isSolved,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.Mic,
                        contentDescription = strings("Start speaking"),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings(if (isListening) "Listening..." else "Speak"),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (heard.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                HeardText(heard)
            }

            if (feedback.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                FeedbackMessage(
                    text = feedback,
                    isSuccess = heard.trim().equals(word.word, ignoreCase = true)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { nextWord() }) {
                    Text(strings("Next"))
                    Icon(
                        Icons.AutoMirrored.Outlined.NavigateNext,
                        contentDescription = strings("Next"),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            content = content
        )
    }
}

@Composable
private fun QuizPrompt(
    label: String,
    title: String,
    message: String
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
    )
}

@Composable
private fun QuizOptionButton(
    option: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isCorrect -> SuccessColor
        isSelected -> ErrorColor
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val containerColor = when {
        isCorrect -> SuccessColor.copy(alpha = 0.12f)
        isSelected -> ErrorColor.copy(alpha = 0.10f)
        else -> MaterialTheme.colorScheme.surface
    }

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = option,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FeedbackMessage(
    text: String,
    isSuccess: Boolean
) {
    val color = if (isSuccess) SuccessColor else ErrorColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isSuccess) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun HeardText(heard: String) {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = strings("Heard"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = heard,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun QuizNotice(
    title: String,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )
        }
    }
}
