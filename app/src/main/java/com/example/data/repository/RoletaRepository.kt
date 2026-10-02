package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AccessCodeDao
import com.example.data.local.AccessCodeEntity
import com.example.data.local.AppDatabase
import com.example.data.local.CampaignDao
import com.example.data.local.CampaignEntity
import com.example.data.local.PrizeDao
import com.example.data.local.PrizeEntity
import com.example.data.local.SpinDao
import com.example.data.local.SpinEntity
import kotlinx.coroutines.flow.Flow
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

sealed class CodeValidationResult {
    data class Valid(val code: AccessCodeEntity, val campaign: CampaignEntity) : CodeValidationResult()
    data class AlreadyUsed(val message: String = "Este código já foi utilizado.") : CodeValidationResult()
    data class Invalid(val message: String = "Código inválido. Verifique o código informado.") : CodeValidationResult()
    data class CampaignUnavailable(val message: String = "Esta roleta não está disponível no momento.") : CodeValidationResult()
}

sealed class SpinExecutionResult {
    data class Success(
        val prize: PrizeEntity,
        val spinId: Long,
        val code: String,
        val allActivePrizes: List<PrizeEntity>,
        val winningIndex: Int
    ) : SpinExecutionResult()
    data class Error(val message: String) : SpinExecutionResult()
}

class RoletaRepository(
    private val database: AppDatabase,
    private val campaignDao: CampaignDao = database.campaignDao(),
    private val prizeDao: PrizeDao = database.prizeDao(),
    private val accessCodeDao: AccessCodeDao = database.accessCodeDao(),
    private val spinDao: SpinDao = database.spinDao()
) {
    val allCampaigns: Flow<List<CampaignEntity>> = campaignDao.getAllCampaigns()
    val allSpins: Flow<List<SpinEntity>> = spinDao.getAllSpins()
    val recentSpins: Flow<List<SpinEntity>> = spinDao.getRecentSpins(15)
    val totalSpinsCount: Flow<Int> = spinDao.countTotalSpins()
    val allAccessCodes: Flow<List<AccessCodeEntity>> = accessCodeDao.getAllCodes()

    fun getPrizesForCampaign(campaignId: Long): Flow<List<PrizeEntity>> =
        prizeDao.getPrizesForCampaign(campaignId)

    fun getCodesForCampaign(campaignId: Long): Flow<List<AccessCodeEntity>> =
        accessCodeDao.getCodesForCampaign(campaignId)

    fun getSpinsForCampaign(campaignId: Long): Flow<List<SpinEntity>> =
        spinDao.getSpinsForCampaign(campaignId)

    fun observeActiveCampaign(): Flow<CampaignEntity?> = campaignDao.observeActiveCampaign()

    suspend fun getActiveCampaign(): CampaignEntity? = campaignDao.getFirstActiveCampaign()

    suspend fun getCampaignById(id: Long): CampaignEntity? = campaignDao.getCampaignById(id)

    suspend fun getCampaignBySlug(slug: String): CampaignEntity? = campaignDao.getCampaignBySlug(slug)

    suspend fun saveCampaign(campaign: CampaignEntity): Long {
        return if (campaign.id == 0L) {
            campaignDao.insertCampaign(campaign)
        } else {
            campaignDao.updateCampaign(campaign)
            campaign.id
        }
    }

    suspend fun deleteCampaign(campaign: CampaignEntity) = campaignDao.deleteCampaign(campaign)

    suspend fun savePrize(prize: PrizeEntity): Long {
        return if (prize.id == 0L) {
            prizeDao.insertPrize(prize)
        } else {
            prizeDao.updatePrize(prize)
            prize.id
        }
    }

    suspend fun deletePrize(prize: PrizeEntity) = prizeDao.deletePrize(prize)

    suspend fun saveAccessCode(code: AccessCodeEntity): Long {
        return if (code.id == 0L) {
            accessCodeDao.insertCode(code)
        } else {
            accessCodeDao.updateCode(code)
            code.id
        }
    }

    suspend fun deleteAccessCode(code: AccessCodeEntity) = accessCodeDao.deleteCode(code)

    /**
     * Validates an access code against the database.
     */
    suspend fun validateAccessCode(
        inputCode: String,
        targetCampaignSlug: String? = null
    ): CodeValidationResult {
        val cleanCode = inputCode.trim().uppercase()
        if (cleanCode.isBlank()) {
            return CodeValidationResult.Invalid("Informe um código de acesso.")
        }

        val codeEntity = accessCodeDao.getCodeByString(cleanCode)
            ?: return CodeValidationResult.Invalid("Código inválido. Verifique o código informado.")

        if (codeEntity.status == AccessCodeEntity.STATUS_USED || codeEntity.usedAt != null) {
            return CodeValidationResult.AlreadyUsed("Este código já foi utilizado.")
        }

        if (codeEntity.status != AccessCodeEntity.STATUS_AVAILABLE) {
            return CodeValidationResult.Invalid("Código inválido ou inativo.")
        }

        val campaign = campaignDao.getCampaignById(codeEntity.campaignId)
            ?: return CodeValidationResult.CampaignUnavailable("Campanha não encontrada.")

        if (targetCampaignSlug != null && !campaign.slug.equals(targetCampaignSlug, ignoreCase = true)) {
            return CodeValidationResult.Invalid("Código não pertence a esta campanha.")
        }

        val now = System.currentTimeMillis()
        if (!campaign.active || now < campaign.startDate || now > campaign.endDate) {
            return CodeValidationResult.CampaignUnavailable("Esta roleta não está disponível no momento.")
        }

        return CodeValidationResult.Valid(code = codeEntity, campaign = campaign)
    }

    /**
     * Atomically executes the spin, draws weighted prize, updates stock,
     * marks code as used, and records spin.
     */
    suspend fun executeSpinAtomic(inputCode: String): SpinExecutionResult {
        val cleanCode = inputCode.trim().uppercase()
        if (cleanCode.isBlank()) {
            return SpinExecutionResult.Error("Código de acesso não fornecido.")
        }

        return database.withTransaction {
            // 1. Fetch code atomically
            val codeEntity = accessCodeDao.getCodeByString(cleanCode)
                ?: return@withTransaction SpinExecutionResult.Error("Código inválido. Verifique o código informado.")

            // 2. Validate usage state
            if (codeEntity.status == AccessCodeEntity.STATUS_USED || codeEntity.usedAt != null) {
                return@withTransaction SpinExecutionResult.Error("Este código já foi utilizado.")
            }
            if (codeEntity.status != AccessCodeEntity.STATUS_AVAILABLE) {
                return@withTransaction SpinExecutionResult.Error("Código não está disponível para uso.")
            }

            // 3. Validate campaign
            val campaign = campaignDao.getCampaignById(codeEntity.campaignId)
                ?: return@withTransaction SpinExecutionResult.Error("Campanha não encontrada.")

            val now = System.currentTimeMillis()
            if (!campaign.active || now < campaign.startDate || now > campaign.endDate) {
                return@withTransaction SpinExecutionResult.Error("Esta roleta não está disponível no momento.")
            }

            // 4. Fetch active prizes for wheel
            val allActivePrizes = prizeDao.getActivePrizesForCampaignSync(campaign.id)
            if (allActivePrizes.isEmpty()) {
                return@withTransaction SpinExecutionResult.Error("Nenhum prêmio cadastrado nesta roleta.")
            }

            // Filter prizes that have stock remaining
            val eligiblePrizes = allActivePrizes.filter { it.unlimitedQuantity || it.quantity > 0 }
            if (eligiblePrizes.isEmpty()) {
                return@withTransaction SpinExecutionResult.Error("Todos os prêmios desta roleta estão esgotados.")
            }

            // 5. Backend Weighted Lottery Draw
            val totalWeight = eligiblePrizes.sumOf { it.weight.coerceAtLeast(1) }
            var randomPick = Random.nextInt(totalWeight)
            var winningPrize = eligiblePrizes.first()

            for (prize in eligiblePrizes) {
                val w = prize.weight.coerceAtLeast(1)
                if (randomPick < w) {
                    winningPrize = prize
                    break
                }
                randomPick -= w
            }

            // 6. Update Prize Stock
            if (!winningPrize.unlimitedQuantity) {
                val updatedPrize = winningPrize.copy(
                    quantity = (winningPrize.quantity - 1).coerceAtLeast(0)
                )
                prizeDao.updatePrize(updatedPrize)
            }

            // 7. Mark Access Code as Used
            val updatedCode = codeEntity.copy(
                status = AccessCodeEntity.STATUS_USED,
                usedAt = now,
                prizeId = winningPrize.id,
                prizeName = winningPrize.name
            )
            accessCodeDao.updateCode(updatedCode)

            // 8. Register Spin Entry
            val spin = SpinEntity(
                campaignId = campaign.id,
                accessCodeId = codeEntity.id,
                code = codeEntity.code,
                prizeId = winningPrize.id,
                prizeName = winningPrize.name,
                clientName = "Não informado",
                observation = "",
                createdAt = now
            )
            val spinId = spinDao.insertSpin(spin)

            // Calculate index in the full list for wheel display alignment
            val winningIndex = allActivePrizes.indexOfFirst { it.id == winningPrize.id }.coerceAtLeast(0)

            SpinExecutionResult.Success(
                prize = winningPrize,
                spinId = spinId,
                code = codeEntity.code,
                allActivePrizes = allActivePrizes,
                winningIndex = winningIndex
            )
        }
    }

    /**
     * Updates client identification and observation for a spin record.
     */
    suspend fun updateSpinDetails(spinId: Long, clientName: String, observation: String) {
        val spin = spinDao.getSpinById(spinId) ?: return
        val updatedSpin = spin.copy(
            clientName = clientName.trim().ifBlank { "Não informado" },
            observation = observation.trim()
        )
        spinDao.updateSpin(updatedSpin)

        // Also sync back to access code
        val accessCode = accessCodeDao.getCodeByString(spin.code)
        if (accessCode != null) {
            accessCodeDao.updateCode(
                accessCode.copy(
                    clientName = clientName.trim(),
                    observation = observation.trim()
                )
            )
        }
    }

    /**
     * Bulk generates random unique access codes.
     */
    suspend fun generateBulkCodes(campaignId: Long, count: Int): Int {
        val alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val random = SecureRandom()
        val generatedCodes = mutableListOf<AccessCodeEntity>()

        var attempts = 0
        while (generatedCodes.size < count && attempts < count * 3) {
            attempts++
            val sb = java.lang.StringBuilder("RLT-")
            repeat(6) {
                sb.append(alphabet[random.nextInt(alphabet.length)])
            }
            val codeStr = sb.toString()
            if (generatedCodes.none { it.code == codeStr }) {
                generatedCodes.add(
                    AccessCodeEntity(
                        campaignId = campaignId,
                        code = codeStr,
                        status = AccessCodeEntity.STATUS_AVAILABLE
                    )
                )
            }
        }

        val insertedIds = accessCodeDao.insertCodes(generatedCodes)
        return insertedIds.count { it > 0 }
    }

    /**
     * Prepares CSV content from all spins.
     */
    fun exportSpinsToCsv(spins: List<SpinEntity>, campaignsMap: Map<Long, String>): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        val sb = StringBuilder()
        sb.append("Código,Cliente,Prêmio,Data,Hora,Campanha,Status,Observação\n")

        for (spin in spins) {
            val date = Date(spin.createdAt)
            val dateStr = dateFormat.format(date)
            val timeStr = timeFormat.format(date)
            val campaignName = campaignsMap[spin.campaignId] ?: "Campanha #${spin.campaignId}"

            val cleanClient = spin.clientName.replace("\"", "\"\"")
            val cleanPrize = spin.prizeName.replace("\"", "\"\"")
            val cleanCampaign = campaignName.replace("\"", "\"\"")
            val cleanObs = spin.observation.replace("\"", "\"\"")

            sb.append("\"${spin.code}\",\"$cleanClient\",\"$cleanPrize\",$dateStr,$timeStr,\"$cleanCampaign\",\"Utilizado\",\"$cleanObs\"\n")
        }
        return sb.toString()
    }

    /**
     * Seeds initial demo data if database is empty.
     */
    suspend fun seedInitialDataIfEmpty() {
        val activeCampaign = campaignDao.getFirstActiveCampaign()
        if (activeCampaign != null) return

        val now = System.currentTimeMillis()
        val oneMonthLater = now + (30L * 24 * 60 * 60 * 1000)

        val defaultCampaign = CampaignEntity(
            name = "Promoção Outubro",
            slug = "outubro",
            startDate = now - (24 * 60 * 60 * 1000), // started yesterday
            endDate = oneMonthLater,
            active = true
        )
        val campaignId = campaignDao.insertCampaign(defaultCampaign)

        val defaultPrizes = listOf(
            PrizeEntity(
                campaignId = campaignId,
                name = "5% de Desconto",
                description = "Válido em compras acima de R$ 50",
                weight = 40,
                quantity = 500,
                colorHex = "#10B981" // Emerald
            ),
            PrizeEntity(
                campaignId = campaignId,
                name = "10% de Desconto",
                description = "Válido para toda a loja",
                weight = 30,
                quantity = 200,
                colorHex = "#2563EB" // Royal Blue
            ),
            PrizeEntity(
                campaignId = campaignId,
                name = "15% de Desconto",
                description = "Válido para produtos selecionados",
                weight = 20,
                quantity = 50,
                colorHex = "#8B5CF6" // Violet
            ),
            PrizeEntity(
                campaignId = campaignId,
                name = "20% de Desconto",
                description = "Desconto especial de cliente fiel",
                weight = 8,
                quantity = 20,
                colorHex = "#F59E0B" // Gold
            ),
            PrizeEntity(
                campaignId = campaignId,
                name = "50% de Desconto",
                description = "Super prêmio da sorte!",
                weight = 2,
                quantity = 5,
                colorHex = "#EF4444" // Coral Red
            ),
            PrizeEntity(
                campaignId = campaignId,
                name = "Brinde Especial",
                description = "Retire no balcão da loja física",
                weight = 10,
                quantity = 30,
                colorHex = "#EC4899" // Pink
            )
        )
        prizeDao.insertPrizes(defaultPrizes)

        // Seed ready-to-test access codes
        val demoCodes = listOf(
            AccessCodeEntity(
                campaignId = campaignId,
                code = "ROULET-8K42P",
                status = AccessCodeEntity.STATUS_AVAILABLE
            ),
            AccessCodeEntity(
                campaignId = campaignId,
                code = "RLT-7X92KP",
                status = AccessCodeEntity.STATUS_AVAILABLE
            ),
            AccessCodeEntity(
                campaignId = campaignId,
                code = "RLT-4M8Q2A",
                status = AccessCodeEntity.STATUS_AVAILABLE
            ),
            AccessCodeEntity(
                campaignId = campaignId,
                code = "RLT-9ZK31B",
                status = AccessCodeEntity.STATUS_AVAILABLE
            ),
            AccessCodeEntity(
                campaignId = campaignId,
                code = "ROULET-001",
                status = AccessCodeEntity.STATUS_AVAILABLE
            ),
            AccessCodeEntity(
                campaignId = campaignId,
                code = "ROULET-USED01",
                status = AccessCodeEntity.STATUS_USED,
                usedAt = now - 3600000,
                prizeName = "10% de Desconto",
                clientName = "Maria Oliveira",
                observation = "Cliente frequente"
            )
        )
        accessCodeDao.insertCodes(demoCodes)
    }
}
