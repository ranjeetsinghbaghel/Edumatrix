package com.auraface.auraface_app.presentation.student

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auraface.auraface_app.data.network.api.StudentApi
import com.auraface.auraface_app.data.remote.dto.MovieEventDto
import com.auraface.auraface_app.data.remote.dto.SportsTournamentDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EntertainmentViewModel @Inject constructor(
    private val api: StudentApi
) : ViewModel() {

    var movies by mutableStateOf<List<MovieEventDto>>(emptyList())
        private set

    var tournaments by mutableStateOf<List<SportsTournamentDto>>(emptyList())
        private set

    var isLoadingMovies by mutableStateOf(false)
        private set
    var moviesError by mutableStateOf<String?>(null)
        private set
    var moviesMessage by mutableStateOf<String?>(null)
        private set

    var isLoadingTournaments by mutableStateOf(false)
        private set
    var tournamentsError by mutableStateOf<String?>(null)
        private set

    fun getMovies() {
        viewModelScope.launch {
            isLoadingMovies = true
            moviesError = null
            try {
                movies = api.getMovies()
            } catch (e: Exception) {
                moviesError = "Failed to load movies: ${e.message}"
            } finally {
                isLoadingMovies = false
            }
        }
    }

    fun bookMovie(movieId: Int) {
        viewModelScope.launch {
            moviesMessage = null
            try {
                api.bookMovie(mapOf("movie_id" to movieId, "seats_booked" to 1))
                moviesMessage = "Booking confirmed!"
                getMovies() // refresh
            } catch (e: Exception) {
                moviesMessage = "Failed to book: ${e.message}"
            }
        }
    }

    fun getTournaments() {
        viewModelScope.launch {
            isLoadingTournaments = true
            tournamentsError = null
            try {
                tournaments = api.getSportsTournaments()
            } catch (e: Exception) {
                tournamentsError = "Failed to load tournaments: ${e.message}"
            } finally {
                isLoadingTournaments = false
            }
        }
    }
}
