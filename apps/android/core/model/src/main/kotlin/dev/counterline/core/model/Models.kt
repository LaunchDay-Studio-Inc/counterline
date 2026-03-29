package dev.counterline.core.model

import kotlinx.serialization.Serializable

/** A single chess move with metadata for display */
@Serializable
data class RepertoireMove(
    val moveNumber: Int,
    val san: String,
    val purpose: String,
    val isWhiteMove: Boolean,
    val whyThisMove: String = "",
    val keyPlanCallout: String = "",
)

/** A complete opening line (seed line through exit) */
@Serializable
data class RepertoireLine(
    val id: String,
    val name: String,
    val family: String,
    val eco: String,
    val side: Side,
    val seedLine: String,
    val exitFen: String,
    val exitEpd: String,
    val exitMoveNumber: Int,
    val specialistType: String,
    val specialistSize: String,
    val screeningRank: Int,
    val screeningScorePct: Double,
    val evaluationAtExit: String,
    val moves: List<RepertoireMove>,
    val memoryHook: String,
    val memoryHookBreakdown: List<String>,
    val skillLevel: SkillLevel = SkillLevel.INTERMEDIATE,
)

@Serializable
enum class Side { WHITE, BLACK }

/** Skill layer controlling content depth visibility */
@Serializable
enum class SkillLevel {
    INTERMEDIATE,
    ADVANCED_CLUB,
    EXPERT_MASTER,
    ELITE_LAB,
}

/** Study mode selection */
@Serializable
enum class StudyMode {
    LEARN,
    RECALL,
    DEVIATION_DRILL,
    PLANS_PATTERNS,
    MODEL_GAME_REPLAY,
    MISTAKE_REVIEW,
    EXAM,
    QUICK_5,
}

/** Confidence grading after a recall attempt */
@Serializable
enum class ReviewGrade {
    FAIL,
    HARD,
    GOOD,
    EASY,
}

/** A plan to execute after reaching the exit position */
@Serializable
data class Plan(
    val id: String,
    val side: Side,
    val title: String,
    val description: String,
    val priority: Int,
    val skillLevel: SkillLevel = SkillLevel.INTERMEDIATE,
)

/** A tactical or strategic theme */
@Serializable
data class Theme(
    val id: String,
    val side: Side,
    val title: String,
    val description: String,
    val occurrenceRate: String? = null,
    val skillLevel: SkillLevel = SkillLevel.INTERMEDIATE,
)

/** Ideal piece placement for a given position */
@Serializable
data class PiecePlacement(
    val piece: String,
    val idealSquare: String,
    val purpose: String,
    val side: Side,
)

/** An opponent deviation from the main line */
@Serializable
data class Deviation(
    val id: String,
    val side: Side,
    val deviationName: String,
    val move: String,
    val description: String,
    val response: String,
    val strategicIdea: String = "",
    val skillLevel: SkillLevel = SkillLevel.INTERMEDIATE,
)

/** An annotated model game */
@Serializable
data class ModelGame(
    val id: String,
    val title: String,
    val side: Side,
    val opening: String,
    val result: String,
    val moveCount: Int,
    val keyTheme: String,
    val annotations: List<GameAnnotation>,
    val evaluationProgression: String,
)

@Serializable
data class GameAnnotation(
    val moveNumber: Int,
    val comment: String,
    val evaluation: String? = null,
)

/** A drill exercise */
@Serializable
data class Drill(
    val id: String,
    val type: DrillType,
    val title: String,
    val question: String,
    val options: List<String>? = null,
    val correctAnswer: String,
    val explanation: String,
    val side: Side? = null,
    val fen: String? = null,
    val skillLevel: SkillLevel = SkillLevel.INTERMEDIATE,
    val lineId: String? = null,
)

@Serializable
enum class DrillType {
    FILL_IN_BLANK,
    CHOOSE_MOVE,
    FEN_RECOGNITION,
    PLANS_QUIZ,
    FLASHCARD,
    TACTICAL_MOTIF,
    STRUCTURE_FLASHCARD,
    TRANSITION_QUIZ,
    COMPARE_POSITION,
    DEVIATION_RESPONSE,
}

/** Claims manifest — loaded from content/claims_manifest.json */
@Serializable
data class ClaimsManifest(
    val version: String,
    val approved_headline: String,
    val approved_subtitle: String,
    val approved_badges: List<String>,
    val approved_promises: List<String>,
    val forbidden_phrases: List<String>,
    val proof_status: ProofStatus,
    val scope_statement: String,
    val allowed_performance_statements: List<String>,
    val required_disclaimers: List<String>,
)

@Serializable
data class ProofStatus(
    val level: String,
    val confidence: String,
    val combined_score: String,
    val combined_elo_estimate: String,
    val black_specialist_score: String,
    val black_specialist_elo_estimate: String,
    val white_specialist_score: String,
    val match_size: String,
    val time_control: String,
    val threads: Int,
    val hash_mb: Int,
    val error_bars: String,
    val games_needed_for_95pct: String,
    val assessment: String,
)

/** Proof match result */
@Serializable
data class ProofMatch(
    val id: String,
    val label: String,
    val suite: String,
    val engine1: String,
    val engine2: String,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val score: Double,
    val pct: Double,
    val asWhite: String,
    val asBlack: String,
    val finding: String? = null,
    val eloEstimate: String? = null,
)

/** Summary of proof results */
@Serializable
data class ProofSummary(
    val headlineResult: String,
    val blackSpecialistResult: String,
    val whiteSpecialistResult: String,
    val nullWrapperControl: String,
    val statisticalStatus: String,
    val gamesNeeded: String,
)

/** Quick-start summary for cheat sheets */
@Serializable
data class QuickStart(
    val side: Side,
    val lineName: String,
    val seedLine: String,
    val memoryHook: String,
    val memoryHookBreakdown: List<String>,
    val threeKeyActions: List<String>,
    val exitFen: String,
    val exitEvaluation: String,
    val typicalResult: String,
)
