package ar.edu.itba.listapp.data.network

import android.content.Context
import ar.edu.itba.listapp.R
import ar.edu.itba.listapp.data.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

// Result types for Shopping Lists
sealed interface GetShoppingListsResult {
    data class Success(val lists: List<ShoppingList>, val pagination: Pagination) : GetShoppingListsResult
    data class Error(val message: String) : GetShoppingListsResult
}

sealed interface CreateShoppingListResult {
    data class Success(val list: ShoppingList) : CreateShoppingListResult
    data class Error(val message: String) : CreateShoppingListResult
}

sealed interface GetShoppingListResult {
    data class Success(val list: ShoppingList) : GetShoppingListResult
    data class Error(val message: String) : GetShoppingListResult
}

sealed interface UpdateShoppingListResult {
    data class Success(val list: ShoppingList) : UpdateShoppingListResult
    data class Error(val message: String) : UpdateShoppingListResult
}

sealed interface DeleteShoppingListResult {
    data object Success : DeleteShoppingListResult
    data class Error(val message: String) : DeleteShoppingListResult
}

sealed interface ShareShoppingListResult {
    data class Success(val user: Owner) : ShareShoppingListResult
    data class Error(val message: String) : ShareShoppingListResult
}

sealed interface GetListSharedUsersResult {
    data class Success(val users: List<Owner>) : GetListSharedUsersResult
    data class Error(val message: String) : GetListSharedUsersResult
}

sealed interface UnshareShoppingListResult {
    data object Success : UnshareShoppingListResult
    data class Error(val message: String) : UnshareShoppingListResult
}

// Result types for Shopping List Items
sealed interface GetShoppingListItemsResult {
    data class Success(val items: List<ShoppingListItem>, val pagination: Pagination) : GetShoppingListItemsResult
    data class Error(val message: String) : GetShoppingListItemsResult
}

sealed interface AddShoppingListItemResult {
    data class Success(val item: ShoppingListItem) : AddShoppingListItemResult
    data class Error(val message: String) : AddShoppingListItemResult
}

sealed interface GetShoppingListItemResult {
    data class Success(val item: ShoppingListItem) : GetShoppingListItemResult
    data class Error(val message: String) : GetShoppingListItemResult
}

sealed interface UpdateShoppingListItemResult {
    data class Success(val item: ShoppingListItem) : UpdateShoppingListItemResult
    data class Error(val message: String) : UpdateShoppingListItemResult
}

sealed interface DeleteShoppingListItemResult {
    data object Success : DeleteShoppingListItemResult
    data class Error(val message: String) : DeleteShoppingListItemResult
}

class ListRepository(
    private val context: Context,
    private val service: ListService = NetworkModule.listService,
    private val sessionManager: SessionManager,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    init {
        // Provide token on each request
        NetworkModule.setAuthTokenProvider { sessionManager.loadAuthToken() }
    }

    // Shopping List operations
    suspend fun getShoppingLists(
        owner: Boolean? = null,
        page: Int = 1,
        perPage: Int = 10,
        sortBy: String? = null,
        order: String? = null
    ): GetShoppingListsResult = withContext(dispatcher) {
        try {
            val response = service.getShoppingLists(owner, page, perPage, sortBy, order)
            GetShoppingListsResult.Success(response.data, response.pagination)
        } catch (httpException: HttpException) {
            val message = when (httpException.code()) {
                400 -> context.getString(R.string.shopping_list_error_bad_request)
                401 -> context.getString(R.string.shopping_list_error_unauthorized)
                500 -> context.getString(R.string.shopping_list_error_server)
                else -> context.getString(R.string.shopping_list_error_unexpected, httpException.code())
            }
            GetShoppingListsResult.Error(message)
        } catch (ioException: IOException) {
            val message = context.getString(R.string.shopping_list_error_connection)
            GetShoppingListsResult.Error(message)
        } catch (exception: Exception) {
            val message = context.getString(R.string.shopping_list_error_generic, exception.message ?: "")
            GetShoppingListsResult.Error(message)
        }
    }

    suspend fun createShoppingList(name: String, metadata: Map<String, String> = emptyMap()): CreateShoppingListResult =
        withContext(dispatcher) {
            try {
                val request = CreateShoppingListRequest(
                    name = name,
                    description = "",
                    recurring = true,
                    metadata = metadata
                )
                val list = service.createShoppingList(request)
                CreateShoppingListResult.Success(list)
            } catch (httpException: HttpException) {
                val message = when (httpException.code()) {
                    400 -> context.getString(R.string.shopping_list_error_bad_request)
                    401 -> context.getString(R.string.shopping_list_error_unauthorized)
                    409 -> context.getString(R.string.shopping_list_error_conflict)
                    500 -> context.getString(R.string.shopping_list_error_server)
                    else -> context.getString(R.string.shopping_list_error_unexpected, httpException.code())
                }
                CreateShoppingListResult.Error(message)
            } catch (ioException: IOException) {
                val message = context.getString(R.string.shopping_list_error_connection)
                CreateShoppingListResult.Error(message)
            } catch (exception: Exception) {
                val message = context.getString(R.string.shopping_list_error_generic, exception.message ?: "")
                CreateShoppingListResult.Error(message)
            }
        }

    suspend fun getShoppingList(id: Long): GetShoppingListResult = withContext(dispatcher) {
        try {
            val list = service.getShoppingList(id)
            GetShoppingListResult.Success(list)
        } catch (httpException: HttpException) {
            val message = when (httpException.code()) {
                400 -> context.getString(R.string.shopping_list_error_bad_request)
                401 -> context.getString(R.string.shopping_list_error_unauthorized)
                404 -> context.getString(R.string.shopping_list_error_not_found)
                500 -> context.getString(R.string.shopping_list_error_server)
                else -> context.getString(R.string.shopping_list_error_unexpected, httpException.code())
            }
            GetShoppingListResult.Error(message)
        } catch (ioException: IOException) {
            val message = context.getString(R.string.shopping_list_error_connection)
            GetShoppingListResult.Error(message)
        } catch (exception: Exception) {
            val message = context.getString(R.string.shopping_list_error_generic, exception.message ?: "")
            GetShoppingListResult.Error(message)
        }
    }

    suspend fun updateShoppingList(id: Long, name: String, metadata: Map<String, String> = emptyMap()): UpdateShoppingListResult =
        withContext(dispatcher) {
            try {
                val request = UpdateShoppingListRequest(name, metadata)
                val list = service.updateShoppingList(id, request)
                UpdateShoppingListResult.Success(list)
            } catch (httpException: HttpException) {
                val message = when (httpException.code()) {
                    400 -> context.getString(R.string.shopping_list_error_bad_request)
                    401 -> context.getString(R.string.shopping_list_error_unauthorized)
                    404 -> context.getString(R.string.shopping_list_error_not_found)
                    500 -> context.getString(R.string.shopping_list_error_server)
                    else -> context.getString(R.string.shopping_list_error_unexpected, httpException.code())
                }
                UpdateShoppingListResult.Error(message)
            } catch (ioException: IOException) {
                val message = context.getString(R.string.shopping_list_error_connection)
                UpdateShoppingListResult.Error(message)
            } catch (exception: Exception) {
                val message = context.getString(R.string.shopping_list_error_generic, exception.message ?: "")
                UpdateShoppingListResult.Error(message)
            }
        }

    suspend fun deleteShoppingList(id: Long): DeleteShoppingListResult = withContext(dispatcher) {
        try {
            service.deleteShoppingList(id)
            DeleteShoppingListResult.Success
        } catch (httpException: HttpException) {
            val message = when (httpException.code()) {
                400 -> context.getString(R.string.shopping_list_error_bad_request)
                401 -> context.getString(R.string.shopping_list_error_unauthorized)
                404 -> context.getString(R.string.shopping_list_error_not_found)
                500 -> context.getString(R.string.shopping_list_error_server)
                else -> context.getString(R.string.shopping_list_error_unexpected, httpException.code())
            }
            DeleteShoppingListResult.Error(message)
        } catch (ioException: IOException) {
            val message = context.getString(R.string.shopping_list_error_connection)
            DeleteShoppingListResult.Error(message)
        } catch (exception: Exception) {
            val message = context.getString(R.string.shopping_list_error_generic, exception.message ?: "")
            DeleteShoppingListResult.Error(message)
        }
    }

    suspend fun shareShoppingList(id: Long, email: String): ShareShoppingListResult = withContext(dispatcher) {
        try {
            val request = ShareShoppingListRequest(email)
            val user = service.shareShoppingList(id, request)
            ShareShoppingListResult.Success(user)
        } catch (httpException: HttpException) {
            val message = when (httpException.code()) {
                400 -> context.getString(R.string.shopping_list_error_bad_request)
                401 -> context.getString(R.string.shopping_list_error_unauthorized)
                404 -> context.getString(R.string.shopping_list_error_not_found)
                500 -> context.getString(R.string.shopping_list_error_server)
                else -> context.getString(R.string.shopping_list_error_unexpected, httpException.code())
            }
            ShareShoppingListResult.Error(message)
        } catch (ioException: IOException) {
            val message = context.getString(R.string.shopping_list_error_connection)
            ShareShoppingListResult.Error(message)
        } catch (exception: Exception) {
            val message = context.getString(R.string.shopping_list_error_generic, exception.message ?: "")
            ShareShoppingListResult.Error(message)
        }
    }

    suspend fun getSharedUsers(id: Long): GetSharedUsersResult = withContext(dispatcher) {
        try {
            val users = service.getSharedUsers(id)
            GetSharedUsersResult.Success(users)
        } catch (httpException: HttpException) {
            val message = when (httpException.code()) {
                400 -> context.getString(R.string.shopping_list_error_bad_request)
                401 -> context.getString(R.string.shopping_list_error_unauthorized)
                404 -> context.getString(R.string.shopping_list_error_not_found)
                500 -> context.getString(R.string.shopping_list_error_server)
                else -> context.getString(R.string.shopping_list_error_unexpected, httpException.code())
            }
            GetSharedUsersResult.Error(message)
        } catch (ioException: IOException) {
            val message = context.getString(R.string.shopping_list_error_connection)
            GetSharedUsersResult.Error(message)
        } catch (exception: Exception) {
            val message = context.getString(R.string.shopping_list_error_generic, exception.message ?: "")
            GetSharedUsersResult.Error(message)
        }
    }

    suspend fun unshareShoppingList(id: Long, email: String): UnshareShoppingListResult = withContext(dispatcher) {
        try {
            service.unshareShoppingList(id, email)
            UnshareShoppingListResult.Success
        } catch (httpException: HttpException) {
            val message = when (httpException.code()) {
                400 -> context.getString(R.string.shopping_list_error_bad_request)
                401 -> context.getString(R.string.shopping_list_error_unauthorized)
                404 -> context.getString(R.string.shopping_list_error_not_found)
                500 -> context.getString(R.string.shopping_list_error_server)
                else -> context.getString(R.string.shopping_list_error_unexpected, httpException.code())
            }
            UnshareShoppingListResult.Error(message)
        } catch (ioException: IOException) {
            val message = context.getString(R.string.shopping_list_error_connection)
            UnshareShoppingListResult.Error(message)
        } catch (exception: Exception) {
            val message = context.getString(R.string.shopping_list_error_generic, exception.message ?: "")
            UnshareShoppingListResult.Error(message)
        }
    }

    // Shopping List Items operations
    suspend fun getShoppingListItems(
        listId: Long,
        page: Int = 1,
        perPage: Int = 100,
        sortBy: String? = null,
        order: String? = null
    ): GetShoppingListItemsResult = withContext(dispatcher) {
        try {
            val response = service.getShoppingListItems(listId, page, perPage, sortBy, order)
            GetShoppingListItemsResult.Success(response.data, response.pagination)
        } catch (httpException: HttpException) {
            val message = when (httpException.code()) {
                400 -> context.getString(R.string.shopping_list_error_bad_request)
                401 -> context.getString(R.string.shopping_list_error_unauthorized)
                404 -> context.getString(R.string.shopping_list_error_not_found)
                500 -> context.getString(R.string.shopping_list_error_server)
                else -> context.getString(R.string.shopping_list_error_unexpected, httpException.code())
            }
            GetShoppingListItemsResult.Error(message)
        } catch (ioException: IOException) {
            val message = context.getString(R.string.shopping_list_error_connection)
            GetShoppingListItemsResult.Error(message)
        } catch (exception: Exception) {
            val message = context.getString(R.string.shopping_list_error_generic, exception.message ?: "")
            GetShoppingListItemsResult.Error(message)
        }
    }

    suspend fun addShoppingListItem(listId: Long, productId: Long, quantity: Double = 1.0, unit: String? = null, metadata: Map<String, String>? = null): AddShoppingListItemResult =
        withContext(dispatcher) {
            try {
                val request = CreateShoppingListItemRequest(ProductReference(productId), quantity, unit, metadata)
                val item = service.addShoppingListItem(listId, request)
                AddShoppingListItemResult.Success(item)
            } catch (httpException: HttpException) {
                val message = when (httpException.code()) {
                    400 -> context.getString(R.string.shopping_list_error_bad_request)
                    401 -> context.getString(R.string.shopping_list_error_unauthorized)
                    404 -> context.getString(R.string.shopping_list_error_not_found)
                    500 -> context.getString(R.string.shopping_list_error_server)
                    else -> context.getString(R.string.shopping_list_error_unexpected, httpException.code())
                }
                AddShoppingListItemResult.Error(message)
            } catch (ioException: IOException) {
                val message = context.getString(R.string.shopping_list_error_connection)
                AddShoppingListItemResult.Error(message)
            } catch (exception: Exception) {
                val message = context.getString(R.string.shopping_list_error_generic, exception.message ?: "")
                AddShoppingListItemResult.Error(message)
            }
        }

    suspend fun updateShoppingListItem(listId: Long, itemId: Long, quantity: Double, unit: String? = null, metadata: Map<String, String>? = null): UpdateShoppingListItemResult =
        withContext(dispatcher) {
            try {
                val request = UpdateShoppingListItemRequest(quantity, unit, metadata)
                val item = service.updateShoppingListItem(listId, itemId, request)
                UpdateShoppingListItemResult.Success(item)
            } catch (httpException: HttpException) {
                val message = when (httpException.code()) {
                    400 -> context.getString(R.string.shopping_list_error_bad_request)
                    401 -> context.getString(R.string.shopping_list_error_unauthorized)
                    404 -> context.getString(R.string.shopping_list_error_not_found)
                    500 -> context.getString(R.string.shopping_list_error_server)
                    else -> context.getString(R.string.shopping_list_error_unexpected, httpException.code())
                }
                UpdateShoppingListItemResult.Error(message)
            } catch (ioException: IOException) {
                val message = context.getString(R.string.shopping_list_error_connection)
                UpdateShoppingListItemResult.Error(message)
            } catch (exception: Exception) {
                val message = context.getString(R.string.shopping_list_error_generic, exception.message ?: "")
                UpdateShoppingListItemResult.Error(message)
            }
        }

    suspend fun deleteShoppingListItem(listId: Long, itemId: Long): DeleteShoppingListItemResult = withContext(dispatcher) {
        try {
            service.deleteShoppingListItem(listId, itemId)
            DeleteShoppingListItemResult.Success
        } catch (httpException: HttpException) {
            val message = when (httpException.code()) {
                400 -> context.getString(R.string.shopping_list_error_bad_request)
                401 -> context.getString(R.string.shopping_list_error_unauthorized)
                404 -> context.getString(R.string.shopping_list_error_not_found)
                500 -> context.getString(R.string.shopping_list_error_server)
                else -> context.getString(R.string.shopping_list_error_unexpected, httpException.code())
            }
            DeleteShoppingListItemResult.Error(message)
        } catch (ioException: IOException) {
            val message = context.getString(R.string.shopping_list_error_connection)
            DeleteShoppingListItemResult.Error(message)
        } catch (exception: Exception) {
            val message = context.getString(R.string.shopping_list_error_generic, exception.message ?: "")
            DeleteShoppingListItemResult.Error(message)
        }
    }
}
