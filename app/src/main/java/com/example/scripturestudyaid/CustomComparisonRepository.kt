package com.example.scripturestudyaid

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object CustomComparisonRepository {
    private const val FILE_NAME = "custom_comparisons.json"
    private val gson = Gson()

    fun getComparisons(context: Context): List<CustomComparison> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()

        return try {
            val reader = FileReader(file)
            val type = object : TypeToken<List<CustomComparison>>() {}.type
            val list: List<CustomComparison>? = gson.fromJson(reader, type)
            reader.close()
            list ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveComparison(context: Context, comparison: CustomComparison) {
        val currentList = getComparisons(context).toMutableList()
        
        // Remove existing if updating
        val index = currentList.indexOfFirst { it.id == comparison.id }
        if (index != -1) {
            currentList[index] = comparison
        } else {
            currentList.add(comparison)
        }
        
        saveList(context, currentList)
    }

    fun deleteComparison(context: Context, comparisonId: String) {
        val currentList = getComparisons(context).toMutableList()
        currentList.removeAll { it.id == comparisonId }
        saveList(context, currentList)
    }

    private fun saveList(context: Context, list: List<CustomComparison>) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val writer = FileWriter(file)
            gson.toJson(list, writer)
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
