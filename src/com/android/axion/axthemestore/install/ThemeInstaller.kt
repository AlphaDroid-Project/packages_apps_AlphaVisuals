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

package com.android.axion.axthemestore.install

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.io.FileInputStream

class ThemeInstaller(private val context: Context) {
    
    companion object {
        private const val TAG = "ThemeInstaller"
        private const val INSTALL_ACTION = "com.android.axion.axthemestore.INSTALL_COMPLETE"
    }
    
    sealed class InstallResult {
        data object Success : InstallResult()
        data class Failure(val errorCode: Int, val errorMessage: String?) : InstallResult()
    }

    fun installTheme(apkFile: File, packageName: String): Flow<InstallResult> = callbackFlow {
        val packageInstaller = context.packageManager.packageInstaller
        
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(packageName)
        
        val sessionId = try {
            packageInstaller.createSession(params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create install session", e)
            trySend(InstallResult.Failure(-1, e.message))
            close()
            return@callbackFlow
        }
        
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                
                Log.d(TAG, "Install status: $status, message: $message")
                
                when (status) {
                    PackageInstaller.STATUS_SUCCESS -> {
                        trySend(InstallResult.Success)
                    }
                    PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                        val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                        confirmIntent?.let {
                            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(it)
                        }
                    }
                    else -> {
                        trySend(InstallResult.Failure(status, message))
                    }
                }
                
                close()
            }
        }
        
        context.registerReceiver(
            receiver, 
            IntentFilter(INSTALL_ACTION),
            Context.RECEIVER_NOT_EXPORTED
        )
        
        try {
            val session = packageInstaller.openSession(sessionId)
            
            session.openWrite("theme.apk", 0, apkFile.length()).use { outputStream ->
                FileInputStream(apkFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
                session.fsync(outputStream)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                Intent(INSTALL_ACTION).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT
            )
            
            session.commit(pendingIntent.intentSender)
            Log.d(TAG, "Install session committed for: ${apkFile.name}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install APK", e)
            trySend(InstallResult.Failure(-1, e.message))
            close()
        }
        
        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
            }
        }
    }
    
    fun uninstallTheme(packageName: String): Flow<InstallResult> = callbackFlow {
        val uninstallAction = "com.android.axion.axthemestore.UNINSTALL_COMPLETE"
        
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                
                when (status) {
                    PackageInstaller.STATUS_SUCCESS -> {
                        trySend(InstallResult.Success)
                    }
                    else -> {
                        trySend(InstallResult.Failure(status, message))
                    }
                }
                close()
            }
        }
        
        context.registerReceiver(
            receiver,
            IntentFilter(uninstallAction),
            Context.RECEIVER_NOT_EXPORTED
        )
        
        try {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                packageName.hashCode(),
                Intent(uninstallAction).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT
            )
            
            context.packageManager.packageInstaller.uninstall(packageName, pendingIntent.intentSender)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to uninstall package", e)
            trySend(InstallResult.Failure(-1, e.message))
            close()
        }
        
        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
            }
        }
    }

    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
