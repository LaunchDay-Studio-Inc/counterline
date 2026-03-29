#include <jni.h>
#include <android/log.h>

#include <atomic>
#include <mutex>
#include <sstream>
#include <string>
#include <vector>

#include "engine.h"
#include "position.h"
#include "search.h"
#include "types.h"
#include "uci.h"

#define LOG_TAG "StockfishBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

std::mutex g_engine_mutex;
std::unique_ptr<Stockfish::Engine> g_engine;
std::atomic<bool> g_initialized{false};
std::atomic<bool> g_searching{false};

// Captured search results
struct SearchResult {
    std::string bestmove;
    std::string ponder;
    int score_cp = 0;
    int depth = 0;
    std::vector<std::string> pv;
    int mate_in = 0;
    // MultiPV results
    struct PvLine {
        std::string move;
        int score_cp = 0;
        int mate_in = 0;
        int depth = 0;
        std::vector<std::string> pv;
    };
    std::vector<PvLine> multi_pv;
};

SearchResult g_last_result;
std::mutex g_result_mutex;

std::string jstring_to_string(JNIEnv* env, jstring jstr) {
    if (jstr == nullptr) return "";
    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    std::string result(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return result;
}

jstring string_to_jstring(JNIEnv* env, const std::string& str) {
    return env->NewStringUTF(str.c_str());
}

} // anonymous namespace

extern "C" {

JNIEXPORT jboolean JNICALL
Java_dev_counterline_core_engine_StockfishBridge_nativeInit(
    JNIEnv* /* env */, jobject /* thiz */) {
    std::lock_guard<std::mutex> lock(g_engine_mutex);
    if (g_initialized.load()) return JNI_TRUE;

    try {
        g_engine = std::make_unique<Stockfish::Engine>();
        g_initialized.store(true);
        LOGI("Stockfish engine initialized");
        return JNI_TRUE;
    } catch (const std::exception& e) {
        LOGE("Failed to initialize engine: %s", e.what());
        return JNI_FALSE;
    }
}

JNIEXPORT void JNICALL
Java_dev_counterline_core_engine_StockfishBridge_nativeDestroy(
    JNIEnv* /* env */, jobject /* thiz */) {
    std::lock_guard<std::mutex> lock(g_engine_mutex);
    if (g_searching.load()) {
        if (g_engine) g_engine->stop();
        g_engine->wait_for_search_finished();
        g_searching.store(false);
    }
    g_engine.reset();
    g_initialized.store(false);
    LOGI("Stockfish engine destroyed");
}

JNIEXPORT jboolean JNICALL
Java_dev_counterline_core_engine_StockfishBridge_nativeIsReady(
    JNIEnv* /* env */, jobject /* thiz */) {
    return g_initialized.load() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_dev_counterline_core_engine_StockfishBridge_nativeSetOption(
    JNIEnv* env, jobject /* thiz */, jstring name, jstring value) {
    std::lock_guard<std::mutex> lock(g_engine_mutex);
    if (!g_engine) return;

    std::string opt_name = jstring_to_string(env, name);
    std::string opt_value = jstring_to_string(env, value);

    try {
        auto& options = g_engine->get_options();
        if (options.count(opt_name)) {
            options[opt_name] = opt_value;
            LOGI("Set option %s = %s", opt_name.c_str(), opt_value.c_str());

            if (opt_name == "Threads") {
                g_engine->resize_threads();
            } else if (opt_name == "Hash") {
                g_engine->set_tt_size(std::stoi(opt_value));
            }
        }
    } catch (const std::exception& e) {
        LOGE("Failed to set option %s: %s", opt_name.c_str(), e.what());
    }
}

JNIEXPORT jboolean JNICALL
Java_dev_counterline_core_engine_StockfishBridge_nativeSetPosition(
    JNIEnv* env, jobject /* thiz */, jstring fen, jobjectArray moves) {
    std::lock_guard<std::mutex> lock(g_engine_mutex);
    if (!g_engine) return JNI_FALSE;

    std::string fen_str = jstring_to_string(env, fen);
    std::vector<std::string> move_list;

    if (moves != nullptr) {
        int len = env->GetArrayLength(moves);
        for (int i = 0; i < len; i++) {
            auto jmove = (jstring) env->GetObjectArrayElement(moves, i);
            move_list.push_back(jstring_to_string(env, jmove));
            env->DeleteLocalRef(jmove);
        }
    }

    auto err = g_engine->set_position(fen_str, move_list);
    if (err.has_value()) {
        LOGE("Failed to set position: invalid FEN or moves");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_dev_counterline_core_engine_StockfishBridge_nativeGo(
    JNIEnv* env, jobject /* thiz */, jint depth, jint movetime_ms, jint multi_pv) {
    std::lock_guard<std::mutex> lock(g_engine_mutex);
    if (!g_engine) return string_to_jstring(env, "");

    // Set MultiPV if requested
    if (multi_pv > 1) {
        try {
            auto& options = g_engine->get_options();
            if (options.count("MultiPV")) {
                options["MultiPV"] = std::to_string(multi_pv);
            }
        } catch (...) {}
    }

    // Build search limits
    Stockfish::Search::LimitsType limits;
    if (depth > 0) limits.depth = depth;
    if (movetime_ms > 0) limits.movetime = movetime_ms;

    // Clear previous result
    {
        std::lock_guard<std::mutex> rlock(g_result_mutex);
        g_last_result = SearchResult{};
        g_last_result.multi_pv.clear();
    }

    // Set up search callbacks to capture results
    g_engine->set_on_bestmove([](std::string_view bestmove, std::string_view ponder) {
        std::lock_guard<std::mutex> rlock(g_result_mutex);
        g_last_result.bestmove = std::string(bestmove);
        g_last_result.ponder = std::string(ponder);
        g_searching.store(false);
    });

    g_engine->set_on_update_full([multi_pv](const Stockfish::Engine::InfoFull& info, bool) {
        std::lock_guard<std::mutex> rlock(g_result_mutex);
        // For MultiPV, accumulate lines
        if (multi_pv > 1) {
            SearchResult::PvLine line;
            line.depth = info.depth;
            // Extract score and PV from info
            // The score is in the info structure
            if (g_last_result.multi_pv.size() < static_cast<size_t>(multi_pv)) {
                g_last_result.multi_pv.push_back(line);
            }
        }
        g_last_result.depth = info.depth;
    });

    // Start search
    g_searching.store(true);
    g_engine->go(limits);
    g_engine->wait_for_search_finished();
    g_searching.store(false);

    // Reset MultiPV to 1
    if (multi_pv > 1) {
        try {
            auto& options = g_engine->get_options();
            if (options.count("MultiPV")) {
                options["MultiPV"] = "1";
            }
        } catch (...) {}
    }

    // Build result JSON
    std::lock_guard<std::mutex> rlock(g_result_mutex);
    std::ostringstream json;
    json << "{";
    json << "\"bestmove\":\"" << g_last_result.bestmove << "\"";
    json << ",\"ponder\":\"" << g_last_result.ponder << "\"";
    json << ",\"depth\":" << g_last_result.depth;
    json << ",\"score_cp\":" << g_last_result.score_cp;
    json << ",\"mate_in\":" << g_last_result.mate_in;
    json << "}";

    return string_to_jstring(env, json.str());
}

JNIEXPORT void JNICALL
Java_dev_counterline_core_engine_StockfishBridge_nativeStop(
    JNIEnv* /* env */, jobject /* thiz */) {
    std::lock_guard<std::mutex> lock(g_engine_mutex);
    if (g_engine && g_searching.load()) {
        g_engine->stop();
    }
}

} // extern "C"
