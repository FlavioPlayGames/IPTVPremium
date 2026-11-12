package br.com.flaviotorres.iptvpremium.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle

/** Existing subtitle control colours are preserved; only the shared background renderer changes. */
internal object MetallicPalette {
    val accent get()=when(ThemeState.selected) {
        PremiumTheme.BLUE->Color(0xFF009EF5)
        PremiumTheme.GOLD->Color(0xFFF2BC36)
        PremiumTheme.PINK->Color(0xFFF64C9B)
        PremiumTheme.PURPLE->Color(0xFFAC68ED)
        PremiumTheme.RED->Color(0xFFF25550)
    }
    val card get()=if(CatalogStyle.light)Color(0xFF506476)else Color(0xFF344655)
    val selected get()=androidx.compose.ui.graphics.lerp(Color(0xFF142B40),accent,.28f)
    val ink get()=Color(0xFFF5F6F8)
    val secondary get()=Color(0xFFDFE6ED)
}

/** Prepared Home scenery, without logo/icons, decoded once through the existing artwork cache. */
@Composable
internal fun MetallicThemeBackdrop(modifier:Modifier=Modifier) {
    val asset=marbleWallpaperAsset(CatalogStyle.light)
    val theme=ThemeState.selected
    val context=LocalContext.current.applicationContext
    val epoch by ThemeArtworkEpoch.collectAsState()
    var image by remember(asset,epoch){mutableStateOf(MarbleArtworkCache.peek(asset))}
    LaunchedEffect(asset,epoch){image=MarbleArtworkCache.load(context,asset)}
    val tint=remember(theme){marbleAccentFilter(theme)}
    Box(modifier.background(CatalogStyle.background)) {
        image?.let {bitmap->
            Image(bitmap,contentDescription=null,modifier=Modifier.matchParentSize(),
                contentScale=ContentScale.Crop,colorFilter=tint,filterQuality=FilterQuality.High)
        }
    }
}
