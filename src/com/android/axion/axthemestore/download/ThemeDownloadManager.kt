/*
 * Copyright (C) 2025 AxionOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.axion.axthemestore.download

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ThemeDownloadManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ThemeDownloadManager"
        private const val BUFFER_SIZE = 8192
    }
    
    private val downloadDir: File
        get() = File(context.cacheDir, "theme_downloads").also { 
            if (!it.exists()) it.mkdirs() 
        }
    
    sealed class DownloadState {
        data object Starting : DownloadState()
        data class Progress(val bytesDownloaded: Long, val totalBytes: Long) : DownloadState() {
            val progressPercent: Float = if (totalBytes > 0) {
                (bytesDownloaded.toFloat() / totalBytes.toFloat())
            } else 0f
        }
        data class Success(val file: File) : DownloadState()
        data class Error(val exception: Exception) : DownloadState()
    }
    
    fun downloadTheme(themeId: String, downloadUrl: String): Flow<DownloadState> = flow {
        emit(DownloadState.Starting)
        
        val outputFile = File(downloadDir, "${themeId}.apk")
        
        try {
            if (outputFile.exists()) {
                outputFile.delete()
            }
            
            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 30000
                readTimeout = 30000
                instanceFollowRedirects = true
            }
            
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP error: $responseCode")
            }
            
            val totalBytes = connection.contentLengthLong
            var bytesDownloaded = 0L
            
            connection.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead
                        emit(DownloadState.Progress(bytesDownloaded, totalBytes))
                    }
                }
            }
            
            connection.disconnect()
            
            Log.d(TAG, "Download complete: ${outputFile.absolutePath}")
            emit(DownloadState.Success(outputFile))
            
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for $themeId", e)
            if (outputFile.exists()) {
                outputFile.delete()
            }
            emit(DownloadState.Error(e))
        }
    }.flowOn(Dispatchers.IO)
    
    fun getDownloadedFile(themeId: String): File? {
        val file = File(downloadDir, "${themeId}.apk")
        return if (file.exists()) file else null
    }
    
    fun deleteDownload(themeId: String): Boolean {
        val file = File(downloadDir, "${themeId}.apk")
        return if (file.exists()) file.delete() else true
    }
    
    fun clearDownloads() {
        downloadDir.listFiles()?.forEach { it.delete() }
    }
}
