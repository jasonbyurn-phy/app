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

package com.google.ai.edge.gallery

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.gallery.ui.theme.GalleryTheme
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// for transparent
import android.graphics.Color as AColor
import androidx.compose.ui.zIndex
// only one theme
import androidx.appcompat.app.AppCompatDelegate
// for initiated effect
import com.google.ai.edge.gallery.ui.splash.BrushStrokeRevealOverlay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  private val modelManagerViewModel: ModelManagerViewModel by viewModels()

  // ✅ 싱글톤 기반 MobileCLIPHelper
  private val mobileCLIP: MobileCLIPHelper by lazy {
    MobileCLIPHelper.getInstance(this)
  }
  private var firebaseAnalytics: FirebaseAnalytics? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    // 전역 다크 고정
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    super.onCreate(savedInstanceState)

    // --- OS 스플래시 설치 및 기본 종료 애니 제거(아이콘 회전 X) ---
    val splash = installSplashScreen()
    splash.setOnExitAnimationListener { it.remove() }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.isNavigationBarContrastEnforced = false
    }
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    // Firebase & Model 초기화
    firebaseAnalytics = FirebaseAnalytics.getInstance(this)
    modelManagerViewModel.loadModelAllowlist()

    // ✅ Compose UI
    setContent {
      GalleryTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          // 루트: 전체 화면 (배경/스플래시 마스크는 패딩 없이 꽉 채움)
          Box(modifier = Modifier.fillMaxSize()) {

            // ✅ 메인 콘텐츠 레이어: 시스템 바만큼 패딩
            Box(
              modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()   // 겹침 방지
            ) {
              // 메인 콘텐츠
              GalleryApp(modelManagerViewModel = modelManagerViewModel)

              // 3) 붓터치 리빌 오버레이 (최상단)
              BrushStrokeRevealOverlay(
                // preDelayMs = 500,
                 sweepDurationMs = 3000,
                 oscillations = 4,
                 postDelayBeforeFadeMs = 0,
                 fadeOutMs = 1000,
                onFinished = { /* 필요시 처리 */ }
              )

            }
          }
        }
      }
    }
  }


  override fun onResume() {
    super.onResume()
    firebaseAnalytics?.logEvent(
      FirebaseAnalytics.Event.APP_OPEN,
      bundleOf(
        "app_version" to BuildConfig.VERSION_NAME,
        "os_version" to Build.VERSION.SDK_INT.toString(),
        "device_model" to Build.MODEL,
      ),
    )
  }

  override fun onDestroy() {
    super.onDestroy()
    try {
      mobileCLIP.close()
      Log.d(TAG, "MobileCLIP closed")
    } catch (e: Exception) {
      Log.w(TAG, "MobileCLIP close 실패: ${e.message}")
    }
  }

  companion object {
    private const val TAG = "MainActivity"
  }
}
