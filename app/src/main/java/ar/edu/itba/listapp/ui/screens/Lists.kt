package ar.edu.itba.listapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.itba.listapp.R
import ar.edu.itba.listapp.data.network.*
import ar.edu.itba.listapp.ui.composables.AddProductsToListDialog
import ar.edu.itba.listapp.ui.composables.CollapsibleList
import ar.edu.itba.listapp.ui.composables.NewShoppingListForm
import ar.edu.itba.listapp.ui.composables.NoItemsMessage
import ar.edu.itba.listapp.ui.composables.SearchBar
import ar.edu.itba.listapp.ui.theme.ListappTheme
import ar.edu.itba.listapp.ui.utils.isTablet
import kotlinx.coroutines.launch

private data class ListItemUI(var id: Long, var emoji: String, var name: String)
private data class ShoppingListUI(var id: Long, var title: String, val items: MutableList<ListItemUI>)

@Composable
fun ListsScreen(scaffoldPadding: PaddingValues) {
    val context = LocalContext.current
    val listRepository = remember { ListRepository(context, sessionManager = SessionManager(context)) }
    val productRepository = remember { ProductRepository(context, sessionManager = SessionManager(context)) }
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    var isLoading by remember { mutableStateOf(true) }
    var searchText by remember { mutableStateOf("") }

    // Shopping Lists and dialog states
    val shoppingLists = remember { mutableStateListOf<ShoppingListUI>() }
    var showNewListDialog by remember { mutableStateOf(false) }
    var showAddProductsDialog by remember { mutableStateOf<ShoppingListUI?>(null) }
    var availableCategories by remember { mutableStateOf(emptyList<ar.edu.itba.listapp.data.model.Category>()) }
    var availableProducts by remember { mutableStateOf(emptyList<ar.edu.itba.listapp.data.model.Product>()) }

    // Reload shopping lists and products
    fun reload() {
        val snackbarHostState = snackbarHostState // capture
        scope.launch {
            isLoading = true
            shoppingLists.clear()
            
            // Load shopping lists
            when (val listsRes = listRepository.getShoppingLists(page = 1, perPage = 100, sortBy = "name", order = "ASC")) {
                is GetShoppingListsResult.Success -> {
                    // para cada lista, obtenemos sus items
                    for (list in listsRes.lists) {
                        val listUi = ShoppingListUI(list.id, list.name, mutableStateListOf())
                        when (val itemsRes = listRepository.getShoppingListItems(listId = list.id, page = 1, perPage = 500, sortBy = "name", order = "ASC")) {
                            is GetShoppingListItemsResult.Success -> {
                                listUi.items.addAll(itemsRes.items.map { item ->
                                    val emoji = item.product.metadata["emoji"] ?: "\uD83D\uDCDD" //fallback emoji
                                    ListItemUI(item.id, emoji, item.product.name)
                                })
                            }
                            is GetShoppingListItemsResult.Error -> {
                                // Show items load error but keep list visible
                                snackbarHostState.showSnackbar(itemsRes.message)
                            }
                        }
                        shoppingLists.add(listUi)
                    }
                }
                is GetShoppingListsResult.Error -> snackbarHostState.showSnackbar(listsRes.message)
            }

            // Load available categories and products for selection
            when (val catRes = productRepository.getCategories(page = 1, perPage = 100, sortBy = "name", order = "ASC")) {
                is GetCategoriesResult.Success -> {
                    availableCategories = catRes.categories
                }
                is GetCategoriesResult.Error -> {
                    snackbarHostState.showSnackbar(catRes.message)
                }
            }

            when (val prodRes = productRepository.getProducts(page = 1, perPage = 500, sortBy = "name", order = "ASC")) {
                is GetProductsResult.Success -> {
                    availableProducts = prodRes.products
                }
                is GetProductsResult.Error -> {
                    snackbarHostState.showSnackbar(prodRes.message)
                }
            }

            isLoading = false
        }
    }

    // Initial load
    LaunchedEffect(Unit) {
        reload()
    }

    // Filtering for UI - reactive to changes in shopping lists content
    val filteredLists by remember(searchText) {
        derivedStateOf {
            val base = shoppingLists.toList()
            if (searchText.isBlank()) base else {
                base.mapNotNull { shoppingList ->
                    val filteredItems = shoppingList.items.filter { it.name.contains(searchText, ignoreCase = true) }
                    when {
                        shoppingList.title.contains(searchText, ignoreCase = true) -> shoppingList
                        filteredItems.isNotEmpty() -> shoppingList.copy(items = filteredItems.toMutableList())
                        else -> null
                    }
                }
            }
        }
    }

    // Dialogs
    if (showNewListDialog) {
        NewShoppingListForm(
            onDismiss = { showNewListDialog = false },
            onConfirm = { listName ->
                if (listName.isBlank()) {
                    showNewListDialog = false
                    return@NewShoppingListForm
                }
                scope.launch {
                    when (val res = listRepository.createShoppingList(listName)) {
                        is CreateShoppingListResult.Success -> {
                            // Optimistic add for instant feedback
                            shoppingLists.add(ShoppingListUI(res.list.id, res.list.name, mutableStateListOf()))
                            // And reload from server to stay in sync
                            reload()
                        }
                        is CreateShoppingListResult.Error -> snackbarHostState.showSnackbar(res.message)
                    }
                    showNewListDialog = false
                }
            }
        )
    }

    // Dialog for adding products to a shopping list
    showAddProductsDialog?.let { shoppingList ->
        AddProductsToListDialog(
            categories = availableCategories,
            products = availableProducts,
            onDismiss = { showAddProductsDialog = null },
            onConfirm = { selectedProducts ->
                scope.launch {
                    var successCount = 0
                    for (product in selectedProducts) {
                        when (val res = listRepository.addShoppingListItem(shoppingList.id, product.id)) {
                            is AddShoppingListItemResult.Success -> {
                                val emoji = res.item.product.metadata["emoji"] ?: "\uD83D\uDCDD"
                                shoppingList.items.add(ListItemUI(res.item.id, emoji, res.item.product.name))
                                successCount++
                            }
                            is AddShoppingListItemResult.Error -> snackbarHostState.showSnackbar(res.message)
                        }
                    }
                    if (successCount > 0) {
                        snackbarHostState.showSnackbar("$successCount ${context.getString(R.string.products_added)}")
                    }
                }
                showAddProductsDialog = null
            }
        )
    }

    Scaffold(
        modifier = Modifier.padding(scaffoldPadding),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewListDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_list_icon_description)) },
                text = { Text(stringResource(R.string.new_list), fontWeight = FontWeight.Bold) },
                containerColor = Color.White,
                contentColor = Color.Black,
                shape = RoundedCornerShape(50)
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            SearchBar(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = stringResource(R.string.search_lists),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!isLoading && filteredLists.isEmpty()) {
                NoItemsMessage()
            } else {
                if (isTablet()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredLists, key = { it.id }) { shoppingList ->
                            CollapsibleList(
                                title = shoppingList.title,
                                items = shoppingList.items.map { it.emoji to it.name },
                                onAddItem = {
                                    showAddProductsDialog = shoppingLists.find { it.id == shoppingList.id } ?: shoppingList
                                },
                                onTitleChanged = { newTitle ->
                                    val idx = shoppingLists.indexOfFirst { it.id == shoppingList.id }
                                    if (idx != -1) {
                                        val old = shoppingLists[idx]
                                        shoppingLists[idx] = old.copy(title = newTitle)
                                        scope.launch {
                                            when (val res = listRepository.updateShoppingList(id = shoppingList.id, name = newTitle)) {
                                                is UpdateShoppingListResult.Success -> Unit
                                                is UpdateShoppingListResult.Error -> {
                                                    shoppingLists[idx] = old
                                                    snackbarHostState.showSnackbar(res.message)
                                                }
                                            }
                                        }
                                    }
                                },
                                onDeleteList = {
                                    scope.launch {
                                        when (val res = listRepository.deleteShoppingList(shoppingList.id)) {
                                            is DeleteShoppingListResult.Success -> {
                                                shoppingLists.removeIf { it.id == shoppingList.id }
                                            }
                                            is DeleteShoppingListResult.Error -> snackbarHostState.showSnackbar(res.message)
                                        }
                                    }
                                },
                                onEditItem = { item ->
                                    // Edit item - for now we'll just show a message
                                    // In a full implementation, you'd show a dialog to edit the item
                                },
                                onDeleteItem = { item ->
                                    val baseList = shoppingLists.find { it.id == shoppingList.id } ?: shoppingList
                                    val listItem = baseList.items.find { it.emoji == item.first && it.name == item.second }
                                    if (listItem != null) {
                                        scope.launch {
                                            when (val res = listRepository.deleteShoppingListItem(shoppingList.id, listItem.id)) {
                                                is DeleteShoppingListItemResult.Success -> {
                                                    baseList.items.remove(listItem)
                                                }
                                                is DeleteShoppingListItemResult.Error -> snackbarHostState.showSnackbar(res.message)
                                            }
                                        }
                                    }
                                },
                                onShareList = {
                                    // Share list - for now we'll just show a message
                                    // In a full implementation, you'd show a dialog to share the list
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredLists, key = { it.id }) { shoppingList ->
                            CollapsibleList(
                                title = shoppingList.title,
                                items = shoppingList.items.map { it.emoji to it.name },
                                onAddItem = {
                                    showAddProductsDialog = shoppingLists.find { it.id == shoppingList.id } ?: shoppingList
                                },
                                onTitleChanged = { newTitle ->
                                    val idx = shoppingLists.indexOfFirst { it.id == shoppingList.id }
                                    if (idx != -1) {
                                        val old = shoppingLists[idx]
                                        shoppingLists[idx] = old.copy(title = newTitle)
                                        scope.launch {
                                            when (val res = listRepository.updateShoppingList(id = shoppingList.id, name = newTitle)) {
                                                is UpdateShoppingListResult.Success -> Unit
                                                is UpdateShoppingListResult.Error -> {
                                                    shoppingLists[idx] = old
                                                    snackbarHostState.showSnackbar(res.message)
                                                }
                                            }
                                        }
                                    }
                                },
                                onDeleteList = {
                                    scope.launch {
                                        when (val res = listRepository.deleteShoppingList(shoppingList.id)) {
                                            is DeleteShoppingListResult.Success -> {
                                                shoppingLists.removeIf { it.id == shoppingList.id }
                                            }
                                            is DeleteShoppingListResult.Error -> snackbarHostState.showSnackbar(res.message)
                                        }
                                    }
                                },
                                onEditItem = { item ->
                                    // Edit item - for now we'll just show a message
                                    // In a full implementation, you'd show a dialog to edit the item
                                },
                                onDeleteItem = { item ->
                                    val baseList = shoppingLists.find { it.id == shoppingList.id } ?: shoppingList
                                    val listItem = baseList.items.find { it.emoji == item.first && it.name == item.second }
                                    if (listItem != null) {
                                        scope.launch {
                                            when (val res = listRepository.deleteShoppingListItem(shoppingList.id, listItem.id)) {
                                                is DeleteShoppingListItemResult.Success -> {
                                                    baseList.items.remove(listItem)
                                                }
                                                is DeleteShoppingListItemResult.Error -> snackbarHostState.showSnackbar(res.message)
                                            }
                                        }
                                    }
                                },
                                onShareList = {
                                    // Share list - for now we'll just show a message
                                    // In a full implementation, you'd show a dialog to share the list
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListsScreenPreview() {
    ListappTheme {
        ListsScreen(PaddingValues())
    }
}
