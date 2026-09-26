package com.example.classroomseating

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.classroomseating.core.ui.theme.ClassRoomSeatingTheme
import com.example.classroomseating.feature.imports.ImportViewModel
import com.example.classroomseating.feature.imports.ui.ImportUiHost
import com.example.classroomseating.navigation.ClassRoomSeatingNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val importViewModel: ImportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            ClassRoomSeatingTheme {
                RootContent(importViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data?.let { importViewModel.onIncomingFileUri(it) }
            Intent.ACTION_SEND -> handleSendIntent(intent)
        }
    }

    private fun handleSendIntent(intent: Intent) {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
        }
        if (uri == null) return
        when {
            intent.type?.startsWith("image/", ignoreCase = true) == true -> importViewModel.onQrImageUri(uri)
            else -> importViewModel.onIncomingFileUri(uri)
        }
    }
}

@Composable
private fun RootContent(importViewModel: ImportViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        ClassRoomSeatingNavHost(importViewModel)
        ImportUiHost(importViewModel)
    }
}