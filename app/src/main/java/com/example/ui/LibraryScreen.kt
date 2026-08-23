package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.FileDownloader
import com.example.model.AppLanguage
import com.example.model.ThemeStyle
import com.example.model.UserSettings
import com.example.ui.components.neonGlow

data class BookItem(
    val id: String,
    val titleTr: String,
    val titleEn: String,
    val titleRu: String,
    val titleAz: String,
    val authorTr: String,
    val authorEn: String,
    val authorRu: String,
    val authorAz: String,
    val coverResId: Int,
    val remoteUrl: String,
    val fileName: String,
    val assetFallback: String
) {
    fun getTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.TR -> titleTr
        AppLanguage.EN -> titleEn
        AppLanguage.RU -> titleRu
        AppLanguage.AZ -> titleAz
    }

    fun getAuthor(lang: AppLanguage): String = when (lang) {
        AppLanguage.TR -> authorTr
        AppLanguage.EN -> authorEn
        AppLanguage.RU -> authorRu
        AppLanguage.AZ -> authorAz
    }
}

@Composable
fun LibraryScreen(
    settings: UserSettings,
    modifier: Modifier = Modifier
) {
    val userLang = settings.language
    val themeStyle = settings.themeStyle

    var activePdfBook by remember { mutableStateOf<BookItem?>(null) }

    val books = remember {
        listOf(
            BookItem(
                id = "kuran",
                titleTr = "Kur'an-ı Kerim",
                titleEn = "The Holy Quran",
                titleRu = "Священный Коран",
                titleAz = "Qurani-Kərim",
                authorTr = "Kelamullah • Arapça Metin & Meali",
                authorEn = "Word of Allah • Arabic & Translation",
                authorRu = "Слово Аллаха • Арабский текст и перевод",
                authorAz = "Kəlamullah • Ərəbcə Mətn və Tərcüməsi",
                coverResId = R.drawable.cover_kuran,
                remoteUrl = FileDownloader.URL_KURAN_PDF,
                fileName = "kuran.pdf",
                assetFallback = "kuran.pdf"
            ),
            BookItem(
                id = "ilmihal",
                titleTr = "İlmihal",
                titleEn = "Islamic Jurisprudence (Ilmihal)",
                titleRu = "Ильмихаль (Основы ислама)",
                titleAz = "Elmihal",
                authorTr = "Temel Dini Bilgiler, İman, İbadet & Namaz",
                authorEn = "Essential Islamic Beliefs & Prayer Guide",
                authorRu = "Основы вероубеждения, поклонение и намаз",
                authorAz = "Əsas Dini Biliklər, İman, İbadət və Namaz",
                coverResId = R.drawable.cover_ilmihal,
                remoteUrl = FileDownloader.URL_ILMIHAL_PDF,
                fileName = "ilmihal.pdf",
                assetFallback = "ilmihal.pdf"
            ),
            BookItem(
                id = "peygamber_hayati",
                titleTr = "Peygamber Efendimizin Hayatı",
                titleEn = "Life of the Prophet Muhammad",
                titleRu = "Жизнь Пророка Мухаммада",
                titleAz = "Peyğəmbərimizin Həyatı",
                authorTr = "Siyer-i Nebi • Doğumu, Tebliği ve Ahlakı",
                authorEn = "Biography, Prophethood & Noble Character",
                authorRu = "Сира • Рождение, пророчество и благородный нрав",
                authorAz = "Siyər • Doğulması, Təbliği və Əxlaqı",
                coverResId = R.drawable.cover_peygamber_hayati,
                remoteUrl = FileDownloader.URL_SIYER_PDF,
                fileName = "peygamber_hayati.pdf",
                assetFallback = "peygamber_hayati.pdf"
            )
        )
    }

    if (activePdfBook != null) {
        val book = activePdfBook!!
        PdfViewerScreen(
            url = book.remoteUrl,
            fileName = book.fileName,
            assetFallback = book.assetFallback,
            bookTitle = book.getTitle(userLang),
            isDarkTheme = settings.isDarkMode,
            onClose = { activePdfBook = null }
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            val libraryTitle = when (userLang) {
                AppLanguage.TR -> "İslami Kütüphane"
                AppLanguage.RU -> "Исламская библиотека"
                AppLanguage.AZ -> "İslam Kitabxanası"
                AppLanguage.EN -> "Islamic Library"
            }
            val librarySubtitle = when (userLang) {
                AppLanguage.TR -> "Buluttan İndirilebilir Temel Eserler & PDF Okuyucu"
                AppLanguage.RU -> "Основные исламские книги и PDF-ридер"
                AppLanguage.AZ -> "Əsas Kitablar və PDF Oxuyucu"
                AppLanguage.EN -> "Cloud-downloaded Islamic Works & PDF Reader"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = libraryTitle,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = libraryTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = librarySubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(books.size) { index ->
                    val book = books[index]
                    BookCard(
                        book = book,
                        themeStyle = themeStyle,
                        language = userLang,
                        onOpenBook = { activePdfBook = book }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookCard(
    book: BookItem,
    themeStyle: ThemeStyle,
    language: AppLanguage,
    onOpenBook: () -> Unit
) {
    val bookTitle = book.getTitle(language)
    val bookAuthor = book.getAuthor(language)
    val readButtonText = when (language) {
        AppLanguage.TR -> "Kitabı Oku"
        AppLanguage.RU -> "Читать"
        AppLanguage.AZ -> "Oxu"
        AppLanguage.EN -> "Read Book"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .neonGlow(themeStyle, cornerRadius = 18.dp)
            .clickable { onOpenBook() }
            .testTag("book_card_${book.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book Cover Image
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(10.dp))
                    .shadow(4.dp, RoundedCornerShape(10.dp))
            ) {
                Image(
                    painter = painterResource(id = book.coverResId),
                    contentDescription = bookTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = bookTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bookAuthor,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onOpenBook,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("read_button_${book.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = readButtonText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = readButtonText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

