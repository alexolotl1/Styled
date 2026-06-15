package com.example.styled.network

import android.util.Base64
import android.util.Log
import com.example.styled.data.ClothingItem
import com.example.styled.data.OutfitSuggestion
import com.example.styled.data.ShoppingSuggestion
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import java.io.File
import java.net.URL

@Serializable
private data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 1024
)

@Serializable
private data class GroqMessage(
    val role: String,
    val content: List<ContentItem>
)

@Serializable
private data class ContentItem(
    val type: String,
    val text: String? = null,
    val image_url: ImageUrl? = null
)

@Serializable
private data class ImageUrl(
    val url: String
)

@Serializable
private data class GroqResponse(
    val choices: List<Choice>,
    val usage: Usage
)

@Serializable
private data class Choice(
    val message: Message,
    val finish_reason: String
)

@Serializable
private data class Message(
    val content: String,
    val role: String
)

@Serializable
private data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

object GroqApiClient {
    private const val GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"
    private const val MODEL = "meta-llama/llama-4-scout-17b-16e-instruct"
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun analyzeClothingImage(
        imageUrl: String,
        apiKey: String
    ): Result<ClothingItem> = try {
        val systemPrompt = """
            Analyze this clothing item image and return STRICT JSON with these exact fields:
            - title: short name (e.g., "Orange Sweatshirt", "Blue Denim Jeans")
            - description: exactly 2 sentences describing the item
            - category: ONE of [top, bottom, outerwear, shoes, accessory]
            - color_primary: ONE word color (e.g., "orange", "blue", "black")
            
            Return ONLY valid JSON, no other text.
        """.trimIndent()

        val request = GroqRequest(
            model = MODEL,
            messages = listOf(
                GroqMessage(
                    role = "user",
                    content = listOf(
                        ContentItem(
                            type = "text",
                            text = systemPrompt
                        ),
                        ContentItem(
                            type = "image_url",
                            image_url = ImageUrl(url = imageUrl)
                        )
                    )
                )
            )
        )

        val response = httpClient.post(GROQ_API_URL) {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (response.status.value in 200..299) {
            val text = response.bodyAsText()
            Log.d("GroqApiClient", "Response: $text")
            
            val groqResponse = json.decodeFromString<GroqResponse>(text)
            val content = groqResponse.choices.firstOrNull()?.message?.content 
                ?: return Result.failure(Exception("No response content"))

            // Extract JSON from the response
            val jsonString = extractJsonFromResponse(content)
            val jsonData = json.decodeFromString<JsonObject>(jsonString)

            val clothingItem = ClothingItem(
                image_url = imageUrl,
                title = jsonData["title"]?.jsonPrimitive?.content ?: "Unknown",
                description = jsonData["description"]?.jsonPrimitive?.content ?: "",
                category = sanitizeCategory(jsonData["category"]?.jsonPrimitive?.content ?: "top"),
                color_primary = jsonData["color_primary"]?.jsonPrimitive?.content ?: "unknown"
            )

            Result.success(clothingItem)
        } else {
            val errorBody = response.bodyAsText()

            Log.e(
                "GroqApiClient",
                "Status=${response.status}\n$errorBody"
            )

            Result.failure(
                Exception("API error: ${response.status}\n$errorBody")
            )
        }
    } catch (e: Exception) {
        Log.e("GroqApiClient", "Error analyzing image", e)
        Result.failure(e)
    }

    private fun extractJsonFromResponse(content: String): String {
        val jsonStart = content.indexOf('{')
        val jsonEnd = content.lastIndexOf('}')
        return if (jsonStart != -1 && jsonEnd != -1) {
            content.substring(jsonStart, jsonEnd + 1)
        } else {
            content
        }
    }

    private fun sanitizeCategory(category: String): String {
        return if (ClothingItem.VALID_CATEGORIES.contains(category.lowercase())) {
            category.lowercase()
        } else {
            "top"
        }
    }

    suspend fun downloadImageAsBase64(url: String): String? = try {
        val url = URL(url)
        val connection = url.openConnection()
        val inputStream = connection.getInputStream()
        val bytes = inputStream.readBytes()
        inputStream.close()
        Base64.encodeToString(bytes, Base64.DEFAULT)
    } catch (e: Exception) {
        Log.e("GroqApiClient", "Error downloading image", e)
        null
    }

    suspend fun generateOutfitSuggestions(
        items: List<ClothingItem>,
        apiKey: String,
        retryCount: Int = 0
    ): Result<List<OutfitSuggestion>> {
        return try {
            if (items.isEmpty()) {
                Result.failure(Exception("No clothing items provided"))
            } else {
                val itemsDescription = items.mapIndexed { index, item ->
                    """
                    ID: ${item.id}
                    Title: ${item.title}
                    Description: ${item.description}
                    Category: ${item.category}
                    Color: ${item.color_primary}
                    """.trimIndent()
                }.joinToString("\n---\n")

                val systemPrompt = """
                    You are an expert fashion stylist. Analyze the following clothing items and create 3-5 outfit suggestions.
                    
                    CLOTHING ITEMS:
                    $itemsDescription
                    
                    For each outfit, provide:
                    1. Creative outfit name
                    2. Item IDs from the inventory
                    3. Color palette harmony notes
                    4. Layering suggestions
                    5. Vibe/aesthetic notes
                    6. A 1-2 sentence outfit description explaining the look
                    7. Accessory suggestions (jewelry, bags, shoes to wear with the outfit)
                    
                    Return ONLY valid JSON, no markdown, no explanation text. Format:
                    [
                      {
                        "outfit_name": "string (creative name)",
                        "item_ids": ["id1", "id2", "id3"],
                        "color_notes": "1 sentence about color palette",
                        "layering_notes": "1 sentence about layering",
                        "vibe_notes": "1 sentence about overall aesthetic",
                        "overall_score": 8,
                        "outfit_description": "1-2 sentences describing the outfit look and feel",
                        "accessory_suggestions": "suggestions like gold jewelry, leather bag, white sneakers, etc."
                      }
                    ]
                """.trimIndent()

                val request = GroqRequest(
                    model = MODEL,
                    messages = listOf(
                        GroqMessage(
                            role = "user",
                            content = listOf(
                                ContentItem(
                                    type = "text",
                                    text = systemPrompt
                                )
                            )
                        )
                    ),
                    temperature = 0.7,
                    max_tokens = 2048
                )

                val response = httpClient.post(GROQ_API_URL) {
                    bearerAuth(apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                if (response.status.value in 200..299) {
                    val text = response.bodyAsText()
                    Log.d("GroqApiClient", "Outfit suggestions response: $text")

                    val groqResponse = json.decodeFromString<GroqResponse>(text)
                    val content = groqResponse.choices.firstOrNull()?.message?.content
                        ?: return Result.failure(Exception("No response content"))

                    // Extract JSON array from the response
                    val jsonString = extractJsonArrayFromResponse(content)
                    val outfits = json.decodeFromString<List<OutfitSuggestion>>(jsonString)

                    if (outfits.isEmpty()) {
                        Result.failure(Exception("No outfits generated"))
                    } else {
                        Result.success(outfits)
                    }
                } else {
                    val errorBody = response.bodyAsText()
                    Log.e(
                        "GroqApiClient",
                        "Status=${response.status}\n$errorBody"
                    )
                    Result.failure(
                        Exception("API error: ${response.status}\n$errorBody")
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("GroqApiClient", "Error generating outfit suggestions", e)
            
            // Retry once on parse errors
            if (retryCount < 1 && e.message?.contains("JSON") == true) {
                Log.d("GroqApiClient", "Retrying outfit generation due to parse error")
                generateOutfitSuggestions(items, apiKey, retryCount + 1)
            } else {
                Result.failure(e)
            }
        }
    }

    private fun extractJsonArrayFromResponse(content: String): String {
        val jsonStart = content.indexOf('[')
        val jsonEnd = content.lastIndexOf(']')
        return if (jsonStart != -1 && jsonEnd != -1) {
            content.substring(jsonStart, jsonEnd + 1)
        } else {
            content
        }
    }

    suspend fun generateShoppingSuggestions(
        items: List<ClothingItem>,
        apiKey: String
    ): Result<List<ShoppingSuggestion>> {
        return try {
            if (items.isEmpty()) {
                Result.failure(Exception("No clothing items provided"))
            } else {
                // Build inventory context
                val inventoryContext = items.mapIndexed { index, item ->
                    """
                    ${item.title} - ${item.category}, ${item.color_primary} color
                    """.trimIndent()
                }.joinToString("\n")

                val systemPrompt = """
                    You are a fashion stylist assistant. Based on the user's current closet, suggest items they should buy to fill gaps and add variety.
                    
                    CURRENT CLOSET INVENTORY:
                    $inventoryContext
                    
                    Analyze the closet and suggest 5-8 items that would:
                    - Fill gaps in their wardrobe (missing styles, colors, or categories)
                    - Complement existing pieces
                    - Add versatility and outfit options
                    
                    For each suggestion:
                    - Mention the item name
                    - Explain briefly why it complements their closet (1-2 sentences)
                    - Suggest a retailer (Uniqlo, H&M, Zara, Gap, ASOS, etc.)
                    - Provide a search URL format: "https://www.google.com/search?q=[item+to+search]"
                    - Estimate a price range (e.g., "$20-40", "$80-120")
                    
                    Return ONLY valid JSON array, no markdown or explanation:
                    [
                      {
                        "item_name": "string",
                        "reason": "string (1-2 sentences)",
                        "suggested_store": "string",
                        "search_url": "string",
                        "price_range": "string"
                      }
                    ]
                """.trimIndent()

                val request = GroqRequest(
                    model = MODEL,
                    messages = listOf(
                        GroqMessage(
                            role = "user",
                            content = listOf(
                                ContentItem(
                                    type = "text",
                                    text = systemPrompt
                                )
                            )
                        )
                    ),
                    temperature = 0.7,
                    max_tokens = 2048
                )

                val response = httpClient.post(GROQ_API_URL) {
                    bearerAuth(apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                if (response.status.value in 200..299) {
                    val text = response.bodyAsText()
                    Log.d("GroqApiClient", "Shopping suggestions response: $text")

                    val groqResponse = json.decodeFromString<GroqResponse>(text)
                    val content = groqResponse.choices.firstOrNull()?.message?.content
                        ?: return Result.failure(Exception("No response content"))

                    val jsonString = extractJsonArrayFromResponse(content)
                    val suggestions = json.decodeFromString<List<ShoppingSuggestion>>(jsonString)

                    if (suggestions.isEmpty()) {
                        Result.failure(Exception("No suggestions generated"))
                    } else {
                        Result.success(suggestions)
                    }
                } else {
                    val errorBody = response.bodyAsText()
                    Log.e("GroqApiClient", "Status=${response.status}\n$errorBody")
                    Result.failure(Exception("API error: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            Log.e("GroqApiClient", "Error generating shopping suggestions", e)
            Result.failure(e)
        }
    }

}


