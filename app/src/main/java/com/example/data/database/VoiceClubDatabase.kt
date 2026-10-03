package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CallLogDao
import com.example.data.dao.LanguageDao
import com.example.data.dao.PartnerDao
import com.example.data.dao.UserDao
import com.example.data.dao.WalletDao
import com.example.data.model.CallLog
import com.example.data.model.Language
import com.example.data.model.Partner
import com.example.data.model.PartnerLanguage
import com.example.data.model.Transaction
import com.example.data.model.User
import com.example.data.model.Wallet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        User::class,
        Partner::class,
        Language::class,
        PartnerLanguage::class,
        CallLog::class,
        Wallet::class,
        Transaction::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VoiceClubDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun partnerDao(): PartnerDao
    abstract fun languageDao(): LanguageDao
    abstract fun callLogDao(): CallLogDao
    abstract fun walletDao(): WalletDao

    companion object {
        const val DEFAULT_USER_ID = "user-default-101"
        const val DEFAULT_USER_WALLET_ID = "wallet-user-101"
        const val SELF_PARTNER_ID = "partner-self-001"
        const val SELF_PARTNER_WALLET_ID = "wallet-partner-001"

        @Volatile
        private var INSTANCE: VoiceClubDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): VoiceClubDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoiceClubDatabase::class.java,
                    "voiceclub_database"
                )
                    .addCallback(VoiceClubDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class VoiceClubDatabaseCallback(
        private val scope: CoroutineScope
    ) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(db: VoiceClubDatabase) {
            val userDao = db.userDao()
            val partnerDao = db.partnerDao()
            val languageDao = db.languageDao()
            val walletDao = db.walletDao()
            val callLogDao = db.callLogDao()

            // 1. Initial User
            val defaultUser = User(
                id = DEFAULT_USER_ID,
                phoneNumber = "+91 98765 43210",
                name = "Murugan P.",
                createdAt = System.currentTimeMillis()
            )
            userDao.insertUser(defaultUser)

            // User Wallet with starter balance
            val userWallet = Wallet(
                id = DEFAULT_USER_WALLET_ID,
                userId = DEFAULT_USER_ID,
                balance = 25.00,
                updatedAt = System.currentTimeMillis()
            )
            walletDao.insertWallet(userWallet)
            walletDao.insertTransaction(
                Transaction(
                    id = UUID.randomUUID().toString(),
                    walletId = DEFAULT_USER_WALLET_ID,
                    amount = 25.00,
                    type = "credit",
                    referenceId = "WELCOME_BONUS",
                    description = "Welcome Calling Credits Bonus",
                    createdAt = System.currentTimeMillis()
                )
            )

            // 2. Languages
            val languages = listOf(
                Language(id = 1, name = "Tamil"),
                Language(id = 2, name = "Hindi"),
                Language(id = 3, name = "English"),
                Language(id = 4, name = "Telugu"),
                Language(id = 5, name = "Kannada"),
                Language(id = 6, name = "Spanish"),
                Language(id = 7, name = "French")
            )
            languageDao.insertLanguages(languages)

            // 3. Partners
            val partners = listOf(
                Partner(
                    id = "partner-001",
                    phoneNumber = "+91 99401 11223",
                    name = "Kavitha Rajan",
                    bio = "Native Tamil speaker & storytelling host. Passionate about conversations on culture, cinema, and life.",
                    status = "online",
                    perMinuteRate = 0.35,
                    rating = 4.95,
                    isVerified = true,
                    totalCallsCompleted = 142,
                    avatarGradientIndex = 0
                ),
                Partner(
                    id = "partner-002",
                    phoneNumber = "+91 98110 33445",
                    name = "Aarav Sharma",
                    bio = "Fluent Hindi & English speaker. Voice artist and friendly buddy for spoken Hindi practice and motivation.",
                    status = "online",
                    perMinuteRate = 0.40,
                    rating = 4.88,
                    isVerified = true,
                    totalCallsCompleted = 98,
                    avatarGradientIndex = 1
                ),
                Partner(
                    id = "partner-003",
                    phoneNumber = "+1 415 555 0192",
                    name = "Sarah Jenkins",
                    bio = "Certified English communication coach. Accent practice, interview confidence & daily chat partner.",
                    status = "online",
                    perMinuteRate = 0.50,
                    rating = 4.98,
                    isVerified = true,
                    totalCallsCompleted = 210,
                    avatarGradientIndex = 2
                ),
                Partner(
                    id = "partner-004",
                    phoneNumber = "+91 94470 66778",
                    name = "Suresh Nair",
                    bio = "Multilingual host (Tamil, Hindi, Malayalam). Tech enthusiast, career discussions & lighthearted chats.",
                    status = "online",
                    perMinuteRate = 0.30,
                    rating = 4.75,
                    isVerified = true,
                    totalCallsCompleted = 65,
                    avatarGradientIndex = 3
                ),
                Partner(
                    id = "partner-005",
                    phoneNumber = "+91 97000 88990",
                    name = "Divya Reddy",
                    bio = "Warm Telugu & English conversationalist. Positive vibes, daily check-ins & language exchange.",
                    status = "online",
                    perMinuteRate = 0.45,
                    rating = 4.85,
                    isVerified = true,
                    totalCallsCompleted = 84,
                    avatarGradientIndex = 4
                ),
                Partner(
                    id = "partner-006",
                    phoneNumber = "+34 600 123 456",
                    name = "Carlos Silva",
                    bio = "Spanish and English native. Conversational Spanish lessons, travel stories, and casual discussions.",
                    status = "online",
                    perMinuteRate = 0.55,
                    rating = 4.90,
                    isVerified = true,
                    totalCallsCompleted = 120,
                    avatarGradientIndex = 5
                ),
                Partner(
                    id = "partner-007",
                    phoneNumber = "+91 98400 55667",
                    name = "Priya Sundaram",
                    bio = "Literary enthusiast, Tamil poetry & conversational practice. Highly rated for engaging voice sessions.",
                    status = "online",
                    perMinuteRate = 0.38,
                    rating = 4.92,
                    isVerified = true,
                    totalCallsCompleted = 176,
                    avatarGradientIndex = 6
                ),
                // 4. Partner Mode Profile (for host testing)
                Partner(
                    id = SELF_PARTNER_ID,
                    phoneNumber = "+91 91234 56789",
                    name = "My Host Profile",
                    bio = "Available for engaging audio sessions in Tamil & English. Let's connect!",
                    status = "online",
                    perMinuteRate = 0.45,
                    rating = 5.0,
                    isVerified = true,
                    totalCallsCompleted = 12,
                    avatarGradientIndex = 0
                )
            )
            partnerDao.insertPartners(partners)

            // Partner Wallets
            val partnerWallets = listOf(
                Wallet(
                    id = SELF_PARTNER_WALLET_ID,
                    userId = SELF_PARTNER_ID,
                    balance = 48.60,
                    updatedAt = System.currentTimeMillis()
                )
            )
            partnerWallets.forEach { walletDao.insertWallet(it) }

            // 5. Partner Languages mapping
            val partnerLanguages = listOf(
                // Kavitha -> Tamil (1), English (3)
                PartnerLanguage("partner-001", 1),
                PartnerLanguage("partner-001", 3),

                // Aarav -> Hindi (2), English (3)
                PartnerLanguage("partner-002", 2),
                PartnerLanguage("partner-002", 3),

                // Sarah -> English (3), Spanish (6)
                PartnerLanguage("partner-003", 3),
                PartnerLanguage("partner-003", 6),

                // Suresh -> Tamil (1), Hindi (2)
                PartnerLanguage("partner-004", 1),
                PartnerLanguage("partner-004", 2),

                // Divya -> Telugu (4), English (3), Hindi (2)
                PartnerLanguage("partner-005", 4),
                PartnerLanguage("partner-005", 3),
                PartnerLanguage("partner-005", 2),

                // Carlos -> Spanish (6), English (3)
                PartnerLanguage("partner-006", 6),
                PartnerLanguage("partner-006", 3),

                // Priya -> Tamil (1), English (3)
                PartnerLanguage("partner-007", 1),
                PartnerLanguage("partner-007", 3),

                // Self Host -> Tamil (1), English (3)
                PartnerLanguage(SELF_PARTNER_ID, 1),
                PartnerLanguage(SELF_PARTNER_ID, 3)
            )
            languageDao.insertPartnerLanguages(partnerLanguages)

            // 6. Sample Initial Call Log
            val sampleCall = CallLog(
                id = UUID.randomUUID().toString(),
                userId = DEFAULT_USER_ID,
                partnerId = "partner-001",
                startTime = System.currentTimeMillis() - 86400000L,
                endTime = System.currentTimeMillis() - 86400000L + 340000L,
                durationSeconds = 340,
                totalCost = 2.10,
                status = "completed"
            )
            callLogDao.insertCallLog(sampleCall)
        }
    }
}
