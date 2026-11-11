package com.auraface.auraface_app.presentation.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.auraface.auraface_app.data.network.RetrofitClient
import com.auraface.auraface_app.data.remote.dto.MovieEventDto
import com.auraface.auraface_app.data.remote.dto.SportsTournamentDto
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusMoviesScreen(
    navController: NavController,
    viewModel: EntertainmentViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.getMovies()
    }

    val movies = viewModel.movies
    val isLoading = viewModel.isLoadingMovies
    val error = viewModel.moviesError
    val message = viewModel.moviesMessage

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campus Movies", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFEF4444),
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
            if (message != null) {
                Text(
                    text = message!!,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF10B981))
                        .padding(8.dp)
                )
            }
            if (error != null) {
                Text(text = error!!, color = Color.Red, modifier = Modifier.padding(16.dp))
            } else if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else if (movies.isEmpty()) {
                Text("No upcoming movies or events.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(movies) { movie ->
                        MovieCard(movie = movie, onBookClick = {
                            viewModel.bookMovie(movie.id)
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun MovieCard(movie: MovieEventDto, onBookClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            if (!movie.poster_url.isNullOrBlank()) {
                AsyncImage(
                    model = RetrofitClient.BASE_URL + movie.poster_url,
                    contentDescription = movie.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFF94A3B8)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                }
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = movie.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                if (!movie.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = movie.description, fontSize = 14.sp, color = Color(0xFF64748B))
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF3B82F6))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = movie.show_time, fontSize = 12.sp, color = Color(0xFF1E293B))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = movie.venue, fontSize = 12.sp, color = Color(0xFF1E293B))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Chair, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${movie.available_seats ?: 0} spots left", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onBookClick,
                    enabled = (movie.available_seats ?: 0) > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if ((movie.available_seats ?: 0) > 0) "Book a Seat" else "House Full", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusSportsScreen(
    navController: NavController,
    viewModel: EntertainmentViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.getTournaments()
    }

    val tournaments = viewModel.tournaments
    val isLoading = viewModel.isLoadingTournaments
    val error = viewModel.tournamentsError

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EduMatrix Sports", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF59E0B),
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
            if (error != null) {
                Text(text = error!!, color = Color.Red, modifier = Modifier.padding(16.dp))
            } else if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else if (tournaments.isEmpty()) {
                Text("No active sports tournaments right now.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(tournaments) { tourney ->
                        TournamentCard(tourney)
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentCard(tournament: SportsTournamentDto) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFFBEB)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = tournament.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SportsBasketball, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF64748B))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = tournament.sport_type, fontSize = 12.sp, color = Color(0xFF64748B))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF64748B))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${tournament.start_date} to ${tournament.end_date}", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }
        }
    }
}
