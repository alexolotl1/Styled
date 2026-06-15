package com.example.styled.network

import android.util.Log
import com.example.styled.data.ClothingItem
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object SupabaseClientProvider {
    private lateinit var supabaseUrl: String
    private lateinit var supabaseAnonKey: String
    private var isInitialized = false

    private val httpClient by lazy {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }
    }

    fun initialize(url: String, anonKey: String) {
        if (!isInitialized) {
            supabaseUrl = url
            supabaseAnonKey = anonKey
            isInitialized = true
            Log.d("SupabaseClientProvider", "Supabase initialized with URL: $url")
        }
    }

    suspend fun insertClothingItem(item: ClothingItem): Result<ClothingItem> = try {
        val url = "$supabaseUrl/rest/v1/clothing_items"
        val response = httpClient.post(url) {
            bearerAuth(supabaseAnonKey)
            header("apiKey", supabaseAnonKey)
            header("Content-Type", "application/json")
            setBody(item)
        }

        if (response.status.value in 200..299) {
            Result.success(item)
        } else {
            Result.failure(Exception("Failed to insert item: ${response.status}"))
        }
    } catch (e: Exception) {
        Log.e("SupabaseClientProvider", "Error inserting clothing item", e)
        Result.failure(e)
    }

    suspend fun getAllClothingItems(): Result<List<ClothingItem>> = try {
        val url = "$supabaseUrl/rest/v1/clothing_items"
        val response = httpClient.get(url) {
            bearerAuth(supabaseAnonKey)
            header("apiKey", supabaseAnonKey)
        }

        if (response.status.value in 200..299) {
            val text = response.bodyAsText()
            val json = Json { ignoreUnknownKeys = true }
            val items = json.decodeFromString<List<ClothingItem>>(text)
            Result.success(items)
        } else {
            Result.failure(Exception("Failed to fetch items: ${response.status}"))
        }
    } catch (e: Exception) {
        Log.e("SupabaseClientProvider", "Error fetching clothing items", e)
        Result.failure(e)
    }

    suspend fun uploadFile(bucketName: String, fileName: String, fileBytes: ByteArray): Result<String> = try {
        val url = "$supabaseUrl/storage/v1/object/$bucketName/$fileName"
        val response = httpClient.post(url) {
            bearerAuth(supabaseAnonKey)
            header("apiKey", supabaseAnonKey)
            header("Content-Type", "image/jpeg")
            setBody(fileBytes)
        }

        if (response.status.value in 200..299) {
            val publicUrl = "$supabaseUrl/storage/v1/object/public/$bucketName/$fileName"
            Result.success(publicUrl)
        } else {
            Result.failure(Exception("Failed to upload file: ${response.status}"))
        }
    } catch (e: Exception) {
        Log.e("SupabaseClientProvider", "Error uploading file", e)
        Result.failure(e)
    }
}




