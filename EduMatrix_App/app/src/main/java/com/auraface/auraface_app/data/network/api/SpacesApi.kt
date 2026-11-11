package com.auraface.auraface_app.data.network.api

import com.auraface.auraface_app.data.network.model.BookingRequest
import com.auraface.auraface_app.data.network.model.BookingResponse
import com.auraface.auraface_app.data.network.model.MyBookingOut
import com.auraface.auraface_app.data.network.model.SpaceItem
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@JvmSuppressWildcards
interface SpacesApi {
    @GET("spaces/list")
    suspend fun getSpaces(): Response<List<SpaceItem>>

    @POST("spaces/book")
    suspend fun bookSpace(@Body request: BookingRequest): Response<BookingResponse>

    @GET("spaces/my-bookings")
    suspend fun getMyBookings(): Response<List<MyBookingOut>>

    @GET("spaces/library/seats")
    suspend fun getLibrarySeats(): Response<List<com.auraface.auraface_app.data.remote.dto.LibrarySeatDto>>

    @POST("spaces/library/book-seat")
    suspend fun bookLibrarySeat(@Body req: Map<String, Any>): Response<com.auraface.auraface_app.data.remote.dto.LibraryBookingResponseDto>
}
