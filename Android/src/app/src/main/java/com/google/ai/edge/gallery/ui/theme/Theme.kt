/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.ai.edge.gallery.proto.Theme
// for transparent
import android.os.Build
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
private val lightScheme =
  lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
  )

private val darkScheme =
  darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
  )

@Immutable
data class CustomColors(
  val appTitleGradientColors: List<Color> = listOf(),
  val tabHeaderBgColor: Color = Color.Transparent,
  val taskCardBgColor: Color = Color.Transparent,
  val taskBgColors: List<Color> = listOf(),
  val taskBgGradientColors: List<List<Color>> = listOf(),
  val taskIconColors: List<Color> = listOf(),
  val taskIconShapeBgColor: Color = Color.Transparent,
  val homeBottomGradient: List<Color> = listOf(),
  val userBubbleBgColor: Color = Color.Transparent,
  val agentBubbleBgColor: Color = Color.Transparent,
  val linkColor: Color = Color.Transparent,
  val successColor: Color = Color.Transparent,
  val recordButtonBgColor: Color = Color.Transparent,
  val waveFormBgColor: Color = Color.Transparent,
  val modelInfoIconColor: Color = Color.Transparent,
)

val LocalCustomColors = staticCompositionLocalOf { CustomColors() }

val lightCustomColors =
  CustomColors(
    // 타이틀 그라디언트: 메인1 ↔ 보색1
    appTitleGradientColors = listOf(Color(0xFF5A8472), Color(0xFF845A6C)),
    // 탭 배경: 메인2
    tabHeaderBgColor = Color(0xFF62734C),
    // 카드 배경: 기존 유지(머티리얼 컨테이너)
    taskCardBgColor = Color(0xFF00A292),
//      surfaceContainerLowestLight,

    // 타일(카드) 단색 배경 4종: 메인 계열
    taskBgColors = listOf(
      Color(0xFF5A8472), // 메인1
      Color(0xFF62734C), // 메인2
      Color(0xFF31564E), // 메인4
      Color(0xFF9D8633), // 메인3
    ),

    // 타일 그라디언트 4종: (메인 ↔ 보색) 페어
    taskBgGradientColors = listOf(
      listOf(Color(0xFFFFEB3B), Color(0xFFE91E63)), // 1
      listOf(Color(0xFFFFEB3B), Color(0xFF009688)), // 2
      listOf(Color(0xFFFF5722), Color(0xFFF6E763)), // 4
      listOf(Color(0xFF3F51B5), Color(0xFF9C27B0)), // 3
    ),

    // 아이콘 컬러: 배경과 대비되게 보색 측 위주
    taskIconColors = listOf(
      Color(0xFFFFFFFF), // 보색1
      Color(0xFFFF0000), // 보색2
      Color(0xFFFFE500), // 보색3
      Color(0xFF008EFF), // 보색4
    ),

    taskIconShapeBgColor = Color.White,

    // 홈 바닥 그라디언트: 투명 → 메인1 약한 틴트
    homeBottomGradient = listOf(Color(0x00000000), Color(0x1A5A8472)),

    // 채팅 버블/링크(가독성 유지하며 살짝 통일)
    agentBubbleBgColor = Color(0xFFE9EEF6),
    userBubbleBgColor  = Color(0xFF1E274C), // 보색8(딥 블루)
    linkColor          = Color(0xFF334A9D), // 보색3

    successColor = Color(0xFF3D860B),
    recordButtonBgColor = Color(0xFFFF9F00), // 메인7(브랜드 버튼)
    waveFormBgColor = Color(0xFFAAAAAA),
    modelInfoIconColor = Color(0xFFCCCCCC),
  )

val darkCustomColors =
  CustomColors(
    // 다크 타이틀 그라디언트: 보색 ↔ 메인 (살짝 반전)
    appTitleGradientColors = listOf(Color(0xFF63b071), Color(0xFF1823fb), Color(0xFFde3635)),
    // 탭 배경: 딥 뉴트럴(보색6)
    tabHeaderBgColor = Color(0xFF1E1B21),
    taskCardBgColor = surfaceContainerHighDark,

    // 타일 단색 배경 4종: 어두운 보색/메인 톤
    taskBgColors = listOf(
      Color(0xFF000000), // 보색6(뉴트럴 딥)
      Color(0xFFE3FFD6), // 메인6(뉴트럴 딥)
      Color(0xFF668AFF), // 보색8(딥 블루)
      Color(0xFF1A70FF), // 보색7(딥 블루)
    ),

    // 타일 그라디언트: (보색 ↔ 메인) 페어
    taskBgGradientColors = listOf(
      listOf(Color(0xFFFFEB3B), Color(0xFF2196F3)), // 1
      listOf(Color(0xFFFFEB3B), Color(0xFF009688)), // 2
      listOf(Color(0xFF31B7AA), Color(0xFF00A0FF)), // 4
      listOf(Color(0xFF3F51B5), Color(0xFF9C27B0)), // 3
    ),
//    taskBgGradientColors = listOf(
//      listOf(Color(0xFF563139), Color(0xFF5A8472)), // 보색4 ↔ 메인1
//      listOf(Color(0xFF5D4C73), Color(0xFF62734C)), // 보색2 ↔ 메인2
//      listOf(Color(0xFF334A9D), Color(0xFF9D8633)), // 보색3 ↔ 메인3
//      listOf(Color(0xFF1E274C), Color(0xFF31564E)), // 보색8 ↔ 메인4
//    ),

    // 아이콘 컬러: 다크에선 메인/보색 중 상대적으로 밝은 톤
    taskIconColors = listOf(
      Color(0xFFACFFDD), // 메인1
      Color(0xFFFFDD55), // 메인3
      Color(0xFFFFB0D0), // 보색1
      Color(0xFF69A2FF), // 보조(가독 보정 원하면 유지/조정)
    ),

    taskIconShapeBgColor = Color(0x80000000),

    homeBottomGradient = listOf(Color(0x00000000), Color(0x1A0C377F)), // 투명 → 보색7 약한 틴트

    agentBubbleBgColor = Color(0x80000000),
    userBubbleBgColor  = Color(0x80000000),
    linkColor          = Color(0xFF9DCAFC),

    successColor = Color(0xFFA1CE83),
    recordButtonBgColor = Color(0xFF00BCD4),
    waveFormBgColor = Color(0xFFAAAAAA),
    modelInfoIconColor = Color(0xFFCCCCCC),
  )

val MaterialTheme.customColors: CustomColors
  @Composable @ReadOnlyComposable get() = LocalCustomColors.current

/**
 * Controls the color of the phone's status bar icons based on whether the app is using a dark
 * theme.
 */
@Composable
fun StatusBarColorController(
  useDarkTheme: Boolean,
  statusBarColor: Color = Color.Transparent,       // 상태바 색
  navigationBarColor: Color = Color.Transparent,   // 내비게이션바 색
  lightIcons: Boolean = false,                     // 아이콘을 '밝게 보이게'가 아니라
  // Android 용어 기준: true = '라이트 모드 아이콘' = 검정 아이콘
  // false = '다크 모드 아이콘' = 흰색 아이콘
  disableNavBarContrast: Boolean = true            // 삼성 등 대비 강제 끄기
) {
  val view = LocalView.current
  val window = (view.context as? Activity)?.window ?: return

  SideEffect {
    // 컨텐츠를 시스템바 영역까지 그리기 (edge-to-edge)
    WindowCompat.setDecorFitsSystemWindows(window, false)

    // 바 색 적용
    window.statusBarColor = statusBarColor.toArgb()
    window.navigationBarColor = navigationBarColor.toArgb()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      // (선택) 하단 디바이더 제거
      window.navigationBarDividerColor = Color.Transparent.toArgb()
    }

    // 아이콘 색(라이트/다크) 설정
    val controller = WindowInsetsControllerCompat(window, window.decorView).apply {
      // true  = 라이트 시스템바(밝은 배경 가정) → '검정 아이콘'
      // false = 다크  시스템바(어두운 배경 가정) → '흰색 아이콘'
      isAppearanceLightStatusBars = lightIcons
      isAppearanceLightNavigationBars = lightIcons
    }

    // Android 10+ 내비게이션바 대비 강제 끄기(검은 띠 방지)
    if (disableNavBarContrast && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.isNavigationBarContrastEnforced = false
    }
  }
}
//@Composable
//fun StatusBarColorController(useDarkTheme: Boolean) {
//  val view = LocalView.current
//  val currentWindow = (view.context as? Activity)?.window
//
//  if (currentWindow != null) {
//    SideEffect {
//      WindowCompat.setDecorFitsSystemWindows(currentWindow, false)
//      val controller = WindowCompat.getInsetsController(currentWindow, view)
//      controller.isAppearanceLightStatusBars = !useDarkTheme // Set to true for light icons
//    }
//  }
//}

@Composable
fun GalleryTheme(content: @Composable () -> Unit) {
  val themeOverride = ThemeSettings.themeOverride
  val darkTheme: Boolean =
    (isSystemInDarkTheme() || themeOverride.value == Theme.THEME_DARK) &&
      themeOverride.value != Theme.THEME_LIGHT

  StatusBarColorController(useDarkTheme = darkTheme)

  val colorScheme =
    when {
      darkTheme -> darkScheme
      else -> lightScheme
    }

  val customColorsPalette = if (darkTheme) darkCustomColors else lightCustomColors

  CompositionLocalProvider(LocalCustomColors provides customColorsPalette) {
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
  }
}
