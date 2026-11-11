package com.auraface.auraface_app.presentation.student

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auraface.auraface_app.data.network.model.RedeemedRewardOut
import com.auraface.auraface_app.data.network.model.RewardItemOut
import com.auraface.auraface_app.data.network.model.RedeemRequest
import com.auraface.auraface_app.data.network.model.GamificationProfile
import com.auraface.auraface_app.data.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val repo: QuizRepository
) : ViewModel() {

    var profile by mutableStateOf<GamificationProfile?>(null)
        private set
        
    var rewards by mutableStateOf<List<RewardItemOut>>(emptyList())
        private set

    var myRewards by mutableStateOf<List<RedeemedRewardOut>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            isLoading = true
            try {
                // Fetch Profile for XP
                val profileRes = repo.getProfile()
                if (profileRes.isSuccessful) {
                    profile = profileRes.body()
                }

                // Fetch Available Rewards
                val rewardsRes = repo.getRewards()
                if (rewardsRes.isSuccessful) {
                    rewards = rewardsRes.body() ?: emptyList()
                }

                // Fetch My Redeemed Rewards
                val myRewardsRes = repo.getMyRewards()
                if (myRewardsRes.isSuccessful) {
                    myRewards = myRewardsRes.body() ?: emptyList()
                }
            } catch (e: Exception) {
                error = "Failed to load rewards: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun redeemReward(rewardId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = repo.redeemReward(RedeemRequest(rewardId))
                if (res.isSuccessful) {
                    onSuccess()
                    loadData() // Refresh data to show updated XP & lists
                } else {
                    val code = res.code()
                    onError(if (code == 400) "Not enough XP or Out of Stock" else "Failed to redeem")
                }
            } catch (e: Exception) {
                onError("Network error")
            }
        }
    }
}
