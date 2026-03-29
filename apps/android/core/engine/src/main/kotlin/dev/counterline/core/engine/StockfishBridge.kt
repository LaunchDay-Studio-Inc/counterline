package dev.counterline.core.engine

import android.util.Log

/**
 * JNI bridge to the native Stockfish library.
 * This class is a thin wrapper; all engine logic lives in C++.
 * Thread safety is handled on the native side.
 */
internal class StockfishBridge {

    companion object {
        private const val TAG = "StockfishBridge"

        init {
            try {
                System.loadLibrary("stockfish_bridge")
                Log.i(TAG, "Native library loaded")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library", e)
            }
        }
    }

    /** Initialize the engine. Returns true on success. */
    external fun nativeInit(): Boolean

    /** Destroy the engine and free resources. */
    external fun nativeDestroy()

    /** Check if the engine is initialized and ready. */
    external fun nativeIsReady(): Boolean

    /** Set a UCI option. */
    external fun nativeSetOption(name: String, value: String)

    /** Set position from FEN with optional moves. Returns true on success. */
    external fun nativeSetPosition(fen: String, moves: Array<String>?): Boolean

    /**
     * Start a search and wait for it to complete.
     * Returns JSON with bestmove, score, depth.
     * @param depth Maximum search depth (0 = no limit)
     * @param movetimeMs Maximum time in milliseconds (0 = no limit)
     * @param multiPv Number of PV lines to return (1 = single best)
     */
    external fun nativeGo(depth: Int, movetimeMs: Int, multiPv: Int): String

    /** Stop a running search. */
    external fun nativeStop()
}
