package ar.edu.itba.listapp.data.network

import ar.edu.itba.listapp.data.model.*
import retrofit2.http.*

interface ListService {

    // Shopping Lists endpoints
    @GET("shopping-lists")
    suspend fun getShoppingLists(
        @Query("owner") owner: Boolean? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10,
        @Query("sort_by") sortBy: String? = null,
        @Query("order") order: String? = null
    ): ShoppingListsResponse

    @POST("shopping-lists")
    suspend fun createShoppingList(@Body body: CreateShoppingListRequest): ShoppingList

    @GET("shopping-lists/{id}")
    suspend fun getShoppingList(@Path("id") id: Long): ShoppingList

    @PUT("shopping-lists/{id}")
    suspend fun updateShoppingList(
        @Path("id") id: Long,
        @Body body: UpdateShoppingListRequest
    ): ShoppingList

    @DELETE("shopping-lists/{id}")
    suspend fun deleteShoppingList(@Path("id") id: Long)

    @POST("shopping-lists/{id}/share")
    suspend fun shareShoppingList(
        @Path("id") id: Long,
        @Body body: ShareShoppingListRequest
    ): Owner

    @GET("shopping-lists/{id}/shared-with")
    suspend fun getSharedUsers(@Path("id") id: Long): List<Owner>

    @DELETE("shopping-lists/{id}/share/{email}")
    suspend fun unshareShoppingList(
        @Path("id") id: Long,
        @Path("email") email: String
    )

    // Shopping List Items endpoints
    @GET("shopping-lists/{id}/items")
    suspend fun getShoppingListItems(
        @Path("id") id: Long,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100,
        @Query("sort_by") sortBy: String? = null,
        @Query("order") order: String? = null
    ): ShoppingListItemsResponse

    @POST("shopping-lists/{id}/items")
    suspend fun addShoppingListItem(
        @Path("id") id: Long,
        @Body body: CreateShoppingListItemRequest
    ): ShoppingListItem

    @GET("shopping-lists/{listId}/items/{itemId}")
    suspend fun getShoppingListItem(
        @Path("listId") listId: Long,
        @Path("itemId") itemId: Long
    ): ShoppingListItem

    @PUT("shopping-lists/{listId}/items/{itemId}")
    suspend fun updateShoppingListItem(
        @Path("listId") listId: Long,
        @Path("itemId") itemId: Long,
        @Body body: UpdateShoppingListItemRequest
    ): ShoppingListItem

    @DELETE("shopping-lists/{listId}/items/{itemId}")
    suspend fun deleteShoppingListItem(
        @Path("listId") listId: Long,
        @Path("itemId") itemId: Long
    )
}
