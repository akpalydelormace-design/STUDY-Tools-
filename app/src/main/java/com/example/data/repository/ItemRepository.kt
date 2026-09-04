package com.example.data.repository

import com.example.data.local.ItemDao
import com.example.data.model.Item
import kotlinx.coroutines.flow.Flow

class ItemRepository(private val itemDao: ItemDao) {
    val allItems: Flow<List<Item>> = itemDao.getAllItems()

    suspend fun insert(item: Item) = itemDao.insertItem(item)

    suspend fun deleteById(id: Int) = itemDao.deleteItemById(id)
}
