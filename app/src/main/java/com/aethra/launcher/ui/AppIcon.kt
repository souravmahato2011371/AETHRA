package com.aethra.launcher.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aethra.launcher.data.LauncherApp

@Composable
fun AppIcon(app: LauncherApp, size: Dp = 48.dp) {
    val context = LocalContext.current
    val bitmap = remember(app.id, app.icon) {
        val drawable = app.icon.constantState
            ?.newDrawable(context.resources)
            ?.mutate()
            ?: app.icon.mutate()

        val pixels = 144
        val image = Bitmap.createBitmap(pixels, pixels, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        drawable.setBounds(0, 0, pixels, pixels)
        drawable.draw(canvas)
        image.asImageBitmap()
    }

    Image(
        bitmap = bitmap,
        contentDescription = app.label,
        modifier = Modifier.size(size),
        contentScale = ContentScale.Fit
    )
}
