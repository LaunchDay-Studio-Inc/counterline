package dev.counterline.core.engine

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lifecycle-aware engine session manager.
 *
 * Attach to an Activity or Fragment lifecycle to automatically:
 * - Cancel analysis when the user leaves the screen (onStop)
 * - Destroy the engine session when the hosting component is destroyed
 * - Prevent analysis from running on screens that don't need it
 *
 * This ensures:
 * - No heavy analysis on every screen
 * - Background work cancelled when user leaves
 * - Stable offline behavior (engine is fully local)
 * - Acceptable battery usage (single-thread, bounded depth)
 */
@Singleton
class EngineSessionManager @Inject constructor(
    private val engine: StockfishEngine,
) : DefaultLifecycleObserver {

    private var scope: CoroutineScope? = null
    private var activeScreen: String? = null

    /**
     * Request engine availability for a specific screen.
     * The engine starts lazily — only when actually needed.
     */
    fun requestEngine(screenTag: String) {
        if (scope == null) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }
        activeScreen = screenTag
        scope?.launch {
            if (!engine.isReady()) {
                engine.startSession()
            }
        }
    }

    /**
     * Release engine from a specific screen.
     * If the screen releasing is the active screen, analysis is cancelled.
     */
    fun releaseEngine(screenTag: String) {
        if (activeScreen == screenTag) {
            engine.cancelAnalysis()
            activeScreen = null
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        // Cancel any running analysis when screen goes to background
        engine.cancelAnalysis()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        // Full cleanup when the activity is destroyed
        scope?.launch {
            engine.stopSession()
        }
        scope?.cancel()
        scope = null
        activeScreen = null
    }

    /** Check if the engine is currently available. */
    fun isAvailable(): Boolean = engine.isReady()
}
