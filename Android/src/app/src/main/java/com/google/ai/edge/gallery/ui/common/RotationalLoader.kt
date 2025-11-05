package com.google.ai.edge.gallery.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.google.ai.edge.gallery.R

/**
 * PNG 스피너 + 무지개 틴트 사이클
 * - 흰색 PNG(`ic_loading_custom`)를 회전시키며,
 * - 틴트 색상을 HSV hue로 0..360° 순환.
 */
@Composable
fun RotationalLoader(
  size: Dp,
  rotateDurationMs: Int = 1500,
  hueCycleDurationMs: Int = 750,
  saturation: Float = 0.95f,
  value: Float = 1.0f,
) {
  val spin by rememberInfiniteTransition(label = "spin").animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(rotateDurationMs, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "deg"
  )

  val hue by rememberInfiniteTransition(label = "hue").animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(hueCycleDurationMs, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "h"
  )

  // HSV → RGB
  val tint = Color.hsv(hue, saturation, value)

  Image(
    painter = painterResource(R.drawable.ic_loading_custom),
    contentDescription = null,
    modifier = Modifier
      .size(size)
      .graphicsLayer { rotationZ = spin },
    colorFilter = ColorFilter.tint(tint) // ← 무지개 색 순환
  )
}


// 백색 회전
//package com.google.ai.edge.gallery.ui.common
//
//import androidx.compose.animation.core.LinearEasing
//import androidx.compose.animation.core.RepeatMode
//import androidx.compose.animation.core.animateFloat
//import androidx.compose.animation.core.infiniteRepeatable
//import androidx.compose.animation.core.rememberInfiniteTransition
//import androidx.compose.animation.core.tween
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.layout.size
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.graphicsLayer
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.unit.Dp
//import com.google.ai.edge.gallery.R
//
///**
// * PNG 기반 로딩 인디케이터(흰색 스피너 이미지 회전).
// */
//@Composable
//fun RotationalLoader(size: Dp) {
//  val angle by rememberInfiniteTransition(label = "spin")
//    .animateFloat(
//      initialValue = 0f,
//      targetValue = 360f,
//      animationSpec = infiniteRepeatable(
//        animation = tween(durationMillis = 1500, easing = LinearEasing),
//        repeatMode = RepeatMode.Restart
//      ),
//      label = "deg"
//    )
//
//  Image(
//    painter = painterResource(R.drawable.ic_loading_custom),
//    contentDescription = null,
//    modifier = Modifier
//      .size(size)
//      .graphicsLayer { rotationZ = angle }
//  )
//}


//package com.google.ai.edge.gallery.ui.common
//
//import androidx.compose.animation.core.LinearEasing
//import androidx.compose.animation.core.RepeatMode
//import androidx.compose.animation.core.animateFloat
//import androidx.compose.animation.core.infiniteRepeatable
//import androidx.compose.animation.core.rememberInfiniteTransition
//import androidx.compose.animation.core.tween
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.layout.size
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.Path
//import androidx.compose.ui.graphics.StrokeCap
//import androidx.compose.ui.graphics.StrokeJoin
//import androidx.compose.ui.graphics.drawscope.Stroke
//import androidx.compose.ui.unit.Dp
//import androidx.compose.ui.unit.dp
//
///**
// * 심플한 흰색 모래시계 로더.
// * - 외곽선(모래시계)만 흰색.
// * - 윗통의 모래가 아랫통으로 떨어지는 애니메이션 반복.
// * - 배경과 잘 어울리도록 반전 없이 고정 표시.
// */
//@Composable
//fun RotationalLoader(size: Dp) {
//  // 진행률 0f -> 1f : 윗통에서 모래가 사라지고 아랫통이 차는 애니메이션
//  val t by rememberInfiniteTransition(label = "hourglass")
//    .animateFloat(
//      initialValue = 0f,
//      targetValue = 1f,
//      animationSpec = infiniteRepeatable(
//        animation = tween(durationMillis = 1200, easing = LinearEasing),
//        repeatMode = RepeatMode.Restart
//      ),
//      label = "sand"
//    )
//
//  // 로더 색상: 기본 흰색(다크 배경용)
//  val white: Color = Color.White.copy(alpha = 0.92f)
//  val strokeWidth = (size * 0.06f).coerceAtLeast(1.dp)
//
//  Canvas(modifier = Modifier.size(size)) {
//    val w = size.toPx()
//    val h = size.toPx()
//    val pad = 0.12f * w
//    val neckHalf = 0.06f * w         // 목 부분 절반 너비
//    val neckY = h * 0.5f
//    val topY = pad
//    val bottomY = h - pad
//    val leftX = pad
//    val rightX = w - pad
//    val centerX = w * 0.5f
//
//    // ── 1) 외곽선(모래시계 윤곽) ───────────────────────────────────────────────
//    val outline = Path().apply {
//      // 상부 좌→중앙
//      moveTo(leftX, topY)
//      quadraticBezierTo(
//        leftX, (topY + neckY) * 0.5f,
//        centerX - neckHalf, neckY
//      )
//      // 상부 우
//      quadraticBezierTo(
//        (rightX + centerX - neckHalf) * 0.5f, neckY,
//        rightX, topY
//      )
//      // 하부 우→중앙
//      quadraticBezierTo(
//        rightX, (bottomY + neckY) * 0.5f,
//        centerX + neckHalf, neckY
//      )
//      // 하부 좌
//      quadraticBezierTo(
//        (leftX + centerX + neckHalf) * 0.5f, neckY,
//        leftX, bottomY
//      )
//      close()
//    }
//    drawPath(
//      path = outline,
//      color = white,
//      style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
//    )
//
//    // 모래 색(동일 흰색, 약간 더 투명)
//    val sand = white.copy(alpha = 0.85f)
//
//    // ── 2) 윗통 모래(삼각형) : progress = (1 - t)
//    run {
//      val fill = 1f - t               // 1 -> 0
//      if (fill > 0.02f) {
//        val topHeight = (neckY - topY) * fill
//        val topPath = Path().apply {
//          moveTo(centerX, neckY - 0.5f)                   // 목 위 살짝
//          lineTo(centerX - (neckHalf * (0.3f + 0.7f * fill)), neckY - topHeight)
//          lineTo(centerX + (neckHalf * (0.3f + 0.7f * fill)), neckY - topHeight)
//          close()
//        }
//        drawPath(topPath, sand)
//      }
//    }
//
//    // ── 3) 떨어지는 모래 기둥
//    run {
//      // 목에서 아래로 얇은 선 (t가 커질수록 살짝 짧아지게)
//      val streamLen = (bottomY - neckY) * 0.35f * (0.7f + 0.3f * (1f - t))
//      drawLine(
//        color = sand,
//        start = Offset(centerX, neckY + strokeWidth.toPx() * 0.3f),
//        end = Offset(centerX, neckY + streamLen),
//        strokeWidth = (strokeWidth * 0.55f).toPx(),
//        cap = StrokeCap.Round
//      )
//    }
//
//    // ── 4) 아랫통 모래(삼각형) : progress = t
//    run {
//      val fill = t                    // 0 -> 1
//      if (fill > 0.02f) {
//        val baseHeight = (bottomY - neckY) * (0.18f + 0.70f * fill)
//        val bottomPath = Path().apply {
//          moveTo(centerX, neckY + 0.5f)                   // 목 아래 살짝
//          lineTo(centerX - (neckHalf * (0.25f + 0.75f * fill)), neckY + baseHeight)
//          lineTo(centerX + (neckHalf * (0.25f + 0.75f * fill)), neckY + baseHeight)
//          close()
//        }
//        drawPath(bottomPath, sand)
//      }
//    }
//  }
//}

///*
// * Copyright 2025 Google LLC
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.google.ai.edge.gallery.ui.common
//
//import androidx.compose.animation.core.CubicBezierEasing
//import androidx.compose.animation.core.EaseInOut
//import androidx.compose.animation.core.RepeatMode
//import androidx.compose.animation.core.animateFloat
//import androidx.compose.animation.core.infiniteRepeatable
//import androidx.compose.animation.core.rememberInfiniteTransition
//import androidx.compose.animation.core.tween
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.lazy.grid.GridCells
//import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
//import androidx.compose.foundation.lazy.grid.itemsIndexed
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.drawWithContent
//import androidx.compose.ui.graphics.BlendMode
//import androidx.compose.ui.graphics.Brush.Companion.linearGradient
//import androidx.compose.ui.graphics.graphicsLayer
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.semantics.clearAndSetSemantics
//import androidx.compose.ui.unit.Dp
//import com.google.ai.edge.gallery.R
//import com.google.ai.edge.gallery.ui.theme.customColors
//
//private const val GRID_SPACING_FACTOR = 0.1f
//private const val ICON_SIZE_FACTOR = 0.3f
//
///**
// * A composable that displays a rotational and scaling animated loader, structured as a 2x2 grid.
// *
// * This loader uses two concurrent infinite animations:
// * 1. **Outer Rotation (rotationZ):** Continuously rotates the entire [LazyVerticalGrid] container
// *    using a custom [CubicBezierEasing] for a distinct non-linear rotation speed.
// * 2. **Inner Scale (scaleX, scaleY):** Cycles the scale of the individual grid items between 1.0
// *    and 0.4 using [EaseInOut] easing for a smooth pulsing/breathing effect.
// */
//@Composable
//fun RotationalLoader(size: Dp) {
//  val infiniteTransition = rememberInfiniteTransition(label = "infinite")
//  val rotationProgress by
//    infiniteTransition.animateFloat(
//      initialValue = 0f,
//      targetValue = 1f,
//      animationSpec =
//        infiniteRepeatable(
//          animation = tween(2000, easing = CubicBezierEasing(0.5f, 0.16f, 0f, 0.71f)),
//          repeatMode = RepeatMode.Restart,
//        ),
//    )
//  val scaleProgress by
//    infiniteTransition.animateFloat(
//      initialValue = 1f,
//      targetValue = 0.4f,
//      animationSpec =
//        infiniteRepeatable(
//          animation = tween(1000, easing = EaseInOut),
//          repeatMode = RepeatMode.Reverse,
//        ),
//    )
//  val curRotationZ = 45f + rotationProgress * 360f
//  val curScale = scaleProgress
//
//  val gridSpacing = size * GRID_SPACING_FACTOR
//  LazyVerticalGrid(
//    columns = GridCells.Fixed(2),
//    horizontalArrangement = Arrangement.spacedBy(gridSpacing),
//    verticalArrangement = Arrangement.spacedBy(gridSpacing),
//    modifier =
//      Modifier.size(size).graphicsLayer { rotationZ = curRotationZ }.clearAndSetSemantics {},
//  ) {
//    itemsIndexed(
//      listOf(
//        R.drawable.four_circle,
//        R.drawable.circle,
//        R.drawable.double_circle,
//        R.drawable.pantegon,
//      )
//    ) { index, imageResource ->
//      Box(
//        modifier = Modifier.size((size - gridSpacing) / 2),
//        contentAlignment =
//          when (index) {
//            0 -> Alignment.BottomEnd
//            1 -> Alignment.BottomStart
//            2 -> Alignment.TopEnd
//            3 -> Alignment.TopStart
//            else -> Alignment.Center
//          },
//      ) {
//        val colorIndex =
//          when (index) {
//            0 -> 2
//            1 -> 1
//            2 -> 0
//            else -> 3
//          }
//        val brush =
//          linearGradient(colors = MaterialTheme.customColors.taskBgGradientColors[colorIndex])
//        Image(
//          painter = painterResource(id = imageResource),
//          contentDescription = null,
//          modifier =
//            Modifier.size(size * ICON_SIZE_FACTOR)
//              .graphicsLayer {
//                // This is important to make blending mode work.
//                alpha = 0.99f
//                rotationZ = -curRotationZ
//                scaleX = curScale
//                scaleY = curScale
//              }
//              .drawWithContent {
//                drawContent()
//                drawRect(brush = brush, blendMode = BlendMode.SrcIn)
//              },
//          contentScale = ContentScale.Fit,
//        )
//      }
//    }
//  }
//}
