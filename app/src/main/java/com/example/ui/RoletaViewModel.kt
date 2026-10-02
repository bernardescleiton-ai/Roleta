package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccessCodeEntity
import com.example.data.local.CampaignEntity
import com.example.data.local.PrizeEntity
import com.example.data.repository.CodeValidationResult
import com.example.data.repository.RoletaRepository
import com.example.data.repository.SpinExecutionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoletaUiState(
    val currentCampaign: CampaignEntity? = null,
    val prizes: List<PrizeEntity> = emptyList(),
    val inputCode: String = "",
    val isValidated: Boolean = false,
    val validatedCode: AccessCodeEntity? = null,
    val validationSuccessMessage: String? = null,
    val errorMessage: String? = null,
    val isCheckingCode: Boolean = false,
    val isSpinning: Boolean = false,
    val targetWinningIndex: Int? = null,
    val wonPrize: PrizeEntity? = null,
    val usedCodeString: String? = null,
    val showResultDialog: Boolean = false,
    val allCampaigns: List<CampaignEntity> = emptyList()
)

class RoletaViewModel(
    private val repository: RoletaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoletaUiState())
    val uiState: StateFlow<RoletaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            loadInitialCampaign()
        }

        viewModelScope.launch {
            repository.allCampaigns.collect { campaigns ->
                _uiState.update { it.copy(allCampaigns = campaigns) }
            }
        }
    }

    private suspend fun loadInitialCampaign() {
        val activeCampaign = repository.getActiveCampaign()
        if (activeCampaign != null) {
            selectCampaign(activeCampaign)
        }
    }

    fun selectCampaign(campaign: CampaignEntity) {
        _uiState.update {
            it.copy(
                currentCampaign = campaign,
                isValidated = false,
                validatedCode = null,
                validationSuccessMessage = null,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            repository.getPrizesForCampaign(campaign.id).collect { prizes ->
                _uiState.update { it.copy(prizes = prizes) }
            }
        }
    }

    fun onCodeInputChanged(newCode: String) {
        _uiState.update {
            it.copy(
                inputCode = newCode.uppercase().trim(),
                errorMessage = null
            )
        }
    }

    fun validateCode() {
        val codeToTest = _uiState.value.inputCode
        if (codeToTest.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Digite um código para continuar.") }
            return
        }

        _uiState.update { it.copy(isCheckingCode = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.validateAccessCode(
                inputCode = codeToTest,
                targetCampaignSlug = _uiState.value.currentCampaign?.slug
            )

            when (result) {
                is CodeValidationResult.Valid -> {
                    // Refresh campaign if needed
                    val campaign = result.campaign
                    if (_uiState.value.currentCampaign?.id != campaign.id) {
                        selectCampaign(campaign)
                    }
                    _uiState.update {
                        it.copy(
                            isCheckingCode = false,
                            isValidated = true,
                            validatedCode = result.code,
                            validationSuccessMessage = "Código validado! Você tem 1 giro disponível.",
                            errorMessage = null
                        )
                    }
                }
                is CodeValidationResult.AlreadyUsed -> {
                    _uiState.update {
                        it.copy(
                            isCheckingCode = false,
                            isValidated = false,
                            validatedCode = null,
                            errorMessage = result.message
                        )
                    }
                }
                is CodeValidationResult.Invalid -> {
                    _uiState.update {
                        it.copy(
                            isCheckingCode = false,
                            isValidated = false,
                            validatedCode = null,
                            errorMessage = result.message
                        )
                    }
                }
                is CodeValidationResult.CampaignUnavailable -> {
                    _uiState.update {
                        it.copy(
                            isCheckingCode = false,
                            isValidated = false,
                            validatedCode = null,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun spinWheel() {
        val validated = _uiState.value.validatedCode
        if (validated == null || !_uiState.value.isValidated || _uiState.value.isSpinning) {
            return
        }

        _uiState.update { it.copy(isSpinning = true, errorMessage = null) }

        viewModelScope.launch {
            // Atomic transaction call on repository
            val result = repository.executeSpinAtomic(validated.code)

            when (result) {
                is SpinExecutionResult.Success -> {
                    _uiState.update {
                        it.copy(
                            wonPrize = result.prize,
                            targetWinningIndex = result.winningIndex,
                            usedCodeString = result.code,
                            // Ensure display prizes match active wheel
                            prizes = result.allActivePrizes
                        )
                    }
                    // Wheel will animate via LaunchedEffect in LuckyWheelCanvas
                }
                is SpinExecutionResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSpinning = false,
                            isValidated = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun onSpinAnimationFinished() {
        _uiState.update {
            it.copy(
                isSpinning = false,
                isValidated = false, // code is now consumed
                showResultDialog = true
            )
        }
    }

    fun dismissResultDialog() {
        _uiState.update {
            it.copy(
                showResultDialog = false,
                inputCode = "",
                isValidated = false,
                validatedCode = null,
                validationSuccessMessage = null,
                wonPrize = null,
                targetWinningIndex = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

class RoletaViewModelFactory(
    private val repository: RoletaRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RoletaViewModel(repository) as T
    }
}
