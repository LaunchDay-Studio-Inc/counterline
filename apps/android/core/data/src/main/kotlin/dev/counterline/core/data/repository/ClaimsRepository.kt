package dev.counterline.core.data.repository

import dev.counterline.core.content.ContentAssetLoader
import dev.counterline.core.model.ClaimsManifest
import dev.counterline.core.model.ProofMatch
import dev.counterline.core.model.ProofSummary
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for claims and proof data.
 * This data is static and loaded from bundled assets.
 */
@Singleton
class ClaimsRepository @Inject constructor(
    private val loader: ContentAssetLoader,
) {
    private var cachedManifest: ClaimsManifest? = null
    private var cachedMatches: List<ProofMatch>? = null

    fun getClaimsManifest(): ClaimsManifest {
        return cachedManifest ?: loader.loadClaimsManifest().also { cachedManifest = it }
    }

    fun getProofMatches(): List<ProofMatch> {
        return cachedMatches ?: loader.loadProofMatches().also { cachedMatches = it }
    }

    fun getProofSummary(): ProofSummary {
        val manifest = getClaimsManifest()
        val status = manifest.proof_status
        return ProofSummary(
            headlineResult = "Combined: ${status.combined_score} (${status.combined_elo_estimate})",
            blackSpecialistResult = "Black specialist: ${status.black_specialist_score} (${status.black_specialist_elo_estimate})",
            whiteSpecialistResult = "White specialist: ${status.white_specialist_score}",
            nullWrapperControl = "Control confirms specialist effect is real",
            statisticalStatus = "${status.level} (${status.confidence})",
            gamesNeeded = status.games_needed_for_95pct,
        )
    }
}
