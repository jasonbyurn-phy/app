package com.google.ai.edge.gallery.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

@Composable
fun AppBackground(
    @DrawableRes resId: Int,
    // 배경 위 가독성 보정(상단 어둡게 → 하단 투명)
    scrim: Brush = Brush.verticalGradient(
        0f to Color.Black.copy(alpha = 0.40f),
        0.35f to Color.Black.copy(alpha = 0.20f),
        1f to Color.Transparent
    ),
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(resId),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.fillMaxSize().background(scrim))
        content()
    }
}
