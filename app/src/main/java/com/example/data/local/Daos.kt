package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {
    @Query("SELECT * FROM campaigns ORDER BY createdAt DESC")
    fun getAllCampaigns(): Flow<List<CampaignEntity>>

    @Query("SELECT * FROM campaigns WHERE id = :id LIMIT 1")
    suspend fun getCampaignById(id: Long): CampaignEntity?

    @Query("SELECT * FROM campaigns WHERE slug = :slug LIMIT 1")
    suspend fun getCampaignBySlug(slug: String): CampaignEntity?

    @Query("SELECT * FROM campaigns WHERE active = 1 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getFirstActiveCampaign(): CampaignEntity?

    @Query("SELECT * FROM campaigns WHERE active = 1 ORDER BY createdAt DESC LIMIT 1")
    fun observeActiveCampaign(): Flow<CampaignEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: CampaignEntity): Long

    @Update
    suspend fun updateCampaign(campaign: CampaignEntity)

    @Delete
    suspend fun deleteCampaign(campaign: CampaignEntity)
}

@Dao
interface PrizeDao {
    @Query("SELECT * FROM prizes WHERE campaignId = :campaignId ORDER BY weight DESC")
    fun getPrizesForCampaign(campaignId: Long): Flow<List<PrizeEntity>>

    @Query("SELECT * FROM prizes WHERE campaignId = :campaignId AND active = 1 ORDER BY weight DESC")
    suspend fun getActivePrizesForCampaignSync(campaignId: Long): List<PrizeEntity>

    @Query("SELECT * FROM prizes WHERE id = :id LIMIT 1")
    suspend fun getPrizeById(id: Long): PrizeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrize(prize: PrizeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrizes(prizes: List<PrizeEntity>)

    @Update
    suspend fun updatePrize(prize: PrizeEntity)

    @Delete
    suspend fun deletePrize(prize: PrizeEntity)
}

@Dao
interface AccessCodeDao {
    @Query("SELECT * FROM access_codes WHERE campaignId = :campaignId ORDER BY createdAt DESC")
    fun getCodesForCampaign(campaignId: Long): Flow<List<AccessCodeEntity>>

    @Query("SELECT * FROM access_codes ORDER BY createdAt DESC")
    fun getAllCodes(): Flow<List<AccessCodeEntity>>

    @Query("SELECT * FROM access_codes WHERE UPPER(code) = UPPER(:code) LIMIT 1")
    suspend fun getCodeByString(code: String): AccessCodeEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCode(code: AccessCodeEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCodes(codes: List<AccessCodeEntity>): List<Long>

    @Update
    suspend fun updateCode(code: AccessCodeEntity)

    @Delete
    suspend fun deleteCode(code: AccessCodeEntity)

    @Query("SELECT COUNT(*) FROM access_codes WHERE campaignId = :campaignId")
    fun countTotalCodes(campaignId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM access_codes WHERE campaignId = :campaignId AND status = 'UTILIZADO'")
    fun countUsedCodes(campaignId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM access_codes WHERE campaignId = :campaignId AND status = 'DISPONIVEL'")
    fun countAvailableCodes(campaignId: Long): Flow<Int>
}

@Dao
interface SpinDao {
    @Query("SELECT * FROM spins ORDER BY createdAt DESC")
    fun getAllSpins(): Flow<List<SpinEntity>>

    @Query("SELECT * FROM spins WHERE campaignId = :campaignId ORDER BY createdAt DESC")
    fun getSpinsForCampaign(campaignId: Long): Flow<List<SpinEntity>>

    @Query("SELECT * FROM spins ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentSpins(limit: Int = 10): Flow<List<SpinEntity>>

    @Query("SELECT * FROM spins WHERE id = :id LIMIT 1")
    suspend fun getSpinById(id: Long): SpinEntity?

    @Query("SELECT * FROM spins WHERE code = :code LIMIT 1")
    suspend fun getSpinByCode(code: String): SpinEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpin(spin: SpinEntity): Long

    @Update
    suspend fun updateSpin(spin: SpinEntity)

    @Delete
    suspend fun deleteSpin(spin: SpinEntity)

    @Query("SELECT COUNT(*) FROM spins")
    fun countTotalSpins(): Flow<Int>
}
