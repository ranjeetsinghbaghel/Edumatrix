package com.auraface.auraface_app.data.repository

import com.auraface.auraface_app.data.network.api.SpacesApi
import com.auraface.auraface_app.data.network.model.BookingRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpacesRepository @Inject constructor(
    private val api: SpacesApi
) {
    suspend fun getSpaces() = api.getSpaces()
    suspend fun bookSpace(request: BookingRequest) = api.bookSpace(request)
    suspend fun getMyBookings() = api.getMyBookings()
}
