package com.android.blurapp
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.blur.BlurEffects
import com.android.blur.StackBlur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import androidx.core.graphics.scale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BlurSampleTheme {
                BlurSampleApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlurSampleApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var blurredBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var blurRadius by remember { mutableStateOf(10f) }
    var isProcessing by remember { mutableStateOf(false) }
    var selectedEffect by remember { mutableStateOf("Custom") }

    fun applyBlur(bitmap: Bitmap, radius: Int, onResult: (Bitmap) -> Unit) {
        scope.launch {
            isProcessing = true
            try {
                val result = withContext(Dispatchers.Default) {
                    when (selectedEffect) {
                        "Light" -> BlurEffects.lightBlur(bitmap)
                        "Medium" -> BlurEffects.mediumBlur(bitmap)
                        "Heavy" -> BlurEffects.heavyBlur(bitmap)
                        "Progressive" -> BlurEffects.progressiveBlur(bitmap, 3)
                        else -> StackBlur.blur(bitmap, radius)
                    }
                }
                onResult(result)
            } catch (e: Exception) {
                Toast.makeText(context, "ぼかし処理に失敗しました", Toast.LENGTH_SHORT).show()
            } finally {
                isProcessing = false
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    val resizedBitmap = resizeBitmapIfNeeded(bitmap, 1024)
                    originalBitmap = resizedBitmap

                    applyBlur(resizedBitmap, blurRadius.toInt()) { result ->
                        blurredBitmap = result
                    }
                } catch (e: IOException) {
                    Toast.makeText(context, "画像の読み込みに失敗しました", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Blur Sample App",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "画像を選択",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            originalBitmap?.let { bitmap ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        Text(
                            text = "元の画像",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Original Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Effect Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "エフェクト選択",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val effects = listOf("Custom", "Light", "Medium", "Heavy", "Progressive")

                        effects.forEach { effect ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedEffect == effect,
                                    onClick = {
                                        selectedEffect = effect
                                        if (!isProcessing) {
                                            applyBlur(bitmap, blurRadius.toInt()) { result ->
                                                blurredBitmap = result
                                            }
                                        }
                                    }
                                )
                                Text(
                                    text = when (effect) {
                                        "Custom" -> "カスタム"
                                        "Light" -> "軽度"
                                        "Medium" -> "中度"
                                        "Heavy" -> "重度"
                                        "Progressive" -> "段階的"
                                        else -> effect
                                    },
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedEffect == "Custom") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "ぼかし強度: ${blurRadius.toInt()}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Slider(
                                value = blurRadius,
                                onValueChange = { blurRadius = it },
                                onValueChangeFinished = {
                                    if (!isProcessing) {
                                        applyBlur(bitmap, blurRadius.toInt()) { result ->
                                            blurredBitmap = result
                                        }
                                    }
                                },
                                valueRange = 1f..50f,
                                steps = 49,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("1", fontSize = 12.sp)
                                Text("50", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (isProcessing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "処理中...",
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                blurredBitmap?.let { blurred ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "ぼかし後",
                                modifier = Modifier.padding(16.dp),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Image(
                                bitmap = blurred.asImageBitmap(),
                                contentDescription = "Blurred Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            if (originalBitmap == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📸",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "画像を選択してぼかし効果を試す",
                            fontSize = 16.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun resizeBitmapIfNeeded(bitmap: Bitmap, maxSize: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    return if (width > maxSize || height > maxSize) {
        val scale = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val scaledWidth = (width * scale).toInt()
        val scaledHeight = (height * scale).toInt()

        bitmap.scale(scaledWidth, scaledHeight)
    } else {
        bitmap
    }
}

@Composable
fun BlurSampleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF6200EE),
            secondary = androidx.compose.ui.graphics.Color(0xFF03DAC6),
            surface = androidx.compose.ui.graphics.Color(0xFFF5F5F5),
            background = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
        ),
        content = content
    )
}
