package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String, // UUID
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,
    val name: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "partners")
data class Partner(
    @PrimaryKey
    val id: String, // UUID
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,
    val name: String,
    val bio: String,
    val status: String, // 'offline', 'online', 'busy'
    @ColumnInfo(name = "per_minute_rate")
    val perMinuteRate: Double,
    val rating: Double,
    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean = true,
    @ColumnInfo(name = "total_calls_completed")
    val totalCallsCompleted: Int = 0,
    @ColumnInfo(name = "avatar_gradient_index")
    val avatarGradientIndex: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "languages")
data class Language(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String // e.g. "Tamil", "Hindi", "English", "Telugu", "Kannada", "Spanish"
)

@Entity(
    tableName = "partner_languages",
    primaryKeys = ["partner_id", "language_id"],
    foreignKeys = [
        ForeignKey(
            entity = Partner::class,
            parentColumns = ["id"],
            childColumns = ["partner_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Language::class,
            parentColumns = ["id"],
            childColumns = ["language_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["partner_id"]),
        Index(value = ["language_id"])
    ]
)
data class PartnerLanguage(
    @ColumnInfo(name = "partner_id")
    val partnerId: String,
    @ColumnInfo(name = "language_id")
    val languageId: Long
)

@Entity(
    tableName = "call_logs",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["partner_id"]),
        Index(value = ["status"])
    ]
)
data class CallLog(
    @PrimaryKey
    val id: String, // UUID
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "partner_id")
    val partnerId: String,
    @ColumnInfo(name = "start_time")
    val startTime: Long,
    @ColumnInfo(name = "end_time")
    val endTime: Long = 0L,
    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Int = 0,
    @ColumnInfo(name = "total_cost")
    val totalCost: Double = 0.0,
    val status: String // 'active', 'completed', 'missed', 'rejected'
)

@Entity(tableName = "wallets")
data class Wallet(
    @PrimaryKey
    val id: String, // UUID
    @ColumnInfo(name = "user_id")
    val userId: String, // Can be user ID or partner ID
    val balance: Double,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["wallet_id"])
    ]
)
data class Transaction(
    @PrimaryKey
    val id: String, // UUID
    @ColumnInfo(name = "wallet_id")
    val walletId: String,
    val amount: Double,
    val type: String, // 'credit', 'debit'
    @ColumnInfo(name = "reference_id")
    val referenceId: String,
    val description: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Result DTO strictly matching the SQL matchmaking query requested:
 * SELECT p.id AS partner_id, p.name AS partner_name, p.per_minute_rate, p.rating ...
 */
data class PartnerMatchResult(
    @ColumnInfo(name = "partner_id")
    val partnerId: String,
    @ColumnInfo(name = "partner_name")
    val partnerName: String,
    @ColumnInfo(name = "per_minute_rate")
    val perMinuteRate: Double,
    @ColumnInfo(name = "rating")
    val rating: Double
)

/**
 * Data class joining call logs with partner info for user display
 */
data class CallLogWithPartner(
    val id: String,
    val partnerId: String,
    val partnerName: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Int,
    val totalCost: Double,
    val status: String,
    val perMinuteRate: Double
)
