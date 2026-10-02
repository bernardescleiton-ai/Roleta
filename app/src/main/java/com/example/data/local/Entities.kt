package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "campaigns",
    indices = [Index(value = ["slug"], unique = true)]
)
data class CampaignEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val slug: String,
    val startDate: Long,
    val endDate: Long,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "prizes",
    foreignKeys = [
        ForeignKey(
            entity = CampaignEntity::class,
            parentColumns = ["id"],
            childColumns = ["campaignId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("campaignId")]
)
data class PrizeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val campaignId: Long,
    val name: String,
    val description: String = "",
    val weight: Int = 10,
    val quantity: Int = 100,
    val unlimitedQuantity: Boolean = false,
    val active: Boolean = true,
    val colorHex: String = "#F59E0B",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "access_codes",
    indices = [
        Index(value = ["code"], unique = true),
        Index("campaignId"),
        Index("status")
    ]
)
data class AccessCodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val campaignId: Long,
    val code: String,
    val status: String = STATUS_AVAILABLE, // "DISPONIVEL", "UTILIZADO", "INATIVO"
    val usedAt: Long? = null,
    val prizeId: Long? = null,
    val prizeName: String? = null,
    val clientName: String = "",
    val observation: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_AVAILABLE = "DISPONIVEL"
        const val STATUS_USED = "UTILIZADO"
        const val STATUS_INACTIVE = "INATIVO"
    }
}

@Entity(
    tableName = "spins",
    indices = [
        Index("campaignId"),
        Index("accessCodeId"),
        Index("code"),
        Index("createdAt")
    ]
)
data class SpinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val campaignId: Long,
    val accessCodeId: Long,
    val code: String,
    val prizeId: Long,
    val prizeName: String,
    val clientName: String = "Não informado",
    val observation: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
