package com.dwarshb.appfunctions

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * Service endpoint for Android AppFunctions integration.
 * Dispatches agent execution calls from system assistants and Google Gemini to AgentCartAppFunctions.
 */
class AgentCartAppFunctionService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "AgentCartAppFunctionService onBind action=${intent?.action}")
        // Return null or AppFunction binder if framework provides platform IPC
        return null
    }

    companion object {
        private const val TAG = "AppFunctionService"
    }
}
