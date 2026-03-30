package dev.counterline.feature.preppack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.data.repository.RepertoireSnapshotRepository
import dev.counterline.core.domain.GetRepertoireLinesUseCase
import dev.counterline.core.domain.GetSettingsUseCase
import dev.counterline.core.model.PreparationPack
import dev.counterline.core.model.RepertoireLine
import dev.counterline.core.model.RepertoireSnapshot
import dev.counterline.core.model.Side
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrepPackUiState(
    val lines: List<RepertoireLine> = emptyList(),
    val selectedSide: Side? = null,
    val prepPack: PreparationPack? = null,
    val snapshots: List<RepertoireSnapshot> = emptyList(),
    val snapshotDiff: String? = null,
    val cheatSheet: String = "",
)

@HiltViewModel
class PrepPackViewModel @Inject constructor(
    private val getLines: GetRepertoireLinesUseCase,
    private val getSettings: GetSettingsUseCase,
    private val snapshotRepo: RepertoireSnapshotRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrepPackUiState())
    val uiState: StateFlow<PrepPackUiState> = _uiState

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val settings = getSettings().first()
            val lines = getLines.forSkillLevel(settings.skillLevel).first()
            val snapshots = snapshotRepo.getAll().first()
            _uiState.update {
                it.copy(lines = lines, snapshots = snapshots)
            }
            generateCheatSheet(lines)
        }
    }

    fun filterSide(side: Side?) {
        _uiState.update { it.copy(selectedSide = side) }
        viewModelScope.launch {
            val settings = getSettings().first()
            val all = getLines.forSkillLevel(settings.skillLevel).first()
            val filtered = if (side != null) all.filter { it.side == side } else all
            _uiState.update { it.copy(lines = filtered) }
            generateCheatSheet(filtered)
        }
    }

    fun generatePrepPack() {
        val state = _uiState.value
        val lines = state.lines
        if (lines.isEmpty()) return

        val side = state.selectedSide
        val relevantLines = if (side != null) lines.filter { it.side == side } else lines

        val pack = PreparationPack(
            id = java.util.UUID.randomUUID().toString(),
            label = "${side?.name ?: "Both"} Prep Pack",
            lineIds = relevantLines.map { it.id },
            createdEpochMs = System.currentTimeMillis(),
            notes = "Generated for ${relevantLines.size} lines",
        )
        _uiState.update { it.copy(prepPack = pack) }
    }

    fun takeSnapshot() {
        viewModelScope.launch {
            val state = _uiState.value
            val lines = state.lines
            val snapshot = RepertoireSnapshot(
                id = java.util.UUID.randomUUID().toString(),
                takenEpochMs = System.currentTimeMillis(),
                label = "Snapshot ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
                lineCount = lines.size,
                totalMoves = lines.sumOf { it.moves.size },
                serializedTree = lines.joinToString("\n") { "${it.name}: ${it.moves.size} moves" },
            )
            snapshotRepo.insert(snapshot)
            val snapshots = snapshotRepo.getAll().first()
            _uiState.update { it.copy(snapshots = snapshots) }
        }
    }

    fun compareSnapshots(id1: String, id2: String) {
        viewModelScope.launch {
            val s1 = snapshotRepo.getById(id1)
            val s2 = snapshotRepo.getById(id2)
            if (s1 == null || s2 == null) return@launch

            val diff = buildString {
                appendLine("Comparing: ${s1.label} vs ${s2.label}")
                appendLine("Lines: ${s1.lineCount} → ${s2.lineCount} (${s2.lineCount - s1.lineCount:+d})")
                appendLine("Moves: ${s1.totalMoves} → ${s2.totalMoves} (${s2.totalMoves - s1.totalMoves:+d})")
            }
            _uiState.update { it.copy(snapshotDiff = diff) }
        }
    }

    private fun generateCheatSheet(lines: List<RepertoireLine>) {
        val sheet = buildString {
            appendLine("═══ COUNTERLINE CHEAT SHEET ═══")
            appendLine()
            val whiteLines = lines.filter { it.side == Side.WHITE }
            val blackLines = lines.filter { it.side == Side.BLACK }

            if (whiteLines.isNotEmpty()) {
                appendLine("── WHITE WEAPON ──")
                whiteLines.forEach { line ->
                    appendLine("${line.name} (${line.eco})")
                    line.moves.take(6).forEach { move ->
                        val label = if (move.isWhiteMove) "${move.moveNumber}." else "${move.moveNumber}..."
                        appendLine("  $label ${move.san}  — ${move.purpose}")
                    }
                    appendLine()
                }
            }

            if (blackLines.isNotEmpty()) {
                appendLine("── BLACK WEAPON ──")
                blackLines.forEach { line ->
                    appendLine("${line.name} (${line.eco})")
                    line.moves.take(6).forEach { move ->
                        val label = if (move.isWhiteMove) "${move.moveNumber}." else "${move.moveNumber}..."
                        appendLine("  $label ${move.san}  — ${move.purpose}")
                    }
                    appendLine()
                }
            }
        }
        _uiState.update { it.copy(cheatSheet = sheet) }
    }
}
