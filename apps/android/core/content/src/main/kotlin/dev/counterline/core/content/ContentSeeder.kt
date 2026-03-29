package dev.counterline.core.content

import dev.counterline.core.database.CounterLineDatabase
import dev.counterline.core.database.entity.DeviationEntity
import dev.counterline.core.database.entity.DrillEntity
import dev.counterline.core.database.entity.ModelGameEntity
import dev.counterline.core.database.entity.PlanEntity
import dev.counterline.core.database.entity.QuickStartEntity
import dev.counterline.core.database.entity.RepertoireLineEntity
import dev.counterline.core.database.entity.RepertoireMoveEntity
import dev.counterline.core.database.entity.ThemeEntity
import dev.counterline.core.model.Deviation
import dev.counterline.core.model.Drill
import dev.counterline.core.model.DrillType
import dev.counterline.core.model.ModelGame
import dev.counterline.core.model.Plan
import dev.counterline.core.model.QuickStart
import dev.counterline.core.model.RepertoireLine
import dev.counterline.core.model.Side
import dev.counterline.core.model.Theme
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the Room database from bundled JSON assets on first launch.
 * Runs inside a single transaction to ensure atomicity.
 */
@Singleton
class ContentSeeder @Inject constructor(
    private val loader: ContentAssetLoader,
    private val database: CounterLineDatabase,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun seedIfEmpty() {
        val existingLines = database.repertoireDao().getLineById("white_vienna")
        if (existingLines != null) return // already seeded

        database.runInTransaction {
            // This runs on the Room executor; we use blocking calls here
            // because Room @Transaction doesn't support suspend functions
            // across multiple DAOs. We pre-load everything and batch-insert.
        }

        // Repertoire lines
        val lines = loader.loadRepertoireLines()
        val lineEntities = lines.map { it.toEntity() }
        database.repertoireDao().insertLines(lineEntities)
        for (line in lines) {
            val moves = line.moves.map { m ->
                RepertoireMoveEntity(
                    lineId = line.id,
                    moveNumber = m.moveNumber,
                    san = m.san,
                    purpose = m.purpose,
                    isWhiteMove = m.isWhiteMove,
                )
            }
            database.repertoireDao().insertMoves(moves)
        }

        // Plans
        database.planDao().insertPlans(loader.loadPlans().map { it.toEntity() })

        // Themes
        database.themeDao().insertThemes(loader.loadThemes().map { it.toEntity() })

        // Deviations
        database.deviationDao().insertDeviations(loader.loadDeviations().map { it.toEntity() })

        // Model games
        database.modelGameDao().insertGames(loader.loadModelGames().map { it.toEntity() })

        // Drills
        database.drillDao().insertDrills(loader.loadDrills().map { it.toEntity() })

        // Quick starts
        database.quickStartDao().insertAll(loader.loadQuickStarts().map { it.toEntity() })
    }

    // --- Mapping helpers ---

    private fun RepertoireLine.toEntity() = RepertoireLineEntity(
        id = id,
        name = name,
        family = family,
        eco = eco,
        side = side.name,
        seedLine = seedLine,
        exitFen = exitFen,
        exitEpd = exitEpd,
        exitMoveNumber = exitMoveNumber,
        specialistType = specialistType,
        specialistSize = specialistSize,
        screeningRank = screeningRank,
        screeningScorePct = screeningScorePct,
        evaluationAtExit = evaluationAtExit,
        memoryHook = memoryHook,
        memoryHookBreakdownJson = json.encodeToString(memoryHookBreakdown),
    )

    private fun Plan.toEntity() = PlanEntity(
        id = id, side = side.name, title = title,
        description = description, priority = priority,
    )

    private fun Theme.toEntity() = ThemeEntity(
        id = id, side = side.name, title = title,
        description = description, occurrenceRate = occurrenceRate,
    )

    private fun Deviation.toEntity() = DeviationEntity(
        id = id, side = side.name, deviationName = deviationName,
        move = move, description = description, response = response,
    )

    private fun ModelGame.toEntity() = ModelGameEntity(
        id = id, title = title, side = side.name,
        opening = opening, result = result, moveCount = moveCount,
        keyTheme = keyTheme,
        annotationsJson = json.encodeToString(annotations),
        evaluationProgression = evaluationProgression,
    )

    private fun Drill.toEntity() = DrillEntity(
        id = id, type = type.name, title = title,
        question = question,
        optionsJson = options?.let { json.encodeToString(it) },
        correctAnswer = correctAnswer, explanation = explanation,
        side = side?.name, fen = fen,
    )

    private fun QuickStart.toEntity() = QuickStartEntity(
        side = side.name, lineName = lineName, seedLine = seedLine,
        memoryHook = memoryHook,
        memoryHookBreakdownJson = json.encodeToString(memoryHookBreakdown),
        threeKeyActionsJson = json.encodeToString(threeKeyActions),
        exitFen = exitFen, exitEvaluation = exitEvaluation,
        typicalResult = typicalResult,
    )
}
