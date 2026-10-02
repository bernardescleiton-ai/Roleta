package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AccessCodeEntity
import com.example.data.local.CampaignEntity
import com.example.data.local.PrizeEntity
import com.example.data.local.SpinEntity
import com.example.ui.AdminTab
import com.example.ui.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    viewModel: AdminViewModel,
    onBackToRoleta: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Android back button handling
    BackHandler {
        if (uiState.activeTab != AdminTab.DASHBOARD) {
            viewModel.setTab(AdminTab.DASHBOARD)
        } else {
            onBackToRoleta()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0B0F19),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF111827),
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBackToRoleta,
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar para Roleta",
                            tint = Color.White
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Painel Administrativo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        val activeCamp = uiState.campaigns.find { it.id == uiState.selectedCampaignId }
                        Text(
                            text = "Campanha: ${activeCamp?.name ?: "Nenhuma selecionada"}",
                            fontSize = 12.sp,
                            color = Color(0xFFF59E0B)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.logout()
                            onBackToRoleta()
                        },
                        modifier = Modifier.testTag("admin_logout_button")
                    ) {
                        Text("Sair", color = Color(0xFFF87171), fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal Admin Tabs
            ScrollableTabRow(
                selectedTabIndex = uiState.activeTab.ordinal,
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFFF59E0B),
                edgePadding = 12.dp
            ) {
                AdminTab.values().forEach { tab ->
                    Tab(
                        selected = uiState.activeTab == tab,
                        onClick = { viewModel.setTab(tab) },
                        modifier = Modifier.testTag("admin_tab_${tab.name.lowercase()}"),
                        text = {
                            Text(
                                text = when (tab) {
                                    AdminTab.DASHBOARD -> "Dashboard"
                                    AdminTab.CAMPAIGNS -> "Campanhas"
                                    AdminTab.PRIZES -> "Prêmios"
                                    AdminTab.CODES -> "Códigos"
                                    AdminTab.RESULTS -> "Resultados"
                                    AdminTab.SETTINGS -> "Configurações"
                                },
                                fontWeight = if (uiState.activeTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.activeTab == tab) Color(0xFFF59E0B) else Color(0xFF94A3B8)
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                when (uiState.activeTab) {
                    AdminTab.DASHBOARD -> DashboardTabContent(viewModel)
                    AdminTab.CAMPAIGNS -> CampaignsTabContent(viewModel)
                    AdminTab.PRIZES -> PrizesTabContent(viewModel)
                    AdminTab.CODES -> CodesTabContent(viewModel)
                    AdminTab.RESULTS -> ResultsTabContent(viewModel)
                    AdminTab.SETTINGS -> SettingsTabContent(viewModel)
                }
            }
        }
    }
}

/* ========================================================================== */
/* 1. DASHBOARD TAB                                                          */
/* ========================================================================== */
@Composable
private fun DashboardTabContent(viewModel: AdminViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val stats = uiState.stats

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Visão Geral",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // KPI Stat Cards Grid (2x2)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "Total de Códigos",
                value = stats.totalCodes.toString(),
                color = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Códigos Disponíveis",
                value = stats.availableCodes.toString(),
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "Códigos Utilizados",
                value = stats.usedCodes.toString(),
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Giros Realizados",
                value = stats.totalSpins.toString(),
                color = Color(0xFF8B5CF6),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Prize Distribution Section
        Text(
            text = "Total de Cada Prêmio Distribuído",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        if (stats.prizeDistribution.isEmpty()) {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Nenhum prêmio distribuído ainda.",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stats.prizeDistribution.forEach { (prizeName, count) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = prizeName, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$count entregue(s)",
                                    color = Color(0xFF34D399),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Recent Spins Preview
        Text(
            text = "Últimos Giros Realizados",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        val recentSpins = uiState.spins.take(6)
        if (recentSpins.isEmpty()) {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Nenhum giro registrado.",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recentSpins.forEach { spin ->
                    SpinItemCard(spin = spin, onEditClient = null)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, fontSize = 12.sp, color = Color(0xFF94A3B8), maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

/* ========================================================================== */
/* 2. CAMPAIGNS TAB                                                           */
/* ========================================================================== */
@Composable
private fun CampaignsTabContent(viewModel: AdminViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingCampaign by remember { mutableStateOf<CampaignEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Campanhas Promocionais",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }

            items(uiState.campaigns) { campaign ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = campaign.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                            Surface(
                                color = if (campaign.active) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (campaign.active) "ATIVA" else "INATIVA",
                                    color = if (campaign.active) Color(0xFF34D399) else Color(0xFFF87171),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Link/Slug: /roleta/${campaign.slug}",
                            fontSize = 13.sp,
                            color = Color(0xFFF59E0B)
                        )

                        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        Text(
                            text = "Período: ${df.format(Date(campaign.startDate))} até ${df.format(Date(campaign.endDate))}",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(
                                onClick = { viewModel.setSelectedCampaign(campaign.id) },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFDE68A))
                            ) {
                                Text(if (uiState.selectedCampaignId == campaign.id) "Selecionada" else "Selecionar")
                            }
                            IconButton(onClick = {
                                editingCampaign = campaign
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF94A3B8))
                            }
                            IconButton(onClick = { viewModel.deleteCampaign(campaign) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        FloatingActionButton(
            onClick = {
                editingCampaign = null
                showDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_campaign_fab"),
            containerColor = Color(0xFFF59E0B),
            contentColor = Color(0xFF1E1B4B)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nova Campanha")
        }
    }

    if (showDialog) {
        CampaignEditDialog(
            campaign = editingCampaign,
            onDismiss = { showDialog = false },
            onSave = { newCamp ->
                viewModel.saveCampaign(newCamp)
                showDialog = false
            }
        )
    }
}

@Composable
private fun CampaignEditDialog(
    campaign: CampaignEntity?,
    onDismiss: () -> Unit,
    onSave: (CampaignEntity) -> Unit
) {
    var name by remember { mutableStateOf(campaign?.name ?: "") }
    var slug by remember { mutableStateOf(campaign?.slug ?: "") }
    var active by remember { mutableStateOf(campaign?.active ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Text(
                text = if (campaign == null) "Nova Campanha" else "Editar Campanha",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (campaign == null && slug.isBlank()) {
                            slug = it.lowercase().replace(" ", "-").replace(Regex("[^a-z0-9-]"), "")
                        }
                    },
                    label = { Text("Nome da Campanha") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = slug,
                    onValueChange = { slug = it.lowercase().trim() },
                    label = { Text("Slug do Link (ex: outubro)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = active,
                        onCheckedChange = { active = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (active) "Campanha Ativa" else "Campanha Inativa", color = Color.White)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    val target = campaign?.copy(
                        name = name.trim(),
                        slug = slug.trim().ifBlank { "promo-${System.currentTimeMillis() % 10000}" },
                        active = active
                    ) ?: CampaignEntity(
                        name = name.trim().ifBlank { "Nova Campanha" },
                        slug = slug.trim().ifBlank { "promo-${System.currentTimeMillis() % 10000}" },
                        startDate = now - 86400000,
                        endDate = now + (30L * 86400000),
                        active = active
                    )
                    onSave(target)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color(0xFF1E1B4B))
            ) {
                Text("SALVAR", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", color = Color(0xFF94A3B8)) }
        }
    )
}

/* ========================================================================== */
/* 3. PRIZES TAB                                                              */
/* ========================================================================== */
@Composable
private fun PrizesTabContent(viewModel: AdminViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingPrize by remember { mutableStateOf<PrizeEntity?>(null) }

    val campaignId = uiState.selectedCampaignId ?: uiState.campaigns.firstOrNull()?.id ?: 1L

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cadastro de Prêmios & Pesos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }

            if (uiState.prizes.isEmpty()) {
                item {
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Nenhum prêmio cadastrado nesta campanha. Clique no botão + para adicionar.",
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            items(uiState.prizes) { prize ->
                val isOutOfStock = !prize.unlimitedQuantity && prize.quantity <= 0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOutOfStock) Color(0xFF261E27) else Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color Indicator Circle
                        Surface(
                            shape = CircleShape,
                            color = try {
                                Color(android.graphics.Color.parseColor(prize.colorHex))
                            } catch (e: Exception) {
                                Color(0xFFF59E0B)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${prize.weight}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = prize.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (isOutOfStock) {
                                    Surface(
                                        color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "ESGOTADO",
                                            color = Color(0xFFF87171),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            if (prize.description.isNotBlank()) {
                                Text(
                                    text = prize.description,
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Peso/Prob.: ${prize.weight}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFDE68A)
                                )
                                Text(
                                    text = if (prize.unlimitedQuantity) "Estoque: Ilimitado" else "Estoque: ${prize.quantity}",
                                    fontSize = 12.sp,
                                    color = if (isOutOfStock) Color(0xFFF87171) else Color(0xFF34D399)
                                )
                            }
                        }

                        IconButton(onClick = {
                            editingPrize = prize
                            showDialog = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF94A3B8))
                        }
                        IconButton(onClick = { viewModel.deletePrize(prize) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color(0xFFEF4444))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        FloatingActionButton(
            onClick = {
                editingPrize = null
                showDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_prize_fab"),
            containerColor = Color(0xFFF59E0B),
            contentColor = Color(0xFF1E1B4B)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Novo Prêmio")
        }
    }

    if (showDialog) {
        PrizeEditDialog(
            prize = editingPrize,
            campaignId = campaignId,
            onDismiss = { showDialog = false },
            onSave = { newPrize ->
                viewModel.savePrize(newPrize)
                showDialog = false
            }
        )
    }
}

@Composable
private fun PrizeEditDialog(
    prize: PrizeEntity?,
    campaignId: Long,
    onDismiss: () -> Unit,
    onSave: (PrizeEntity) -> Unit
) {
    var name by remember { mutableStateOf(prize?.name ?: "") }
    var description by remember { mutableStateOf(prize?.description ?: "") }
    var weightStr by remember { mutableStateOf(prize?.weight?.toString() ?: "10") }
    var quantityStr by remember { mutableStateOf(prize?.quantity?.toString() ?: "100") }
    var unlimitedQuantity by remember { mutableStateOf(prize?.unlimitedQuantity ?: false) }
    var active by remember { mutableStateOf(prize?.active ?: true) }
    var selectedColor by remember { mutableStateOf(prize?.colorHex ?: "#10B981") }

    val presetColors = listOf("#10B981", "#2563EB", "#8B5CF6", "#F59E0B", "#EF4444", "#EC4899", "#06B6D4", "#F97316")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Text(
                text = if (prize == null) "Novo Prêmio" else "Editar Prêmio",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Prêmio (ex: 20% de desconto)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição / Condição") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = weightStr,
                        onValueChange = { weightStr = it },
                        label = { Text("Peso (Prob.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Quantidade") },
                        enabled = !unlimitedQuantity,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = unlimitedQuantity,
                        onCheckedChange = { unlimitedQuantity = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFF59E0B))
                    )
                    Text(text = "Quantidade Ilimitada", color = Color.White, fontSize = 13.sp)
                }

                // Palette selection
                Text(text = "Cor da fatia na roleta:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetColors.forEach { hex ->
                        val col = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == hex) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weight = weightStr.toIntOrNull() ?: 10
                    val qty = quantityStr.toIntOrNull() ?: 100
                    val target = prize?.copy(
                        name = name.trim().ifBlank { "Prêmio" },
                        description = description.trim(),
                        weight = weight.coerceAtLeast(1),
                        quantity = qty.coerceAtLeast(0),
                        unlimitedQuantity = unlimitedQuantity,
                        active = active,
                        colorHex = selectedColor
                    ) ?: PrizeEntity(
                        campaignId = campaignId,
                        name = name.trim().ifBlank { "Novo Prêmio" },
                        description = description.trim(),
                        weight = weight.coerceAtLeast(1),
                        quantity = qty.coerceAtLeast(0),
                        unlimitedQuantity = unlimitedQuantity,
                        active = active,
                        colorHex = selectedColor
                    )
                    onSave(target)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color(0xFF1E1B4B))
            ) {
                Text("SALVAR", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", color = Color(0xFF94A3B8)) }
        }
    )
}

/* ========================================================================== */
/* 4. ACCESS CODES TAB                                                        */
/* ========================================================================== */
@Composable
private fun CodesTabContent(viewModel: AdminViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showBulkDialog by remember { mutableStateOf(false) }
    var showSingleDialog by remember { mutableStateOf(false) }

    val campaignId = uiState.selectedCampaignId ?: uiState.campaigns.firstOrNull()?.id ?: 1L

    val filteredCodes = uiState.accessCodes.filter { code ->
        val matchesCampaign = code.campaignId == campaignId || uiState.campaigns.isEmpty()
        val matchesStatus = uiState.filterStatus == null || code.status == uiState.filterStatus
        val matchesSearch = uiState.searchQuery.isBlank() ||
                code.code.contains(uiState.searchQuery, ignoreCase = true) ||
                code.clientName.contains(uiState.searchQuery, ignoreCase = true)
        matchesCampaign && matchesStatus && matchesSearch
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Buscar código ou cliente...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("codes_search_field"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Button(
                onClick = { showBulkDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                modifier = Modifier.testTag("generate_bulk_codes_button")
            ) {
                Text("Gerar Lote", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Status Filter Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val filters = listOf(null to "Todos", AccessCodeEntity.STATUS_AVAILABLE to "Disponíveis", AccessCodeEntity.STATUS_USED to "Utilizados")
            filters.forEach { (status, label) ->
                FilterChip(
                    selected = uiState.filterStatus == status,
                    onClick = { viewModel.setStatusFilter(status) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFF59E0B),
                        selectedLabelColor = Color(0xFF1E1B4B)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Total: ${filteredCodes.size} código(s)",
            fontSize = 12.sp,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Codes List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredCodes) { codeItem ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = codeItem.code,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFFFDE68A)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (codeItem.status) {
                                        AccessCodeEntity.STATUS_AVAILABLE -> Color(0xFF10B981).copy(alpha = 0.2f)
                                        AccessCodeEntity.STATUS_USED -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                        else -> Color(0xFF6B7280).copy(alpha = 0.2f)
                                    }
                                ) {
                                    Text(
                                        text = codeItem.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (codeItem.status) {
                                            AccessCodeEntity.STATUS_AVAILABLE -> Color(0xFF34D399)
                                            AccessCodeEntity.STATUS_USED -> Color(0xFFFBBF24)
                                            else -> Color(0xFF9CA3AF)
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (codeItem.clientName.isNotBlank()) {
                                Text(
                                    text = "Cliente: ${codeItem.clientName}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                            if (codeItem.prizeName != null) {
                                Text(
                                    text = "Ganhou: ${codeItem.prizeName}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF34D399)
                                )
                            }
                        }

                        // Share / Copy code action for WhatsApp
                        IconButton(onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "🎉 Olá! Você ganhou uma tentativa na nossa Roleta da Sorte!\n\n" +
                                    "🔗 Acesse: https://ais-pre-hukgme3yaqxtd3avmqybqk-409086643827.us-east1.run.app\n" +
                                    "🎟️ Seu código exclusivo: ${codeItem.code}\n\n" +
                                    "Digite o código acima, gire a roleta e ganhe seu prêmio!"
                                )
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Enviar código"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartilhar", tint = Color(0xFF38BDF8))
                        }

                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Código Roleta", codeItem.code)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Código copiado: ${codeItem.code}", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }
    }

    if (showBulkDialog) {
        BulkGenerateCodesDialog(
            onDismiss = { showBulkDialog = false },
            onGenerate = { count ->
                viewModel.generateBulkCodes(campaignId, count)
                showBulkDialog = false
            }
        )
    }
}

@Composable
private fun BulkGenerateCodesDialog(
    onDismiss: () -> Unit,
    onGenerate: (Int) -> Unit
) {
    var countStr by remember { mutableStateOf("50") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Text(
                text = "Gerar Novos Códigos",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column {
                Text(
                    text = "Gera códigos aleatórios únicos (ex: RLT-7X92KP) para evitar adivinhações.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = countStr,
                    onValueChange = { countStr = it },
                    label = { Text("Quantidade de Códigos") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("10", "50", "100").forEach { q ->
                        OutlinedButton(
                            onClick = { countStr = q },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(q)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = (countStr.toIntOrNull() ?: 50).coerceIn(1, 500)
                    onGenerate(count)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color(0xFF1E1B4B))
            ) {
                Text("GERAR CÓDIGOS", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", color = Color(0xFF94A3B8)) }
        }
    )
}

/* ========================================================================== */
/* 5. RESULTS TAB                                                             */
/* ========================================================================== */
@Composable
private fun ResultsTabContent(viewModel: AdminViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var editingSpin by remember { mutableStateOf<SpinEntity?>(null) }

    val filteredSpins = uiState.spins.filter { spin ->
        val q = uiState.searchQuery
        q.isBlank() ||
                spin.code.contains(q, ignoreCase = true) ||
                spin.clientName.contains(q, ignoreCase = true) ||
                spin.prizeName.contains(q, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Pesquisar código, cliente ou prêmio...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("results_search_field"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // Export CSV Button
            Button(
                onClick = {
                    val csv = viewModel.exportSpinsCsv()
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, csv)
                        type = "text/csv"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Exportar Resultados CSV"))
                },
                modifier = Modifier.testTag("export_csv_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("CSV", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Mostrando ${filteredSpins.size} resultado(s). Toque em um item para identificar o cliente.",
            fontSize = 12.sp,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredSpins) { spin ->
                SpinItemCard(
                    spin = spin,
                    onEditClient = { editingSpin = spin }
                )
            }
        }
    }

    if (editingSpin != null) {
        EditClientObservationDialog(
            spin = editingSpin!!,
            onDismiss = { editingSpin = null },
            onSave = { client, obs ->
                viewModel.updateSpinDetails(editingSpin!!.id, client, obs)
                editingSpin = null
            }
        )
    }
}

@Composable
private fun SpinItemCard(
    spin: SpinEntity,
    onEditClient: (() -> Unit)?
) {
    val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onEditClient != null) { onEditClient?.invoke() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = spin.code,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFFFDE68A)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${df.format(Date(spin.createdAt))}",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Prêmio: ${spin.prizeName}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF34D399)
                )

                Text(
                    text = "Cliente: ${spin.clientName}",
                    fontSize = 13.sp,
                    color = Color.White
                )

                if (spin.observation.isNotBlank()) {
                    Text(
                        text = "Obs: ${spin.observation}",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onEditClient != null) {
                IconButton(onClick = onEditClient) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Identificar Cliente",
                        tint = Color(0xFF38BDF8)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditClientObservationDialog(
    spin: SpinEntity,
    onDismiss: () -> Unit,
    onSave: (clientName: String, observation: String) -> Unit
) {
    var clientName by remember { mutableStateOf(if (spin.clientName == "Não informado") "" else spin.clientName) }
    var observation by remember { mutableStateOf(spin.observation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Text(
                text = "Identificar Cliente",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Código: ${spin.code}", color = Color(0xFFFDE68A), fontWeight = FontWeight.Bold)
                        Text(text = "Prêmio Sorteado: ${spin.prizeName}", color = Color(0xFF34D399), fontSize = 13.sp)
                    }
                }

                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Nome do Cliente") },
                    placeholder = { Text("Ex: João da Silva") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = observation,
                    onValueChange = { observation = it },
                    label = { Text("Observação") },
                    placeholder = { Text("Ex: Cliente da loja física, resgatou em 02/10") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(clientName, observation) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color(0xFF1E1B4B))
            ) {
                Text("SALVAR", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", color = Color(0xFF94A3B8)) }
        }
    )
}

/* ========================================================================== */
/* 6. SETTINGS TAB                                                            */
/* ========================================================================== */
@Composable
private fun SettingsTabContent(viewModel: AdminViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var newPin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Configurações do Sistema",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Segurança & PIN Administrativo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "PIN atual: ${uiState.adminPin}. Digite um novo PIN abaixo caso deseje alterar.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it },
                        placeholder = { Text("Novo PIN (ex: 4321)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Button(
                        onClick = {
                            viewModel.updatePin(newPin)
                            newPin = ""
                        },
                        enabled = newPin.length >= 4,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        Text("Atualizar")
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Dados de Demonstração / Testes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Recarrega campanha, prêmios padrão e códigos de demonstração caso você queira testar novamente a roleta.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.resetDemoData() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color(0xFF1E1B4B))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recarregar Dados de Exemplo")
                }
            }
        }
    }
}
