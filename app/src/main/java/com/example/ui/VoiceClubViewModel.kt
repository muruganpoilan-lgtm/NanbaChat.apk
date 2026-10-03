package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.GeminiApiClient
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerationConfig
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.example.data.database.VoiceClubDatabase
import com.example.data.model.CallLog
import com.example.data.model.CallLogWithPartner
import com.example.data.model.Language
import com.example.data.model.Partner
import com.example.data.model.Transaction
import com.example.data.model.Wallet
import com.example.data.repository.VoiceClubRepository
import com.example.ui.components.AppMode
import com.example.voice.VoiceAudioManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil

enum class UserScreenTab {
    DISCOVER,
    WALLET,
    HISTORY
}

sealed class MatchState {
    object Idle : MatchState()
    object Searching : MatchState()
    data class Found(val partner: Partner) : MatchState()
    data class NoMatch(val reason: String) : MatchState()
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "partner"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ActiveCallData(
    val callLogId: String,
    val partner: Partner,
    val isIncomingForHost: Boolean = false,
    val startTimeMs: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val currentCost: Double = 0.0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val messages: List<ChatMessage> = emptyList(),
    val isAiResponding: Boolean = false
)

data class CallSummaryData(
    val partnerName: String,
    val durationSeconds: Int,
    val totalCost: Double,
    val perMinuteRate: Double
)

class VoiceClubViewModel(application: Application) : AndroidViewModel(application) {

    private val database = VoiceClubDatabase.getDatabase(application, viewModelScope)
    private val repository = VoiceClubRepository(database)
    val audioManager = VoiceAudioManager(application)

    // Current dual-app mode: USER caller or PARTNER host
    private val _appMode = MutableStateFlow(AppMode.USER)
    val appMode: StateFlow<AppMode> = _appMode.asStateFlow()

    private val _userTab = MutableStateFlow(UserScreenTab.DISCOVER)
    val userTab: StateFlow<UserScreenTab> = _userTab.asStateFlow()

    // Selected language filter/preference (default Tamil)
    private val _selectedLanguage = MutableStateFlow("Tamil")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    // Matchmaking state for SQL query
    private val _matchState = MutableStateFlow<MatchState>(MatchState.Idle)
    val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    // Active call session (null if not in call)
    private val _activeCall = MutableStateFlow<ActiveCallData?>(null)
    val activeCall: StateFlow<ActiveCallData?> = _activeCall.asStateFlow()

    // Post-call summary sheet
    private val _callSummary = MutableStateFlow<CallSummaryData?>(null)
    val callSummary: StateFlow<CallSummaryData?> = _callSummary.asStateFlow()

    // Incoming call simulator alert for Host
    private val _incomingHostCall = MutableStateFlow<Partner?>(null)
    val incomingHostCall: StateFlow<Partner?> = _incomingHostCall.asStateFlow()

    private var callTimerJob: Job? = null

    // Room DB Flows
    val partners: StateFlow<List<Partner>> = repository.allPartners
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val languages: StateFlow<List<Language>> = repository.allLanguages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userWallet: StateFlow<Wallet?> = repository.getUserWallet()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val partnerWallet: StateFlow<Wallet?> = repository.getPartnerWallet()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userTransactions: StateFlow<List<Transaction>> = repository.getTransactions(VoiceClubDatabase.DEFAULT_USER_WALLET_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userCallLogs: StateFlow<List<CallLogWithPartner>> = repository.getUserCallLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val partnerProfile: StateFlow<Partner?> = repository.getPartnerProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val partnerLanguages: StateFlow<List<Language>> = repository.getLanguagesForPartner(VoiceClubDatabase.SELF_PARTNER_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setAppMode(mode: AppMode) {
        _appMode.value = mode
    }

    fun setUserTab(tab: UserScreenTab) {
        _userTab.value = tab
    }

    fun selectLanguage(lang: String) {
        _selectedLanguage.value = lang
    }

    /**
     * Executes the exact SQL Matchmaking Query requested by the user:
     * Filters online partners speaking the selected language,
     * excludes active call locks, sorts by rating DESC and RANDOM(), LIMIT 1.
     */
    fun startMatchmaking() {
        viewModelScope.launch {
            _matchState.value = MatchState.Searching
            delay(1200) // Radar search animation delay

            val matchedPartner = repository.matchmakePartner(_selectedLanguage.value)
            if (matchedPartner != null) {
                _matchState.value = MatchState.Found(matchedPartner)
            } else {
                _matchState.value = MatchState.NoMatch(
                    "No online hosts speaking ${_selectedLanguage.value} currently available. Try another language or check back in a moment!"
                )
            }
        }
    }

    fun clearMatchState() {
        _matchState.value = MatchState.Idle
    }

    /**
     * Starts an anonymous voice call with a partner
     */
    fun initiateCall(partner: Partner) {
        val currentBalance = userWallet.value?.balance ?: 0.0
        if (currentBalance < partner.perMinuteRate) {
            _matchState.value = MatchState.NoMatch(
                "Insufficient credits. You need at least $${String.format("%.2f", partner.perMinuteRate)} for 1 minute. Please top up your wallet!"
            )
            _userTab.value = UserScreenTab.WALLET
            return
        }

        viewModelScope.launch {
            val callLog = repository.startCall(VoiceClubDatabase.DEFAULT_USER_ID, partner.id)
            _matchState.value = MatchState.Idle

            val initialCall = ActiveCallData(
                callLogId = callLog.id,
                partner = partner,
                isIncomingForHost = false,
                startTimeMs = System.currentTimeMillis(),
                durationSeconds = 0,
                currentCost = partner.perMinuteRate,
                messages = listOf(
                    ChatMessage(
                        sender = "partner",
                        text = "Vanakkam! I'm ${partner.name}. Thanks for connecting on NanbaChat. How are you doing today?"
                    )
                )
            )
            _activeCall.value = initialCall

            // Speak greeting through TTS
            audioManager.speak(
                "Hello! I'm ${partner.name}. Thanks for connecting on NanbaChat. How are you doing today?",
                _selectedLanguage.value
            )

            startCallTimer(partner.perMinuteRate)
        }
    }

    private fun startCallTimer(perMinuteRate: Double) {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            var elapsed = 0
            while (isActive) {
                delay(1000)
                elapsed++
                val billedMinutes = ceil(elapsed / 60.0).toInt().coerceAtLeast(1)
                val cost = billedMinutes * perMinuteRate

                _activeCall.value = _activeCall.value?.copy(
                    durationSeconds = elapsed,
                    currentCost = cost
                )

                // Auto disconnect if wallet balance is exceeded
                val currentBalance = userWallet.value?.balance ?: 0.0
                if (cost > currentBalance) {
                    endCall()
                    break
                }
            }
        }
    }

    fun toggleMute() {
        _activeCall.value = _activeCall.value?.let { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleSpeaker() {
        _activeCall.value = _activeCall.value?.let { it.copy(isSpeakerOn = !it.isSpeakerOn) }
    }

    /**
     * Send message / speech prompt to the host during the live call,
     * answered by Gemini model (gemini-3.8-flash) in character!
     */
    fun sendMessageInCall(userText: String) {
        val call = _activeCall.value ?: return
        if (userText.isBlank()) return

        val userMessage = ChatMessage(sender = "user", text = userText)
        val updatedMessages = call.messages + userMessage
        _activeCall.value = call.copy(messages = updatedMessages, isAiResponding = true)

        viewModelScope.launch {
            val partner = call.partner
            val responseText = queryGeminiForHost(partner, updatedMessages, _selectedLanguage.value)

            val partnerMessage = ChatMessage(sender = "partner", text = responseText)
            _activeCall.value = _activeCall.value?.copy(
                messages = updatedMessages + partnerMessage,
                isAiResponding = false
            )

            // Speak aloud in voice
            if (_activeCall.value?.isSpeakerOn != false) {
                audioManager.speak(responseText, _selectedLanguage.value)
            }
        }
    }

    private suspend fun queryGeminiForHost(
        partner: Partner,
        history: List<ChatMessage>,
        languageName: String
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Intelligent fallback responses based on host bio and language
            return when {
                languageName.equals("Tamil", ignoreCase = true) ->
                    "Romba nalladhu! That's wonderful to hear. As a NanbaChat host, I love discussing this. Please tell me more about your thoughts!"
                languageName.equals("Hindi", ignoreCase = true) ->
                    "Bohat accha laga sunkar! I am glad we are having this conversation. What else would you like to practice today?"
                else ->
                    "That's so interesting! I really enjoy conversational sessions here on NanbaChat. How does that connect with your day?"
            }
        }

        try {
            val systemInstructionText = """
                You are ${partner.name}, an engaging and authentic host on NanbaChat.
                Bio: ${partner.bio}
                Language of conversation: $languageName.
                Style: Natural, warm, conversational audio call response. Keep it brief (1 to 3 spoken sentences) as if talking on a phone call.
                Respond naturally in or appropriate to the language ($languageName), maintaining your warm host personality.
            """.trimIndent()

            val contents = history.takeLast(6).map { msg ->
                GeminiContent(
                    role = if (msg.sender == "user") "user" else "model",
                    parts = listOf(GeminiPart(text = msg.text))
                )
            }

            val request = GeminiRequest(
                contents = contents,
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemInstructionText))
                ),
                generationConfig = GeminiGenerationConfig(temperature = 0.85f)
            )

            // Using models/gemini-3.8-flash as requested in prompt instructions
            val response = GeminiApiClient.apiService.generateContent(
                model = "gemini-3.8-flash",
                apiKey = apiKey,
                request = request
            )

            return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "I'm having a great time talking with you! What should we chat about next?"
        } catch (e: Exception) {
            return "That's wonderful! I really enjoy having this audio chat with you on NanbaChat."
        }
    }

    /**
     * Ends the call and performs ACID ledger billing
     */
    fun endCall() {
        val call = _activeCall.value ?: return
        callTimerJob?.cancel()
        callTimerJob = null
        audioManager.stopSpeaking()

        val duration = call.durationSeconds
        val rate = call.partner.perMinuteRate

        viewModelScope.launch {
            val finalCost = repository.completeCallBilling(call.callLogId, duration, rate)
            _callSummary.value = CallSummaryData(
                partnerName = call.partner.name,
                durationSeconds = duration,
                totalCost = finalCost,
                perMinuteRate = rate
            )
            _activeCall.value = null
        }
    }

    fun dismissCallSummary() {
        _callSummary.value = null
    }

    /**
     * Wallet top-up action
     */
    fun topUpUserWallet(amount: Double) {
        viewModelScope.launch {
            repository.topUpWallet(VoiceClubDatabase.DEFAULT_USER_ID, amount)
        }
    }

    // ==========================================
    // PARTNER / HOST CONTROLS
    // ==========================================

    fun updateHostStatus(status: String) {
        viewModelScope.launch {
            repository.updatePartnerStatus(VoiceClubDatabase.SELF_PARTNER_ID, status)
        }
    }

    fun updateHostProfile(bio: String, perMinuteRate: Double) {
        viewModelScope.launch {
            repository.updatePartnerProfile(VoiceClubDatabase.SELF_PARTNER_ID, bio, perMinuteRate)
        }
    }

    fun toggleHostLanguage(languageId: Long) {
        val currentLangs = partnerLanguages.value.map { it.id }.toMutableSet()
        if (currentLangs.contains(languageId)) {
            if (currentLangs.size > 1) {
                currentLangs.remove(languageId)
            }
        } else {
            currentLangs.add(languageId)
        }
        viewModelScope.launch {
            repository.updatePartnerLanguages(VoiceClubDatabase.SELF_PARTNER_ID, currentLangs.toList())
        }
    }

    fun simulateIncomingCallForHost() {
        viewModelScope.launch {
            val dummyCaller = Partner(
                id = "user-caller-sim",
                phoneNumber = "+91 98840 99887",
                name = "Anonymous Caller (Chennai)",
                bio = "Looking to practice spoken Tamil and discuss technology career path.",
                status = "online",
                perMinuteRate = partnerProfile.value?.perMinuteRate ?: 0.45,
                rating = 5.0
            )
            _incomingHostCall.value = dummyCaller
        }
    }

    fun acceptIncomingCall() {
        val caller = _incomingHostCall.value ?: return
        _incomingHostCall.value = null
        viewModelScope.launch {
            val callLog = repository.startCall(caller.id, VoiceClubDatabase.SELF_PARTNER_ID)
            val callData = ActiveCallData(
                callLogId = callLog.id,
                partner = caller,
                isIncomingForHost = true,
                startTimeMs = System.currentTimeMillis(),
                durationSeconds = 0,
                currentCost = 0.0,
                messages = listOf(
                    ChatMessage(
                        sender = "user",
                        text = "Hello Host! I've connected to you via NanbaChat. Thanks for accepting!"
                    )
                )
            )
            _activeCall.value = callData
            startCallTimer(caller.perMinuteRate)
        }
    }

    fun rejectIncomingCall() {
        _incomingHostCall.value = null
    }

    fun requestPayout(amount: Double) {
        viewModelScope.launch {
            val wallet = partnerWallet.value ?: return@launch
            if (amount <= wallet.balance && amount > 0) {
                // Deduct from partner wallet and record payout transaction
                database.walletDao().updateWallet(
                    wallet.copy(balance = wallet.balance - amount, updatedAt = System.currentTimeMillis())
                )
                database.walletDao().insertTransaction(
                    Transaction(
                        id = java.util.UUID.randomUUID().toString(),
                        walletId = wallet.id,
                        amount = amount,
                        type = "debit",
                        referenceId = "PAYOUT_" + System.currentTimeMillis(),
                        description = "Instant Payout to Bank/UPI (-$${String.format("%.2f", amount)})"
                    )
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        callTimerJob?.cancel()
        audioManager.shutdown()
    }
}
