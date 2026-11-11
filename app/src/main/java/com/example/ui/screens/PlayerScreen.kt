package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.ActivityEntity
import com.example.ui.LumiViewModel
import com.example.ui.Screen
import com.example.ui.theme.LumiAmber
import com.example.ui.theme.SuccessGreen
import org.json.JSONObject

class LumiJsBridge(
    private val onReadyCallback: () -> Unit,
    private val onScoreCallback: (Int, Int) -> Unit,
    private val onProgressCallback: (Int, Int) -> Unit
) {
    @JavascriptInterface
    fun onRunnerReady() {
        try {
            onReadyCallback()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun onScore(score: Int, maxScore: Int) {
        try {
            onScoreCallback(score, maxScore)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun onProgress(current: Int, total: Int) {
        try {
            onProgressCallback(current, total)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun onActivityLoaded(title: String?, library: String?) {
        // Activity initialized safely
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    activity: ActivityEntity,
    viewModel: LumiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentScore by viewModel.currentScore.collectAsState()
    val maxScore by viewModel.maxScore.collectAsState()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showCompletionDialog by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.navigateTo(Screen.Library)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activity.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = "${activity.type.label} • Offline H5P",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Library) },
                        modifier = Modifier.testTag("player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Library"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val escapedManifest = JSONObject.quote(activity.h5pJson)
                            val escapedContent = JSONObject.quote(activity.contentJson)
                            webViewRef?.evaluateJavascript(
                                "window.loadH5P($escapedManifest, $escapedContent);",
                                null
                            )
                        },
                        modifier = Modifier.testTag("player_restart_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart Activity")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Persistent Score and Status bar
            if (maxScore > 0) {
                Card(
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Score: $currentScore / $maxScore",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Button(
                            onClick = { showCompletionDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("player_view_results_button")
                        ) {
                            Text("Results", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("h5p_webview"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val escapedManifest = JSONObject.quote(activity.h5pJson)
                                val escapedContent = JSONObject.quote(activity.contentJson)
                                view?.evaluateJavascript(
                                    "if (typeof window.loadH5P === 'function') { window.loadH5P($escapedManifest, $escapedContent); }",
                                    null
                                )
                            }
                        }

                        val bridge = LumiJsBridge(
                            onReadyCallback = {
                                post {
                                    val escapedManifest = JSONObject.quote(activity.h5pJson)
                                    val escapedContent = JSONObject.quote(activity.contentJson)
                                    evaluateJavascript(
                                        "if (typeof window.loadH5P === 'function') { window.loadH5P($escapedManifest, $escapedContent); }",
                                        null
                                    )
                                }
                            },
                            onScoreCallback = { score, max ->
                                post {
                                    viewModel.onScoreReceived(score, max)
                                }
                            },
                            onProgressCallback = { cur, tot ->
                                // Optional progress hook
                            }
                        )

                        addJavascriptInterface(bridge, "LumiBridge")
                        loadUrl("file:///android_asset/h5p/player.html")
                        webViewRef = this
                    }
                },
                update = { webView ->
                    webViewRef = webView
                }
            )
        }
    }

    // Completion Dialog
    if (showCompletionDialog) {
        val percentage = if (maxScore > 0) (currentScore * 100) / maxScore else 0
        AlertDialog(
            onDismissRequest = { showCompletionDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Celebration,
                        contentDescription = null,
                        tint = LumiAmber,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (percentage >= 70) "Great Job!" else "Keep Practicing!",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$currentScore / $maxScore",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (percentage >= 70) SuccessGreen else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$percentage% accuracy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (percentage == 100) "Flawless score! You have mastered this activity."
                        else if (percentage >= 70) "Well done! You passed the activity successfully."
                        else "Try retrying to reinforce your knowledge.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompletionDialog = false
                        viewModel.navigateTo(Screen.Library)
                    }
                ) {
                    Text("Back to Library")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showCompletionDialog = false
                        val escapedManifest = JSONObject.quote(activity.h5pJson)
                        val escapedContent = JSONObject.quote(activity.contentJson)
                        webViewRef?.evaluateJavascript(
                            "window.loadH5P($escapedManifest, $escapedContent);",
                            null
                        )
                    }
                ) {
                    Text("Try Again")
                }
            }
        )
    }
}
