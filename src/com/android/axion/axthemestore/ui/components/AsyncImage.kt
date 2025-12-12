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

package com.android.axion.axthemestore.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import java.net.URL

object ImageCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    
    private val cache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }
    
    fun get(url: String): Bitmap? = cache.get(url)
    
    fun put(url: String, bitmap: Bitmap) {
        cache.put(url, bitmap)
    }
}

sealed class ImageLoadState {
    data object Loading : ImageLoadState()
    data class Success(val bitmap: Bitmap) : ImageLoadState()
    data object Error : ImageLoadState()
}

@Composable
fun AsyncNetworkImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    errorContent: @Composable (() -> Unit)? = null
) {
    var loadState by remember(url) { mutableStateOf<ImageLoadState>(ImageLoadState.Loading) }
    
    LaunchedEffect(url) {
        val cached = ImageCache.get(url)
        if (cached != null) {
            loadState = ImageLoadState.Success(cached)
            return@LaunchedEffect
        }
        
        loadState = ImageLoadState.Loading
        loadState = try {
            val bitmap = withContext(Dispatchers.IO) {
                val connection = URL(url).openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.inputStream.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            }
            if (bitmap != null) {
                ImageCache.put(url, bitmap)
                ImageLoadState.Success(bitmap)
            } else {
                ImageLoadState.Error
            }
        } catch (e: Exception) {
            ImageLoadState.Error
        }
    }
    
    when (val state = loadState) {
        is ImageLoadState.Loading -> {
            Box(
                modifier = modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
        is ImageLoadState.Success -> {
            Image(
                bitmap = state.bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        }
        is ImageLoadState.Error -> {
            if (errorContent != null) {
                errorContent()
            } else {
                Box(
                    modifier = modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = "Failed to load image",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ImagePlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
            modifier = Modifier.size(48.dp)
        )
    }
}
