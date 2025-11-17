package ar.edu.itba.listapp.data.model

import kotlinx.serialization.Serializable

// Shopping List models
@Serializable
data class ShoppingList(
    val id: Long,
    val name: String,
    val metadata: Map<String, String>? = null,
    val createdAt: String,
    val updatedAt: String,
    val owner: Owner? = null,
    val sharedWith: List<Owner> = emptyList()
)

@Serializable
data class CreateShoppingListRequest(
    val name: String,
    val description: String = "",
    val recurring: Boolean = true,
    val metadata: Map<String, String>? = null
)

@Serializable
data class UpdateShoppingListRequest(
    val name: String,
    val metadata: Map<String, String>? = null
)

@Serializable
data class ShoppingListsResponse(
    val data: List<ShoppingList>,
    val pagination: Pagination
)

@Serializable
data class ShareShoppingListRequest(
    val email: String
)

// Shopping List Item models
@Serializable
data class ShoppingListItem(
    val id: Long,
    val quantity: Double,
    val unit: String? = null,
    val metadata: Map<String, String>? = null,
    val product: Product,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateShoppingListItemRequest(
    val product: ProductReference,
    val quantity: Double,
    val unit: String? = null,
    val metadata: Map<String, String>? = null
)

@Serializable
data class UpdateShoppingListItemRequest(
    val quantity: Double,
    val unit: String? = null,
    val metadata: Map<String, String>? = null
)

@Serializable
data class ShoppingListItemsResponse(
    val data: List<ShoppingListItem>,
    val pagination: Pagination
)
