package com.auraface.auraface_app.presentation.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.auraface.auraface_app.data.network.model.RewardItemOut
import kotlinx.coroutines.launch
import com.auraface.auraface_app.data.network.model.RedeemedRewardOut

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsStoreScreen(
    navController: NavController,
    viewModel: RewardsViewModel = hiltViewModel()
) {
    val profile = viewModel.profile
    val rewards = viewModel.rewards
    val myRewards = viewModel.myRewards
    val isLoading = viewModel.isLoading
    val error = viewModel.error
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Rewards Store", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4F46E5),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
        ) {
            // Header showing XP
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFF4F46E5),
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Star, contentDescription = "XP", modifier = Modifier.size(48.dp), tint = Color(0xFFFFD700))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${profile?.total_xp ?: 0} XP", 
                        style = MaterialTheme.typography.headlineMedium, 
                        fontWeight = FontWeight.ExtraBold, 
                        color = Color.White
                    )
                    Text("Available Balance", color = Color.White.copy(alpha = 0.8f))
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF4F46E5),
                modifier = Modifier.padding(16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Available Rewards", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("My Inventory", fontWeight = FontWeight.SemiBold) }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF4F46E5))
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error, color = Color.Red)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (selectedTab == 0) {
                        items(rewards) { reward ->
                            RewardCard(
                                reward = reward,
                                isAffordable = (profile?.total_xp ?: 0) >= reward.xp_cost,
                                onRedeem = { id ->
                                    viewModel.redeemReward(
                                        rewardId = id,
                                        onSuccess = {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Reward claimed successfully!")
                                            }
                                        },
                                        onError = { msg ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Error: $msg")
                                            }
                                        }
                                    )
                                }
                            )
                        }
                    } else {
                        if (myRewards.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("You haven't claimed any rewards yet.", color = Color.Gray)
                                }
                            }
                        } else {
                            items(myRewards) { myReward ->
                                RedeemedRewardCard(myReward)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RewardCard(
    reward: RewardItemOut,
    isAffordable: Boolean,
    onRedeem: (Int) -> Unit
) {
    // Basic mapping for strings to icons
    val icon = when (reward.icon_name.lowercase()) {
        "description" -> Icons.Default.Description
        "mic" -> Icons.Default.Mic
        "work" -> Icons.Default.Work
        "coffee" -> Icons.Default.LocalCafe
        else -> Icons.Default.Star
    }

    val parsedColor = try {
        Color(android.graphics.Color.parseColor(reward.bg_color.replace("0x", "#")))
    } catch (e: Exception) {
        Color(0xFFF1F5F9) // Fallback
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(parsedColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.Black.copy(0.7f), modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(reward.title, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Text(reward.description, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${reward.xp_cost} XP", 
                        fontWeight = FontWeight.Bold, 
                        color = if (isAffordable) Color(0xFF10B981) else Color.Red
                    )
                }
            }
            
            Button(
                onClick = { onRedeem(reward.id) },
                enabled = isAffordable && reward.is_active && (reward.stock != 0),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5),
                    disabledContainerColor = Color.LightGray
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(if (reward.stock == 0) "Sold Out" else "Claim")
            }
        }
    }
}

@Composable
fun RedeemedRewardCard(redeemed: RedeemedRewardOut) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reward Claimed", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.weight(1f))
                Text(redeemed.status, color = Color(0xFFF59E0B), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Reward ID: ${redeemed.reward_id}", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            Text("Date: ${redeemed.redeemed_at}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}
