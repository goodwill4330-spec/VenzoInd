package com.example.utils

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiagnosticReport(
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val firebaseAppInitialized: Boolean = false,
    val firebaseAppName: String = "None",
    val projectId: String = "Unknown",
    val authStatus: String = "UNKNOWN",
    val authUid: String? = null,
    val authPhone: String? = null,
    val authIsAnonymous: Boolean = false,
    val authTokenValid: Boolean = false,
    val firestoreInitialized: Boolean = false,
    val firestorePingSuccess: Boolean = false,
    val firestoreLatencyMs: Long = -1,
    val globalMessagesAccess: String = "PENDING",
    val activeCallsAccess: String = "PENDING",
    val usersAccess: String = "PENDING",
    val activeListenersCount: Int = 0,
    val errorSummary: List<String> = emptyList(),
    val rawLogText: String = ""
)

object FirebaseDiagnostics {

    private const val TAG = "FirebaseDiagnostics"

    private val _lastReport = MutableStateFlow<DiagnosticReport?>(null)
    val lastReport: StateFlow<DiagnosticReport?> = _lastReport.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    /**
     * Executes a comprehensive diagnostic test of Firebase App, Firebase Auth,
     * Firestore connectivity, and Realtime Signaling for Calls and Messages.
     *
     * Prints full formatted diagnostic output to Logcat / Console.
     */
    fun runDiagnostics(
        context: Context,
        onComplete: ((DiagnosticReport) -> Unit)? = null
    ) {
        if (_isRunning.value) {
            Log.w(TAG, "Diagnostics is already running. Please wait for completion.")
            return
        }

        _isRunning.value = true
        val logBuffer = StringBuilder()

        fun logLine(level: Int, tag: String, message: String) {
            when (level) {
                Log.ERROR -> Log.e(tag, message)
                Log.WARN -> Log.w(tag, message)
                else -> Log.i(tag, message)
            }
            println("[$tag] $message")
            logBuffer.append("[$tag] $message\n")
        }

        CoroutineScope(Dispatchers.IO).launch {
            val errors = mutableListOf<String>()
            var appInit = false
            var appName = "None"
            var projId = "Unknown"
            var authState = "NOT_AUTHENTICATED"
            var uid: String? = null
            var phone: String? = null
            var isAnon = false
            var tokenValid = false
            var firestoreInit = false
            var firestorePing = false
            var pingLatency: Long = -1
            var globalMsgStatus = "UNKNOWN"
            var activeCallsStatus = "UNKNOWN"
            var usersStatus = "UNKNOWN"

            logLine(Log.INFO, TAG, "=================================================================")
            logLine(Log.INFO, TAG, " 🔥 STARTING VENZOIND FIREBASE & FIRESTORE DIAGNOSTIC SUITE 🔥 ")
            logLine(Log.INFO, TAG, "=================================================================")
            logLine(Log.INFO, TAG, "Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())}")

            // -------------------------------------------------------------
            // STEP 1: Firebase Core App Diagnostic
            // -------------------------------------------------------------
            logLine(Log.INFO, TAG, "\n--- [1/5] FIREBASE CORE INITIALIZATION CHECK ---")
            try {
                val apps = FirebaseApp.getApps(context)
                if (apps.isEmpty()) {
                    logLine(Log.WARN, TAG, "[WARN] No FirebaseApp found. Attempting initializeApp()...")
                    val app = FirebaseApp.initializeApp(context)
                    if (app != null) {
                        appInit = true
                        appName = app.name
                        projId = app.options.projectId ?: "Unknown"
                        logLine(Log.INFO, TAG, "[PASS] FirebaseApp initialized successfully: name='$appName', projectId='$projId'")
                    } else {
                        val err = "Failed to initialize FirebaseApp instance (returned null)."
                        errors.add(err)
                        logLine(Log.ERROR, TAG, "[FAIL] $err")
                    }
                } else {
                    val defaultApp = FirebaseApp.getInstance()
                    appInit = true
                    appName = defaultApp.name
                    projId = defaultApp.options.projectId ?: "Unknown"
                    logLine(Log.INFO, TAG, "[PASS] FirebaseApp instance found (${apps.size} active app(s)): name='$appName', projectId='$projId'")
                    logLine(Log.INFO, TAG, "       ApplicationId: ${defaultApp.options.applicationId}")
                    logLine(Log.INFO, TAG, "       GcmSenderId:   ${defaultApp.options.gcmSenderId}")
                }
            } catch (e: Exception) {
                val err = "FirebaseCore error: ${e.message}"
                errors.add(err)
                logLine(Log.ERROR, TAG, "[FAIL] $err")
            }

            // -------------------------------------------------------------
            // STEP 2: Firebase Auth Diagnostic
            // -------------------------------------------------------------
            logLine(Log.INFO, TAG, "\n--- [2/5] FIREBASE AUTHENTICATION STATUS CHECK ---")
            try {
                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    authState = "AUTHENTICATED"
                    uid = currentUser.uid
                    phone = currentUser.phoneNumber
                    isAnon = currentUser.isAnonymous
                    logLine(Log.INFO, TAG, "[PASS] Active Firebase User session found:")
                    logLine(Log.INFO, TAG, "       UID:          $uid")
                    logLine(Log.INFO, TAG, "       Phone Number: ${phone ?: "None"}")
                    logLine(Log.INFO, TAG, "       Email:        ${currentUser.email ?: "None"}")
                    logLine(Log.INFO, TAG, "       Is Anonymous: $isAnon")
                    logLine(Log.INFO, TAG, "       Provider ID:  ${currentUser.providerId}")

                    // Verify token retrieval
                    try {
                        val tokenResult = withTimeoutOrNull(5000) {
                            currentUser.getIdToken(false).await()
                        }
                        if (tokenResult != null && !tokenResult.token.isNullOrBlank()) {
                            tokenValid = true
                            logLine(Log.INFO, TAG, "[PASS] Auth Token retrieved successfully (expires in: ${tokenResult.expirationTimestamp})")
                        } else {
                            logLine(Log.WARN, TAG, "[WARN] Auth Token fetch timed out or returned empty token.")
                        }
                    } catch (e: Exception) {
                        logLine(Log.WARN, TAG, "[WARN] Could not refresh Auth Token: ${e.message}")
                    }
                } else {
                    authState = "SIGNED_OUT"
                    logLine(Log.WARN, TAG, "[INFO] No Firebase Auth user is currently signed in (Guest / Local Sovereign mode).")
                    logLine(Log.INFO, TAG, "       Note: If Firestore security rules require 'request.auth != null', unauthenticated calls/messages will fail unless rules allow public read/write or anonymous auth is used.")
                }
            } catch (e: Exception) {
                val err = "FirebaseAuth check failed: ${e.message}"
                errors.add(err)
                logLine(Log.ERROR, TAG, "[FAIL] $err")
            }

            // -------------------------------------------------------------
            // STEP 3: Firestore Connection & Ping Diagnostic
            // -------------------------------------------------------------
            logLine(Log.INFO, TAG, "\n--- [3/5] FIRESTORE CONNECTION & READ/WRITE PING ---")
            var firestore: FirebaseFirestore? = null
            try {
                firestore = FirebaseFirestore.getInstance()
                firestoreInit = true
                logLine(Log.INFO, TAG, "[PASS] FirebaseFirestore instance obtained.")
                logLine(Log.INFO, TAG, "       Persistence Enabled: ${firestore.firestoreSettings.isPersistenceEnabled}")
                logLine(Log.INFO, TAG, "       Host: ${firestore.firestoreSettings.host}")
                logLine(Log.INFO, TAG, "       SSL Enabled: ${firestore.firestoreSettings.isSslEnabled}")

                // Perform real-time ping (Write + Read test)
                val startTime = System.currentTimeMillis()
                val pingData = hashMapOf(
                    "ping" to true,
                    "timestamp" to startTime,
                    "sender" to "diagnostic_runner",
                    "deviceModel" to android.os.Build.MODEL,
                    "androidVersion" to android.os.Build.VERSION.RELEASE
                )

                val pingDocRef = firestore.collection("_diagnostics").document("ping_test")
                
                val writeSuccess = withTimeoutOrNull(7000) {
                    try {
                        pingDocRef.set(pingData, SetOptions.merge()).await()
                        true
                    } catch (e: Exception) {
                        logLine(Log.WARN, TAG, "[WARN] Firestore ping write error: ${e.message}")
                        false
                    }
                } ?: false

                if (writeSuccess) {
                    val readSuccess = withTimeoutOrNull(5000) {
                        try {
                            val snapshot = pingDocRef.get().await()
                            snapshot.exists()
                        } catch (e: Exception) {
                            logLine(Log.WARN, TAG, "[WARN] Firestore ping read error: ${e.message}")
                            false
                        }
                    } ?: false

                    val endTime = System.currentTimeMillis()
                    pingLatency = endTime - startTime

                    if (readSuccess) {
                        firestorePing = true
                        logLine(Log.INFO, TAG, "[PASS] Firestore Ping SUCCESS (Roundtrip Latency: ${pingLatency}ms)")
                    } else {
                        val err = "Firestore ping write succeeded but read failed."
                        errors.add(err)
                        logLine(Log.WARN, TAG, "[WARN] $err")
                    }
                } else {
                    val err = "Firestore ping write timed out or failed (Check internet / Firestore rules / App Check)."
                    errors.add(err)
                    logLine(Log.ERROR, TAG, "[FAIL] $err")
                }
            } catch (e: Exception) {
                val err = "Firestore ping exception: ${e.message}"
                errors.add(err)
                logLine(Log.ERROR, TAG, "[FAIL] $err")
            }

            // -------------------------------------------------------------
            // STEP 4: Calling & Messaging Collection Access Checks
            // -------------------------------------------------------------
            logLine(Log.INFO, TAG, "\n--- [4/5] CALLING & MESSAGING PIPELINE ACCESSIBILITY ---")
            if (firestore != null) {
                // Test 'global_messages' collection
                try {
                    val msgQuery = withTimeoutOrNull(5000) {
                        firestore.collection("global_messages").limit(5).get().await()
                    }
                    if (msgQuery != null) {
                        globalMsgStatus = "ACCESSIBLE (${msgQuery.size()} docs sample)"
                        logLine(Log.INFO, TAG, "[PASS] 'global_messages' collection is ACCESSIBLE (Found ${msgQuery.size()} sample messages)")
                    } else {
                        globalMsgStatus = "TIMEOUT"
                        logLine(Log.WARN, TAG, "[WARN] 'global_messages' collection query timed out.")
                    }
                } catch (e: FirebaseFirestoreException) {
                    globalMsgStatus = "PERMISSION_DENIED (${e.code})"
                    val err = "'global_messages' access denied (${e.code}): ${e.message}"
                    errors.add(err)
                    logLine(Log.ERROR, TAG, "[FAIL] $err")
                } catch (e: Exception) {
                    globalMsgStatus = "ERROR (${e.message})"
                    logLine(Log.WARN, TAG, "[WARN] 'global_messages' query error: ${e.message}")
                }

                // Test 'active_calls' collection
                try {
                    val callsQuery = withTimeoutOrNull(5000) {
                        firestore.collection("active_calls").limit(5).get().await()
                    }
                    if (callsQuery != null) {
                        activeCallsStatus = "ACCESSIBLE (${callsQuery.size()} docs sample)"
                        logLine(Log.INFO, TAG, "[PASS] 'active_calls' collection is ACCESSIBLE (Signaling channel online)")
                    } else {
                        activeCallsStatus = "TIMEOUT"
                        logLine(Log.WARN, TAG, "[WARN] 'active_calls' collection query timed out.")
                    }
                } catch (e: FirebaseFirestoreException) {
                    activeCallsStatus = "PERMISSION_DENIED (${e.code})"
                    val err = "'active_calls' access denied (${e.code}): ${e.message}"
                    errors.add(err)
                    logLine(Log.ERROR, TAG, "[FAIL] $err")
                } catch (e: Exception) {
                    activeCallsStatus = "ERROR (${e.message})"
                    logLine(Log.WARN, TAG, "[WARN] 'active_calls' query error: ${e.message}")
                }

                // Test 'users' collection (Peer Discovery)
                try {
                    val usersQuery = withTimeoutOrNull(5000) {
                        firestore.collection("users").limit(10).get().await()
                    }
                    if (usersQuery != null) {
                        usersStatus = "ACCESSIBLE (${usersQuery.size()} peers registered)"
                        logLine(Log.INFO, TAG, "[PASS] 'users' collection is ACCESSIBLE (${usersQuery.size()} peer users registered)")
                    } else {
                        usersStatus = "TIMEOUT"
                        logLine(Log.WARN, TAG, "[WARN] 'users' collection query timed out.")
                    }
                } catch (e: FirebaseFirestoreException) {
                    usersStatus = "PERMISSION_DENIED (${e.code})"
                    val err = "'users' collection access denied (${e.code}): ${e.message}"
                    errors.add(err)
                    logLine(Log.ERROR, TAG, "[FAIL] $err")
                } catch (e: Exception) {
                    usersStatus = "ERROR (${e.message})"
                    logLine(Log.WARN, TAG, "[WARN] 'users' query error: ${e.message}")
                }
            }

            // -------------------------------------------------------------
            // STEP 5: Root-Cause Analysis & Summary
            // -------------------------------------------------------------
            logLine(Log.INFO, TAG, "\n--- [5/5] DIAGNOSTIC SUMMARY & ROOT-CAUSE ANALYSIS ---")
            if (errors.isEmpty() && firestorePing) {
                logLine(Log.INFO, TAG, "✅ ALL SYSTEMS OPERATIONAL: Firebase Auth & Firestore are connected and fully functional for Calling & Messaging.")
            } else {
                logLine(Log.WARN, TAG, "⚠️ IDENTIFIED POTENTIAL ISSUES:")
                if (!firestorePing) {
                    logLine(Log.WARN, TAG, " • Firestore could not write/read ping. Potential causes: Offline network, Firestore Rules blocking unauthenticated writes, or Firebase App Check blocking requests.")
                }
                if (globalMsgStatus.contains("PERMISSION_DENIED") || activeCallsStatus.contains("PERMISSION_DENIED")) {
                    logLine(Log.WARN, TAG, " • FIRESTORE RULES: Read/write permission denied on messaging or calls. Ensure Firestore security rules allow 'allow read, write: if true;' for dev or 'request.auth != null' with authenticated users.")
                }
                if (authState == "SIGNED_OUT") {
                    logLine(Log.WARN, TAG, " • AUTH STATUS: Current user is signed out in Firebase Auth. Messages/Calls will use device ID fallback or local Room caching.")
                }
                errors.forEach { logLine(Log.ERROR, TAG, "   -> $it") }
            }
            logLine(Log.INFO, TAG, "=================================================================\n")

            val report = DiagnosticReport(
                timestamp = System.currentTimeMillis(),
                firebaseAppInitialized = appInit,
                firebaseAppName = appName,
                projectId = projId,
                authStatus = authState,
                authUid = uid,
                authPhone = phone,
                authIsAnonymous = isAnon,
                authTokenValid = tokenValid,
                firestoreInitialized = firestoreInit,
                firestorePingSuccess = firestorePing,
                firestoreLatencyMs = pingLatency,
                globalMessagesAccess = globalMsgStatus,
                activeCallsAccess = activeCallsStatus,
                usersAccess = usersStatus,
                errorSummary = errors,
                rawLogText = logBuffer.toString()
            )

            _lastReport.value = report
            _isRunning.value = false
            onComplete?.invoke(report)
        }
    }
}
