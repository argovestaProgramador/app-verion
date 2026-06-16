package com.verion.practicas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiClient {
    const val BASE_URL = "https://wordker-veri-on.enigma36sades.workers.dev"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    data class ApiResult(
        val success: Boolean,
        val data: JSONObject?,
        val error: String?,
        val isUnauthorized: Boolean = false,
        val dataArray: JSONArray? = null
    )

    private fun parseResult(json: JSONObject, is401: Boolean): ApiResult {
        if (!json.optBoolean("success", false)) {
            return ApiResult(false, null, json.optString("error", "Error desconocido"), is401)
        }
        val raw = json.opt("data")
        return ApiResult(
            success   = true,
            data      = raw as? JSONObject,
            error     = null,
            dataArray = raw as? JSONArray
        )
    }

    suspend fun get(path: String, authToken: String? = null): ApiResult =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder().url("$BASE_URL$path").get()
                if (authToken != null) req.addHeader("Authorization", "Bearer $authToken")
                val response = client.newCall(req.build()).execute()
                val json = JSONObject(response.body?.string() ?: "{}")
                parseResult(json, response.code == 401)
            } catch (e: Exception) {
                ApiResult(false, null, e.message ?: "Error de conexión")
            }
        }

    suspend fun refreshToken(refreshTokenValue: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().put("refreshToken", refreshTokenValue)
                val req = Request.Builder()
                    .url("$BASE_URL/api/auth/refresh")
                    .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                val json = JSONObject(client.newCall(req).execute().body?.string() ?: "{}")
                if (json.optBoolean("success", false))
                    json.optJSONObject("data")?.optString("accessToken")
                else null
            } catch (e: Exception) { null }
        }

    suspend fun post(path: String, body: JSONObject, authToken: String? = null): ApiResult =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url("$BASE_URL$path")
                    .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                if (authToken != null) req.addHeader("Authorization", "Bearer $authToken")
                val response = client.newCall(req.build()).execute()
                val json = JSONObject(response.body?.string() ?: "{}")
                parseResult(json, response.code == 401)
            } catch (e: Exception) {
                ApiResult(false, null, e.message ?: "Error de conexión")
            }
        }

    suspend fun put(path: String, body: JSONObject, authToken: String? = null): ApiResult =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url("$BASE_URL$path")
                    .put(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                if (authToken != null) req.addHeader("Authorization", "Bearer $authToken")
                val response = client.newCall(req.build()).execute()
                val json = JSONObject(response.body?.string() ?: "{}")
                parseResult(json, response.code == 401)
            } catch (e: Exception) {
                ApiResult(false, null, e.message ?: "Error de conexión")
            }
        }

    suspend fun patch(path: String, body: JSONObject, authToken: String? = null): ApiResult =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url("$BASE_URL$path")
                    .patch(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                if (authToken != null) req.addHeader("Authorization", "Bearer $authToken")
                val response = client.newCall(req.build()).execute()
                val json = JSONObject(response.body?.string() ?: "{}")
                parseResult(json, response.code == 401)
            } catch (e: Exception) {
                ApiResult(false, null, e.message ?: "Error de conexión")
            }
        }

    suspend fun delete(path: String, authToken: String? = null): ApiResult =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder().url("$BASE_URL$path").delete()
                if (authToken != null) req.addHeader("Authorization", "Bearer $authToken")
                val response = client.newCall(req.build()).execute()
                val json = JSONObject(response.body?.string() ?: "{}")
                parseResult(json, response.code == 401)
            } catch (e: Exception) {
                ApiResult(false, null, e.message ?: "Error de conexión")
            }
        }

    suspend fun upload(
        path: String,
        bytes: ByteArray,
        mimeType: String,
        fileName: String,
        authToken: String? = null
    ): ApiResult = withContext(Dispatchers.IO) {
        try {
            val fileBody = bytes.toRequestBody(mimeType.toMediaType())
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, fileBody)
                .build()
            val req = Request.Builder().url("$BASE_URL$path").put(multipart)
            if (authToken != null) req.addHeader("Authorization", "Bearer $authToken")
            val response = client.newCall(req.build()).execute()
            val json = JSONObject(response.body?.string() ?: "{}")
            parseResult(json, response.code == 401)
        } catch (e: Exception) {
            ApiResult(false, null, e.message ?: "Error de conexión")
        }
    }
}
