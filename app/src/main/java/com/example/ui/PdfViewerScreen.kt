package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.FileDownloader
import com.example.data.PreferencesManager
import com.example.model.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Thread-safe wrapper around Android's PdfRenderer.
 * Prevents "Document already closed" or "PdfProcessor null reference" by strictly synchronizing
 * render calls and ensuring pages are always closed in finally blocks.
 */
class SafePdfDocument(val file: File) {
    private val lock = Any()
    private var pfd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private var isClosed = false

    var pageCount: Int = 0
        private set

    init {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        pfd = descriptor
        val pdfR = PdfRenderer(descriptor)
        renderer = pdfR
        pageCount = pdfR.pageCount
    }

    fun renderPage(pageIndex: Int): Bitmap? {
        synchronized(lock) {
            val r = renderer ?: return null
            if (isClosed || pageIndex < 0 || pageIndex >= pageCount) return null

            var page: PdfRenderer.Page? = null
            return try {
                page = r.openPage(pageIndex)
                val width = (page.width * 2).coerceIn(700, 1800)
                val height = (page.height * 2).coerceIn(1000, 2600)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                val canvas = Canvas(bitmap)
                // Pure white canvas background (RGB 255, 255, 255) for crisp rendering
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } catch (e: Exception) {
                Log.e("SafePdfDocument", "Error rendering page $pageIndex: ${e.message}")
                null
            } finally {
                try {
                    page?.close()
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    fun close() {
        synchronized(lock) {
            if (!isClosed) {
                isClosed = true
                try {
                    renderer?.close()
                } catch (e: Exception) {
                    // ignore
                }
                renderer = null
                try {
                    pfd?.close()
                } catch (e: Exception) {
                    // ignore
                }
                pfd = null
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PdfViewerScreen(
    fileName: String,
    bookTitle: String,
    url: String,
    assetFallback: String? = null,
    onClose: () -> Unit,
    isDarkTheme: Boolean = true
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefsManager = remember { PreferencesManager.getInstance(context) }
    val userLang = prefsManager.loadSettings().language

    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var isDownloading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var localPdfFile by remember { mutableStateOf<File?>(null) }
    var reloadTrigger by remember { mutableIntStateOf(0) }

    var isDarkModeActive by remember { mutableStateOf(isDarkTheme) }

    var safePdfDoc by remember { mutableStateOf<SafePdfDocument?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }

    // Page bitmap cache
    val pageBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }

    // Zoom & Pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Jump to page dialog state
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpPageInput by remember { mutableStateOf("") }

    // Trigger download of the real remote PDF book on start
    LaunchedEffect(fileName, url, reloadTrigger) {
        errorMessage = null
        val cached = FileDownloader.getCachedFile(context, fileName)
        if (cached != null && FileDownloader.isValidPdf(cached) && cached.length() > 50000) {
            localPdfFile = cached
            isDownloading = false
        } else {
            isDownloading = true
            downloadProgress = 0f
            val result = FileDownloader.downloadOrGetFile(
                context = context,
                url = url,
                fileName = fileName,
                assetFallback = assetFallback,
                forceDownload = true,
                onProgress = { progress ->
                    downloadProgress = progress
                }
            )
            result.onSuccess { file ->
                localPdfFile = file
                isDownloading = false
            }.onFailure { err ->
                errorMessage = err.message ?: when (userLang) {
                    AppLanguage.TR -> "Kitap indirilemedi. Lütfen 'Tekrar Dene' butonuna dokununuz."
                    AppLanguage.RU -> "Не удалось скачать книгу. Нажмите 'Повторить'."
                    AppLanguage.AZ -> "Kitab endirilə bilmədi. Zəhmət olmasa 'Yenidən cəhd et' düyməsinə vurun."
                    AppLanguage.EN -> "Failed to download book. Please tap Retry."
                }
                isDownloading = false
            }
        }
    }

    // Initialize SafePdfDocument once local file is ready
    LaunchedEffect(localPdfFile) {
        val file = localPdfFile ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                safePdfDoc?.close()
                safePdfDoc = null
                pageBitmaps.clear()

                val doc = SafePdfDocument(file)
                safePdfDoc = doc
                pageCount = doc.pageCount
                errorMessage = null
            } catch (e: Exception) {
                Log.e("PdfViewerScreen", "Failed to open PDF document: ${e.message}", e)
                file.delete()
                localPdfFile = null
                errorMessage = when (userLang) {
                    AppLanguage.TR -> "PDF dosyası açılamadı. Lütfen 'Tekrar Dene' butonuna basınız."
                    AppLanguage.RU -> "Не удалось открыть PDF. Пожалуйста, нажмите 'Повторить'."
                    AppLanguage.AZ -> "PDF faylı açıla bilmədi. Zəhmət olmasa 'Yenidən cəhd et' düyməsinə basın."
                    AppLanguage.EN -> "Failed to open PDF. Please tap Retry."
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            safePdfDoc?.close()
            safePdfDoc = null
            pageBitmaps.clear()
        }
    }

    val pagerState = rememberPagerState(pageCount = { if (pageCount > 0) pageCount else 1 })

    // Function to render a specific page into bitmap
    fun requestPageRender(pageIndex: Int) {
        val doc = safePdfDoc ?: return
        if (pageIndex < 0 || pageIndex >= pageCount || pageBitmaps.containsKey(pageIndex)) return

        coroutineScope.launch(Dispatchers.IO) {
            val bmp = doc.renderPage(pageIndex)
            if (bmp != null) {
                withContext(Dispatchers.Main) {
                    pageBitmaps[pageIndex] = bmp
                }
            }
        }
    }

    // Pre-render current, previous and next pages & reset zoom on page change
    LaunchedEffect(pagerState.currentPage, safePdfDoc, pageCount) {
        if (pageCount > 0 && safePdfDoc != null) {
            val curr = pagerState.currentPage
            requestPageRender(curr)
            if (curr + 1 < pageCount) requestPageRender(curr + 1)
            if (curr + 2 < pageCount) requestPageRender(curr + 2)
            if (curr - 1 >= 0) requestPageRender(curr - 1)
        }
        scale = 1f
        offset = Offset.Zero
    }

    // Strict neutral colors: Dark Mode = Neutral Gray (#1E1E1E), Light Mode = Crisp White (#FFFFFF)
    val deskBgColor = if (isDarkModeActive) Color(0xFF181818) else Color(0xFFFFFFFF)
    val paperContainerBg = if (isDarkModeActive) Color(0xFF242424) else Color(0xFFFFFFFF)
    val paperBorderColor = if (isDarkModeActive) Color(0xFF383838) else Color(0xFFE5E7EB)
    val topBarBg = if (isDarkModeActive) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val bottomBarBg = if (isDarkModeActive) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val primaryText = if (isDarkModeActive) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val secondaryText = if (isDarkModeActive) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Jump to Page Dialog
    if (showJumpDialog) {
        val dialogTitle = when (userLang) {
            AppLanguage.TR -> "Sayfaya Git (1 - $pageCount)"
            AppLanguage.RU -> "Перейти к странице (1 - $pageCount)"
            AppLanguage.AZ -> "Səhifəyə Get (1 - $pageCount)"
            AppLanguage.EN -> "Go to Page (1 - $pageCount)"
        }
        val labelText = when (userLang) {
            AppLanguage.TR -> "Sayfa Numarası"
            AppLanguage.RU -> "Номер страницы"
            AppLanguage.AZ -> "Səhifə Nömrəsi"
            AppLanguage.EN -> "Page Number"
        }
        val goText = when (userLang) {
            AppLanguage.TR -> "Git"
            AppLanguage.RU -> "Перейти"
            AppLanguage.AZ -> "Get"
            AppLanguage.EN -> "Go"
        }
        val cancelText = when (userLang) {
            AppLanguage.TR -> "İptal"
            AppLanguage.RU -> "Отмена"
            AppLanguage.AZ -> "İmtina"
            AppLanguage.EN -> "Cancel"
        }

        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text(dialogTitle) },
            text = {
                Column {
                    OutlinedTextField(
                        value = jumpPageInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) jumpPageInput = input
                        },
                        label = { Text(labelText) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val targetPage = jumpPageInput.toIntOrNull()
                                if (targetPage != null && targetPage in 1..pageCount) {
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(targetPage - 1)
                                    }
                                    showJumpDialog = false
                                }
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetPage = jumpPageInput.toIntOrNull()
                        if (targetPage != null && targetPage in 1..pageCount) {
                            coroutineScope.launch {
                                pagerState.scrollToPage(targetPage - 1)
                            }
                            showJumpDialog = false
                        }
                    }
                ) {
                    Text(goText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text(cancelText)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = bookTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (pageCount > 0) {
                            val pageStr = when (userLang) {
                                AppLanguage.TR -> "Sayfa ${pagerState.currentPage + 1} / $pageCount"
                                AppLanguage.RU -> "Страница ${pagerState.currentPage + 1} / $pageCount"
                                AppLanguage.AZ -> "Səhifə ${pagerState.currentPage + 1} / $pageCount"
                                AppLanguage.EN -> "Page ${pagerState.currentPage + 1} of $pageCount"
                            }
                            Text(
                                text = pageStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = secondaryText
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("pdf_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = primaryText)
                    }
                },
                actions = {
                    // Jump to page icon
                    if (pageCount > 1) {
                        IconButton(
                            onClick = {
                                jumpPageInput = "${pagerState.currentPage + 1}"
                                showJumpDialog = true
                            },
                            modifier = Modifier.testTag("pdf_jump_page_button")
                        ) {
                            Icon(Icons.Default.FindInPage, contentDescription = "Sayfaya Git", tint = primaryText)
                        }
                    }

                    // Dark (Neutral Gray) / Light (White) Toggle Button
                    IconButton(
                        onClick = { isDarkModeActive = !isDarkModeActive },
                        modifier = Modifier.testTag("pdf_dark_filter_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDarkModeActive) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                            contentDescription = if (isDarkModeActive) "Açık Tema (Beyaz)" else "Karanlık Tema (Gri)",
                            tint = if (isDarkModeActive) Color(0xFFFBBF24) else primaryText
                        )
                    }

                    // Zoom in & out controls
                    IconButton(
                        onClick = { scale = (scale + 0.4f).coerceAtMost(4f) },
                        modifier = Modifier.testTag("zoom_in_button")
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Büyüt", tint = primaryText)
                    }
                    IconButton(
                        onClick = {
                            scale = (scale - 0.4f).coerceAtLeast(1f)
                            if (scale == 1f) offset = Offset.Zero
                        },
                        modifier = Modifier.testTag("zoom_out_button")
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Küçült", tint = primaryText)
                    }
                    if (scale > 1f) {
                        IconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            },
                            modifier = Modifier.testTag("zoom_reset_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Sıfırla", tint = primaryText)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarBg,
                    titleContentColor = primaryText
                )
            )
        },
        bottomBar = {
            if (pageCount > 0 && errorMessage == null && !isDownloading) {
                Surface(
                    color = bottomBarBg,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Previous Page Button
                            IconButton(
                                onClick = {
                                    if (pagerState.currentPage > 0) {
                                        coroutineScope.launch {
                                            pagerState.scrollToPage(pagerState.currentPage - 1)
                                        }
                                        scale = 1f
                                        offset = Offset.Zero
                                    }
                                },
                                enabled = pagerState.currentPage > 0,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("pdf_prev_page")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Önceki Sayfa",
                                    tint = if (pagerState.currentPage > 0) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f)
                                )
                            }

                            // Interactive Page Indicator Button
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isDarkModeActive) Color(0xFF2E2E2E) else Color(0xFFF1F5F9),
                                modifier = Modifier.clickable {
                                    jumpPageInput = "${pagerState.currentPage + 1}"
                                    showJumpDialog = true
                                }
                            ) {
                                Text(
                                    text = "${pagerState.currentPage + 1} / $pageCount",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryText,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                                )
                            }

                            // Next Page Button
                            IconButton(
                                onClick = {
                                    if (pagerState.currentPage < pageCount - 1) {
                                        coroutineScope.launch {
                                            pagerState.scrollToPage(pagerState.currentPage + 1)
                                        }
                                        scale = 1f
                                        offset = Offset.Zero
                                    }
                                },
                                enabled = pagerState.currentPage < pageCount - 1,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("pdf_next_page")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Sonraki Sayfa",
                                    tint = if (pagerState.currentPage < pageCount - 1) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(deskBgColor),
            contentAlignment = Alignment.Center
        ) {
            when {
                isDownloading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = if (isDarkModeActive) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        val prepTitle = when (userLang) {
                            AppLanguage.TR -> "Eser Hazırlanıyor..."
                            AppLanguage.RU -> "Подготовка книги..."
                            AppLanguage.AZ -> "Kitab Hazırlanır..."
                            AppLanguage.EN -> "Preparing Book..."
                        }
                        val prepDesc = when (userLang) {
                            AppLanguage.TR -> "Orijinal sayfalar indiriliyor ve işleniyor..."
                            AppLanguage.RU -> "Загрузка и обработка страниц..."
                            AppLanguage.AZ -> "Orijinal səhifələr endirilir və emal olunur..."
                            AppLanguage.EN -> "Downloading and processing original pages..."
                        }
                        Text(
                            text = prepTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = prepDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { if (downloadProgress > 0) downloadProgress else 0.5f },
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (isDarkModeActive) Color(0xFF9E9E9E) else MaterialTheme.colorScheme.primary,
                            trackColor = if (isDarkModeActive) Color(0xFF2C2C2E) else Color(0xFFE2E8F0)
                        )
                    }
                }

                errorMessage != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        val retryText = when (userLang) {
                            AppLanguage.TR -> "Tekrar İndir ve Aç"
                            AppLanguage.RU -> "Повторить загрузку"
                            AppLanguage.AZ -> "Yenidən Endir və Aç"
                            AppLanguage.EN -> "Retry Download & Open"
                        }
                        Button(
                            onClick = {
                                File(context.filesDir, fileName).delete()
                                localPdfFile = null
                                reloadTrigger++
                            },
                            modifier = Modifier.testTag("pdf_retry_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(retryText)
                        }
                    }
                }

                pageCount > 0 -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Smooth, unhindered swipe HorizontalPager
                        HorizontalPager(
                            state = pagerState,
                            userScrollEnabled = scale <= 1.05f,
                            modifier = Modifier.fillMaxSize()
                        ) { pageIdx ->
                            val isCurrentPage = pageIdx == pagerState.currentPage
                            val pageBitmap = pageBitmaps[pageIdx]
                            if (pageBitmap == null) {
                                requestPageRender(pageIdx)
                            }

                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val maxHeight = this.maxHeight - 4.dp

                                Card(
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, paperBorderColor),
                                    colors = CardDefaults.cardColors(containerColor = paperContainerBg),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    modifier = Modifier
                                        .height(maxHeight)
                                        .aspectRatio(595f / 842f)
                                        .graphicsLayer {
                                             if (isCurrentPage) {
                                                 scaleX = scale
                                                 scaleY = scale
                                                 translationX = offset.x
                                                 translationY = offset.y
                                             } else {
                                                 scaleX = 1f
                                                 scaleY = 1f
                                                 translationX = 0f
                                                 translationY = 0f
                                             }
                                        }
                                        .then(
                                            // Only enable pan gesture when zoomed in to never block swipe
                                            if (isCurrentPage && scale > 1.05f) {
                                                Modifier.pointerInput(pageIdx, scale) {
                                                    detectTransformGestures { _, pan, zoom, _ ->
                                                        val newScale = (scale * zoom).coerceIn(1f, 4.5f)
                                                        scale = newScale
                                                        if (newScale > 1.05f) {
                                                            val maxOffsetX = (size.width.toFloat() * (newScale - 1f)) / 2f
                                                            val maxOffsetY = (size.height.toFloat() * (newScale - 1f)) / 2f
                                                            val newOffset = offset + pan
                                                            offset = Offset(
                                                                x = newOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
                                                                y = newOffset.y.coerceIn(-maxOffsetY, maxOffsetY)
                                                            )
                                                        } else {
                                                            offset = Offset.Zero
                                                        }
                                                    }
                                                }
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .testTag("pdf_paper_sheet_$pageIdx")
                                ) {
                                    if (pageBitmap != null) {
                                        // Display original page with true authentic colors:
                                        // Yellow/sepia pages stay yellow/sepia, white pages stay white, black ink is crisp!
                                        Image(
                                            bitmap = pageBitmap.asImageBitmap(),
                                            contentDescription = "PDF Sayfa ${pageIdx + 1}",
                                            colorFilter = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(4.dp)),
                                            contentScale = ContentScale.FillBounds
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = if (isDarkModeActive) Color.White else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Left Edge Page Turn Zone (Click to go previous)
                        if (scale <= 1.05f && pagerState.currentPage > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(44.dp)
                                    .align(Alignment.CenterStart)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        coroutineScope.launch {
                                            pagerState.scrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                            )
                        }

                        // Right Edge Page Turn Zone (Click to go next)
                        if (scale <= 1.05f && pagerState.currentPage < pageCount - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(44.dp)
                                    .align(Alignment.CenterEnd)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        coroutineScope.launch {
                                            pagerState.scrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                            )
                        }
                    }
                }

                else -> {
                    CircularProgressIndicator(
                        color = if (isDarkModeActive) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }
    }
}
