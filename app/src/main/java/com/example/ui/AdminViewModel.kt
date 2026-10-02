package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccessCodeEntity
import com.example.data.local.CampaignEntity
import com.example.data.local.PrizeEntity
import com.example.data.local.SpinEntity
import com.example.data.repository.RoletaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AdminTab {
    DASHBOARD,
    CAMPAIGNS,
    PRIZES,
    CODES,
    RESULTS,
    SETTINGS
}

data class DashboardStats(
    val totalCodes: Int = 0,
    val availableCodes: Int = 0,
    val usedCodes: Int = 0,
    val totalSpins: Int = 0,
    val prizeDistribution: Map<String, Int> = emptyMap()
)

data class AdminUiState(
    val isAuthenticated: Boolean = false,
    val activeTab: AdminTab = AdminTab.DASHBOARD,
    val campaigns: List<CampaignEntity> = emptyList(),
    val selectedCampaignId: Long? = null,
    val prizes: List<PrizeEntity> = emptyList(),
    val accessCodes: List<AccessCodeEntity> = emptyList(),
    val spins: List<SpinEntity> = emptyList(),
    val stats: DashboardStats = DashboardStats(),
    val searchQuery: String = "",
    val filterStatus: String? = null,
    val message: String? = null,
    val adminPin: String = "1234"
)

class AdminViewModel(
    private val repository: RoletaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allCampaigns.collect { campaigns ->
                _uiState.update { current ->
                    val selected = current.selectedCampaignId ?: campaigns.firstOrNull()?.id
                    current.copy(campaigns = campaigns, selectedCampaignId = selected)
                }
                loadPrizesAndCodes()
            }
        }

        viewModelScope.launch {
            repository.allSpins.collect { spins ->
                _uiState.update { current ->
                    current.copy(spins = spins)
                }
                updateDashboardStats()
            }
        }

        viewModelScope.launch {
            repository.allAccessCodes.collect { codes ->
                _uiState.update { current ->
                    current.copy(accessCodes = codes)
                }
                updateDashboardStats()
            }
        }
    }

    private fun loadPrizesAndCodes() {
        val campId = _uiState.value.selectedCampaignId ?: return
        viewModelScope.launch {
            repository.getPrizesForCampaign(campId).collect { prizes ->
                _uiState.update { it.copy(prizes = prizes) }
            }
        }
    }

    fun authenticate(inputPin: String): Boolean {
        return if (inputPin == _uiState.value.adminPin) {
            _uiState.update { it.copy(isAuthenticated = true) }
            true
        } else {
            false
        }
    }

    fun logout() {
        _uiState.update { it.copy(isAuthenticated = false) }
    }

    fun setTab(tab: AdminTab) {
        _uiState.update { it.copy(activeTab = tab, searchQuery = "", message = null) }
    }

    fun setSelectedCampaign(campaignId: Long) {
        _uiState.update { it.copy(selectedCampaignId = campaignId) }
        loadPrizesAndCodes()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setStatusFilter(status: String?) {
        _uiState.update { it.copy(filterStatus = status) }
    }

    private fun updateDashboardStats() {
        val codes = _uiState.value.accessCodes
        val spins = _uiState.value.spins

        val total = codes.size
        val used = codes.count { it.status == AccessCodeEntity.STATUS_USED }
        val available = codes.count { it.status == AccessCodeEntity.STATUS_AVAILABLE }

        val prizeCount = mutableMapOf<String, Int>()
        spins.forEach { spin ->
            prizeCount[spin.prizeName] = (prizeCount[spin.prizeName] ?: 0) + 1
        }

        _uiState.update { current ->
            current.copy(
                stats = DashboardStats(
                    totalCodes = total,
                    availableCodes = available,
                    usedCodes = used,
                    totalSpins = spins.size,
                    prizeDistribution = prizeCount
                )
            )
        }
    }

    fun saveCampaign(campaign: CampaignEntity) {
        viewModelScope.launch {
            repository.saveCampaign(campaign)
            _uiState.update { it.copy(message = "Campanha salva com sucesso!") }
        }
    }

    fun deleteCampaign(campaign: CampaignEntity) {
        viewModelScope.launch {
            repository.deleteCampaign(campaign)
            _uiState.update { it.copy(message = "Campanha removida.") }
        }
    }

    fun savePrize(prize: PrizeEntity) {
        viewModelScope.launch {
            repository.savePrize(prize)
            _uiState.update { it.copy(message = "Prêmio salvo com sucesso!") }
        }
    }

    fun deletePrize(prize: PrizeEntity) {
        viewModelScope.launch {
            repository.deletePrize(prize)
            _uiState.update { it.copy(message = "Prêmio excluído.") }
        }
    }

    fun saveAccessCode(code: AccessCodeEntity) {
        viewModelScope.launch {
            repository.saveAccessCode(code)
            _uiState.update { it.copy(message = "Código salvo com sucesso!") }
        }
    }

    fun deleteAccessCode(code: AccessCodeEntity) {
        viewModelScope.launch {
            repository.deleteAccessCode(code)
            _uiState.update { it.copy(message = "Código removido.") }
        }
    }

    fun generateBulkCodes(campaignId: Long, count: Int) {
        viewModelScope.launch {
            val generated = repository.generateBulkCodes(campaignId, count)
            _uiState.update { it.copy(message = "$generated novos códigos foram gerados com sucesso!") }
        }
    }

    fun updateSpinDetails(spinId: Long, clientName: String, observation: String) {
        viewModelScope.launch {
            repository.updateSpinDetails(spinId, clientName, observation)
            _uiState.update { it.copy(message = "Dados do cliente atualizados!") }
        }
    }

    fun exportSpinsCsv(): String {
        val campMap = _uiState.value.campaigns.associate { it.id to it.name }
        return repository.exportSpinsToCsv(_uiState.value.spins, campMap)
    }

    fun updatePin(newPin: String) {
        if (newPin.length >= 4) {
            _uiState.update { it.copy(adminPin = newPin, message = "PIN de acesso atualizado!") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            _uiState.update { it.copy(message = "Dados iniciais redefinidos com sucesso!") }
        }
    }
}

class AdminViewModelFactory(
    private val repository: RoletaRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AdminViewModel(repository) as T
    }
}
