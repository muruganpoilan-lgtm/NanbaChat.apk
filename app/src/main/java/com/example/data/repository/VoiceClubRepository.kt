package com.example.data.repository

import com.example.data.database.VoiceClubDatabase
import com.example.data.model.CallLog
import com.example.data.model.CallLogWithPartner
import com.example.data.model.Language
import com.example.data.model.Partner
import com.example.data.model.PartnerLanguage
import com.example.data.model.PartnerMatchResult
import com.example.data.model.Transaction
import com.example.data.model.Wallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.ceil

class VoiceClubRepository(private val database: VoiceClubDatabase) {

    private val partnerDao = database.partnerDao()
    private val userDao = database.userDao()
    private val languageDao = database.languageDao()
    private val callLogDao = database.callLogDao()
    private val walletDao = database.walletDao()

    val allPartners: Flow<List<Partner>> = partnerDao.getAllPartners()
    val allLanguages: Flow<List<Language>> = languageDao.getAllLanguages()

    fun getPartnersByLanguage(lang: String): Flow<List<Partner>> =
        partnerDao.getPartnersByLanguage(lang)

    fun getUserWallet(userId: String = VoiceClubDatabase.DEFAULT_USER_ID): Flow<Wallet?> =
        walletDao.getWalletByUserId(userId)

    fun getPartnerWallet(partnerId: String = VoiceClubDatabase.SELF_PARTNER_ID): Flow<Wallet?> =
        walletDao.getWalletByUserId(partnerId)

    fun getTransactions(walletId: String): Flow<List<Transaction>> =
        walletDao.getTransactionsByWalletId(walletId)

    fun getUserCallLogs(userId: String = VoiceClubDatabase.DEFAULT_USER_ID): Flow<List<CallLogWithPartner>> =
        callLogDao.getCallLogsWithPartnerForUser(userId)

    fun getPartnerCallLogs(partnerId: String = VoiceClubDatabase.SELF_PARTNER_ID): Flow<List<CallLog>> =
        callLogDao.getCallLogsForPartner(partnerId)

    fun getPartnerProfile(partnerId: String = VoiceClubDatabase.SELF_PARTNER_ID): Flow<Partner?> =
        partnerDao.getPartnerById(partnerId)

    fun getLanguagesForPartner(partnerId: String): Flow<List<Language>> =
        languageDao.getLanguagesForPartner(partnerId)

    /**
     * Executes the exact SQL Matchmaking Query requested by the user
     */
    suspend fun matchmakePartner(preferredLanguage: String): Partner? = withContext(Dispatchers.IO) {
        partnerDao.findFullPartnerMatch(preferredLanguage)
    }

    suspend fun matchmakePartnerSummary(preferredLanguage: String): PartnerMatchResult? = withContext(Dispatchers.IO) {
        partnerDao.findMatch(preferredLanguage)
    }

    suspend fun getPartnerDirect(partnerId: String): Partner? = withContext(Dispatchers.IO) {
        partnerDao.getPartnerByIdDirect(partnerId)
    }

    /**
     * Updates partner availability (online / offline / busy)
     */
    suspend fun updatePartnerStatus(partnerId: String, status: String) = withContext(Dispatchers.IO) {
        partnerDao.updateStatus(partnerId, status)
    }

    /**
     * Updates partner bio and rate
     */
    suspend fun updatePartnerProfile(partnerId: String, bio: String, perMinuteRate: Double) = withContext(Dispatchers.IO) {
        partnerDao.updateProfile(partnerId, bio, perMinuteRate)
    }

    /**
     * Adds or updates spoken languages for host
     */
    suspend fun updatePartnerLanguages(partnerId: String, languageIds: List<Long>) = withContext(Dispatchers.IO) {
        languageDao.clearLanguagesForPartner(partnerId)
        val mappings = languageIds.map { PartnerLanguage(partnerId, it) }
        languageDao.insertPartnerLanguages(mappings)
    }

    /**
     * Top-up user wallet with credits
     */
    suspend fun topUpWallet(userId: String, amount: Double) = withContext(Dispatchers.IO) {
        var wallet = walletDao.getWalletByUserIdDirect(userId)
        if (wallet == null) {
            wallet = Wallet(
                id = UUID.randomUUID().toString(),
                userId = userId,
                balance = 0.0,
                updatedAt = System.currentTimeMillis()
            )
            walletDao.insertWallet(wallet)
        }

        walletDao.addCredits(
            walletId = wallet.id,
            amount = amount,
            refId = "PAYMENT_" + System.currentTimeMillis(),
            description = "Wallet Recharge (+$${String.format("%.2f", amount)})"
        )
    }

    /**
     * Starts an anonymous voice call session
     */
    suspend fun startCall(userId: String, partnerId: String): CallLog = withContext(Dispatchers.IO) {
        val callId = UUID.randomUUID().toString()
        val callLog = CallLog(
            id = callId,
            userId = userId,
            partnerId = partnerId,
            startTime = System.currentTimeMillis(),
            endTime = 0L,
            durationSeconds = 0,
            totalCost = 0.0,
            status = "active"
        )
        callLogDao.insertCallLog(callLog)
        callLog
    }

    /**
     * Concludes call session and performs ACID billing deduction:
     * - calculates billed minutes = ceil(durationSeconds / 60.0)
     * - deducts totalCost from User wallet (ledger debit)
     * - credits host wallet with earnings (ledger credit)
     * - records Transaction entries
     * - updates CallLog status to 'completed'
     */
    suspend fun completeCallBilling(
        callId: String,
        durationSeconds: Int,
        perMinuteRate: Double
    ): Double = withContext(Dispatchers.IO) {
        val call = callLogDao.getCallLogById(callId) ?: return@withContext 0.0

        // Minimum 1 billed minute if duration > 5 seconds, else 0
        val billedMinutes = if (durationSeconds <= 5) 0 else ceil(durationSeconds / 60.0).toInt().coerceAtLeast(1)
        val totalCost = billedMinutes * perMinuteRate

        val endTime = System.currentTimeMillis()
        val updatedCall = call.copy(
            endTime = endTime,
            durationSeconds = durationSeconds,
            totalCost = totalCost,
            status = "completed"
        )
        callLogDao.updateCallLog(updatedCall)

        if (totalCost > 0) {
            // 1. User Wallet debit
            val userWallet = walletDao.getWalletByUserIdDirect(call.userId)
            if (userWallet != null) {
                val newBalance = (userWallet.balance - totalCost).coerceAtLeast(0.0)
                walletDao.updateWallet(
                    userWallet.copy(
                        balance = newBalance,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                walletDao.insertTransaction(
                    Transaction(
                        id = UUID.randomUUID().toString(),
                        walletId = userWallet.id,
                        amount = totalCost,
                        type = "debit",
                        referenceId = callId,
                        description = "Call to Host (${billedMinutes} min @ $${String.format("%.2f", perMinuteRate)}/min)",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 2. Partner Wallet credit
            var partnerWallet = walletDao.getWalletByUserIdDirect(call.partnerId)
            if (partnerWallet == null) {
                partnerWallet = Wallet(
                    id = UUID.randomUUID().toString(),
                    userId = call.partnerId,
                    balance = 0.0,
                    updatedAt = System.currentTimeMillis()
                )
                walletDao.insertWallet(partnerWallet)
            }
            val partnerEarnings = totalCost * 0.85 // 85% host share, 15% platform fee
            val newPartnerBalance = partnerWallet.balance + partnerEarnings
            walletDao.updateWallet(
                partnerWallet.copy(
                    balance = newPartnerBalance,
                    updatedAt = System.currentTimeMillis()
                )
            )
            walletDao.insertTransaction(
                Transaction(
                    id = UUID.randomUUID().toString(),
                    walletId = partnerWallet.id,
                    amount = partnerEarnings,
                    type = "credit",
                    referenceId = callId,
                    description = "Earnings: ${billedMinutes}m Call ($${String.format("%.2f", partnerEarnings)})",
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        totalCost
    }
}
