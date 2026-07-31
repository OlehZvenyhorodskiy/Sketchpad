package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.academic.study.ReviewGrade
import com.example.data.models.FlashcardEntity
import com.example.data.models.StudyDeckEntity
import com.example.data.models.StudyDeckSummary

/**
 * Standalone, state-hoisted UI for offline study decks.
 *
 * A caller normally collects [StudyDeckRepository.observeDecks], `observeCards`, and
 * `observeDueCards`, then forwards the callbacks to the repository from its coroutine scope.
 */
@Composable
fun StudyDeckDialog(
    deckSummaries: List<StudyDeckSummary>,
    selectedDeck: StudyDeckEntity?,
    cards: List<FlashcardEntity>,
    dueCards: List<FlashcardEntity>,
    onSelectDeck: (String) -> Unit,
    onCreateDeck: (title: String, description: String) -> Unit,
    onAddCard: (deckId: String, prompt: String, answer: String, hint: String) -> Unit,
    onGradeCard: (card: FlashcardEntity, grade: ReviewGrade) -> Unit,
    onDeleteCard: (FlashcardEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var destination by rememberSaveable { mutableStateOf(StudyDeckDestination.DECKS) }
    var reviewQueue by remember { mutableStateOf<List<String>>(emptyList()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f)
                .widthIn(max = 920.dp)
                .heightIn(min = 480.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.fillMaxSize()) {
                StudyDeckHeader(
                    title = when (destination) {
                        StudyDeckDestination.DECKS -> "Study decks"
                        StudyDeckDestination.CARDS -> selectedDeck?.title ?: "Study deck"
                        StudyDeckDestination.REVIEW -> "Review"
                    },
                    canNavigateBack = destination != StudyDeckDestination.DECKS,
                    onBack = {
                        destination = if (destination == StudyDeckDestination.REVIEW) {
                            StudyDeckDestination.CARDS
                        } else {
                            StudyDeckDestination.DECKS
                        }
                    },
                    onDismiss = onDismiss
                )
                HorizontalDivider()

                when (destination) {
                    StudyDeckDestination.DECKS -> DeckList(
                        decks = deckSummaries,
                        onCreateDeck = onCreateDeck,
                        onOpenDeck = {
                            onSelectDeck(it)
                            destination = StudyDeckDestination.CARDS
                        }
                    )

                    StudyDeckDestination.CARDS -> DeckCards(
                        deck = selectedDeck,
                        cards = cards,
                        dueCount = dueCards.size,
                        onAddCard = onAddCard,
                        onDeleteCard = onDeleteCard,
                        onStartReview = {
                            reviewQueue = dueCards.map { it.id }
                            destination = StudyDeckDestination.REVIEW
                        }
                    )

                    StudyDeckDestination.REVIEW -> ReviewDeck(
                        queueIds = reviewQueue,
                        availableCards = dueCards,
                        onGrade = { card, grade ->
                            reviewQueue = reviewQueue.drop(1)
                            onGradeCard(card, grade)
                        },
                        onFinish = { destination = StudyDeckDestination.CARDS }
                    )
                }
            }
        }
    }
}

private enum class StudyDeckDestination { DECKS, CARDS, REVIEW }

@Composable
private fun StudyDeckHeader(
    title: String,
    canNavigateBack: Boolean,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (canNavigateBack) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        } else {
            Icon(
                Icons.Outlined.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(12.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }
}

@Composable
private fun DeckList(
    decks: List<StudyDeckSummary>,
    onCreateDeck: (String, String) -> Unit,
    onOpenDeck: (String) -> Unit
) {
    var showCreator by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (decks.isEmpty()) "Create your first deck" else "Review a little every day",
                style = MaterialTheme.typography.bodyLarge
            )
            FilledTonalButton(onClick = { showCreator = !showCreator }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New deck")
            }
        }

        if (showCreator) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Deck title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCreator = false }) { Text("Cancel") }
                        Button(
                            onClick = {
                                onCreateDeck(title, description)
                                title = ""
                                description = ""
                                showCreator = false
                            },
                            enabled = title.isNotBlank()
                        ) { Text("Create") }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(decks, key = { it.deck.id }) { summary ->
                Card(onClick = { onOpenDeck(summary.deck.id) }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                summary.deck.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (summary.deck.description.isNotBlank()) {
                                Text(
                                    summary.deck.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                "${summary.cardCount} cards",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DueBadge(summary.dueCount)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckCards(
    deck: StudyDeckEntity?,
    cards: List<FlashcardEntity>,
    dueCount: Int,
    onAddCard: (String, String, String, String) -> Unit,
    onDeleteCard: (FlashcardEntity) -> Unit,
    onStartReview: () -> Unit
) {
    if (deck == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a deck to continue")
        }
        return
    }

    var showCreator by rememberSaveable(deck.id) { mutableStateOf(false) }
    var prompt by rememberSaveable(deck.id) { mutableStateOf("") }
    var answer by rememberSaveable(deck.id) { mutableStateOf("") }
    var hint by rememberSaveable(deck.id) { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onStartReview, enabled = dueCount > 0) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (dueCount > 0) "Review $dueCount due" else "Nothing due")
            }
            OutlinedButton(onClick = { showCreator = !showCreator }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add card")
            }
        }

        if (showCreator) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text("Question or prompt") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        label = { Text("Answer") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = hint,
                        onValueChange = { hint = it },
                        label = { Text("Hint (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCreator = false }) { Text("Cancel") }
                        Button(
                            onClick = {
                                onAddCard(deck.id, prompt, answer, hint)
                                prompt = ""
                                answer = ""
                                hint = ""
                                showCreator = false
                            },
                            enabled = prompt.isNotBlank() && answer.isNotBlank()
                        ) { Text("Add") }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        if (cards.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No cards yet. Add a question and answer to begin.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cards, key = { it.id }) { card ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(card.prompt, fontWeight = FontWeight.Medium, maxLines = 2)
                                Text(
                                    card.answer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onDeleteCard(card) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete card")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewDeck(
    queueIds: List<String>,
    availableCards: List<FlashcardEntity>,
    onGrade: (FlashcardEntity, ReviewGrade) -> Unit,
    onFinish: () -> Unit
) {
    val card = queueIds.firstOrNull()?.let { id -> availableCards.firstOrNull { it.id == id } }
    var answerVisible by rememberSaveable(card?.id) { mutableStateOf(false) }
    var hintVisible by rememberSaveable(card?.id) { mutableStateOf(false) }

    if (card == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Text("Review complete", style = MaterialTheme.typography.headlineSmall)
            Text("Nice work — your next reviews are scheduled offline.")
            Spacer(Modifier.height(20.dp))
            Button(onClick = onFinish) { Text("Back to deck") }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "${queueIds.size} remaining",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    card.prompt,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (hintVisible && card.hint.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Hint: ${card.hint}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (answerVisible) {
                    Spacer(Modifier.height(22.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(22.dp))
                    Text(card.answer, style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        if (!answerVisible) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (card.hint.isNotBlank()) {
                    OutlinedButton(onClick = { hintVisible = true }) { Text("Show hint") }
                }
                Button(onClick = { answerVisible = true }) { Text("Show answer") }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GradeButton("Again", Color(0xFFB3261E), Modifier.weight(1f)) {
                    onGrade(card, ReviewGrade.AGAIN)
                }
                GradeButton("Hard", Color(0xFF9A6700), Modifier.weight(1f)) {
                    onGrade(card, ReviewGrade.HARD)
                }
                GradeButton("Good", Color(0xFF19723B), Modifier.weight(1f)) {
                    onGrade(card, ReviewGrade.GOOD)
                }
                GradeButton("Easy", MaterialTheme.colorScheme.primary, Modifier.weight(1f)) {
                    onGrade(card, ReviewGrade.EASY)
                }
            }
        }
    }
}

@Composable
private fun GradeButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = color)
    ) { Text(label, maxLines = 1) }
}

@Composable
private fun DueBadge(count: Int) {
    Surface(
        color = if (count > 0) MaterialTheme.colorScheme.primaryContainer else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Text(
            text = if (count > 0) "$count due" else "Up to date",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}
