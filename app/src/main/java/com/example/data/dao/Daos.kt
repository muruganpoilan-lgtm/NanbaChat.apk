package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction as RoomTransaction
import androidx.room.Update
import com.example.data.model.CallLog
import com.example.data.model.CallLogWithPartner
import com.example.data.model.Language
import com.example.data.model.Partner
import com.example.data.model.PartnerLanguage
import com.example.data.model.PartnerMatchResult
import com.example.data.model.Transaction
import com.example.data.model.User
import com.example.data.model.Wallet
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserById(userId: String): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
}

@Dao
interface PartnerDao {
    @Query("SELECT * FROM partners ORDER BY status = 'online' DESC, rating DESC")
    fun getAllPartners(): Flow<List<Partner>>

    @Query("SELECT * FROM partners WHERE id = :partnerId LIMIT 1")
    fun getPartnerById(partnerId: String): Flow<Partner?>

    @Query("SELECT * FROM partners WHERE id = :partnerId LIMIT 1")
    suspend fun getPartnerByIdDirect(partnerId: String): Partner?

    /**
     * User's exact matchmaking SQL query:
     * Filters by preferred language, online status, excludes active call locks,
     * sorted by rating DESC and RANDOM(), returning the best immediate match.
     */
    @Query("""
        SELECT 
            p.id AS partner_id,
            p.name AS partner_name,
            p.per_minute_rate,
            p.rating
        FROM 
            partners p
        JOIN 
            partner_languages pl ON p.id = pl.partner_id
        JOIN 
            languages l ON pl.language_id = l.id
        WHERE 
            l.name = :languageName
            AND p.status = 'online'
            AND p.id NOT IN (
                SELECT partner_id 
                FROM call_logs 
                WHERE status = 'active'
            )
        ORDER BY 
            p.rating DESC,
            RANDOM()
        LIMIT 1
    """)
    suspend fun findMatch(languageName: String): PartnerMatchResult?

    /**
     * Query returning the full matched Partner entity
     */
    @Query("""
        SELECT p.*
        FROM partners p
        JOIN partner_languages pl ON p.id = pl.partner_id
        JOIN languages l ON pl.language_id = l.id
        WHERE l.name = :languageName
          AND p.status = 'online'
          AND p.id NOT IN (
              SELECT partner_id 
              FROM call_logs 
              WHERE status = 'active'
          )
        ORDER BY p.rating DESC, RANDOM()
        LIMIT 1
    """)
    suspend fun findFullPartnerMatch(languageName: String): Partner?

    @Query("""
        SELECT p.*
        FROM partners p
        JOIN partner_languages pl ON p.id = pl.partner_id
        JOIN languages l ON pl.language_id = l.id
        WHERE l.name = :languageName
        ORDER BY p.status = 'online' DESC, p.rating DESC
    """)
    fun getPartnersByLanguage(languageName: String): Flow<List<Partner>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartners(partners: List<Partner>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartner(partner: Partner)

    @Update
    suspend fun updatePartner(partner: Partner)

    @Query("UPDATE partners SET status = :status WHERE id = :partnerId")
    suspend fun updateStatus(partnerId: String, status: String)

    @Query("UPDATE partners SET bio = :bio, per_minute_rate = :rate WHERE id = :partnerId")
    suspend fun updateProfile(partnerId: String, bio: String, rate: Double)
}

@Dao
interface LanguageDao {
    @Query("SELECT * FROM languages ORDER BY name ASC")
    fun getAllLanguages(): Flow<List<Language>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLanguages(languages: List<Language>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPartnerLanguages(partnerLanguages: List<PartnerLanguage>)

    @Query("""
        SELECT l.* 
        FROM languages l
        JOIN partner_languages pl ON l.id = pl.language_id
        WHERE pl.partner_id = :partnerId
    """)
    fun getLanguagesForPartner(partnerId: String): Flow<List<Language>>

    @Query("""
        SELECT l.* 
        FROM languages l
        JOIN partner_languages pl ON l.id = pl.language_id
        WHERE pl.partner_id = :partnerId
    """)
    suspend fun getLanguagesForPartnerDirect(partnerId: String): List<Language>

    @Query("DELETE FROM partner_languages WHERE partner_id = :partnerId")
    suspend fun clearLanguagesForPartner(partnerId: String)
}

@Dao
interface CallLogDao {
    @Query("""
        SELECT 
            c.id AS id,
            c.partner_id AS partnerId,
            p.name AS partnerName,
            c.start_time AS startTime,
            c.end_time AS endTime,
            c.duration_seconds AS durationSeconds,
            c.total_cost AS totalCost,
            c.status AS status,
            p.per_minute_rate AS perMinuteRate
        FROM call_logs c
        JOIN partners p ON c.partner_id = p.id
        WHERE c.user_id = :userId
        ORDER BY c.start_time DESC
    """)
    fun getCallLogsWithPartnerForUser(userId: String): Flow<List<CallLogWithPartner>>

    @Query("SELECT * FROM call_logs WHERE partner_id = :partnerId ORDER BY start_time DESC")
    fun getCallLogsForPartner(partnerId: String): Flow<List<CallLog>>

    @Query("SELECT * FROM call_logs WHERE id = :callId LIMIT 1")
    suspend fun getCallLogById(callId: String): CallLog?

    @Query("SELECT * FROM call_logs WHERE status = 'active' LIMIT 1")
    fun getActiveCall(): Flow<CallLog?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLog)

    @Update
    suspend fun updateCallLog(callLog: CallLog)
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets WHERE user_id = :userId LIMIT 1")
    fun getWalletByUserId(userId: String): Flow<Wallet?>

    @Query("SELECT * FROM wallets WHERE user_id = :userId LIMIT 1")
    suspend fun getWalletByUserIdDirect(userId: String): Wallet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: Wallet)

    @Update
    suspend fun updateWallet(wallet: Wallet)

    @Query("SELECT * FROM transactions WHERE wallet_id = :walletId ORDER BY created_at DESC")
    fun getTransactionsByWalletId(walletId: String): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @RoomTransaction
    suspend fun addCredits(walletId: String, amount: Double, refId: String, description: String) {
        // Safe update with ACID compliance
        val currentWallet = getWalletByIdDirect(walletId) ?: return
        val newBalance = currentWallet.balance + amount
        updateWallet(currentWallet.copy(balance = newBalance, updatedAt = System.currentTimeMillis()))
        insertTransaction(
            Transaction(
                id = java.util.UUID.randomUUID().toString(),
                walletId = walletId,
                amount = amount,
                type = "credit",
                referenceId = refId,
                description = description
            )
        )
    }

    @Query("SELECT * FROM wallets WHERE id = :walletId LIMIT 1")
    suspend fun getWalletByIdDirect(walletId: String): Wallet?
}
