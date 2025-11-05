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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.google.ai.edge.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.data.AppBarAction
import com.google.ai.edge.gallery.data.AppBarActionType
import androidx.compose.material3.TopAppBarDefaults

/** The top app bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryTopAppBar(
  title: String,
  modifier: Modifier = Modifier,
  leftAction: AppBarAction? = null,
  rightAction: AppBarAction? = null,
  scrollBehavior: TopAppBarScrollBehavior? = null,
  subtitle: String = "",
  containerColor: Color = Color.Transparent,
  showContent: Boolean = true,
  showTitle: Boolean = true,
  showLeftIcon: Boolean = true,
  showRightAction: Boolean = true,
) {
  if (!showContent) {
    // ✅ 비어 있는(투명) 앱바: 아이콘/텍스트 모두 제거
    // icon switch on
    CenterAlignedTopAppBar(
      title = { /* no title */ },
      navigationIcon = { /* no nav icon */ },
      actions = { /* no actions */ },
      modifier = modifier,
      scrollBehavior = scrollBehavior,
      colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = containerColor,
        scrolledContainerColor = containerColor,
        navigationIconContentColor = Color.Transparent,
        titleContentColor = Color.Transparent,
        actionIconContentColor = Color.Transparent,
      ),
      // windowInsets는 기본값 유지(Scaffold가 상단 인셋 처리)
    )
    return
  }
  // app bar switch on
  val titleColor = MaterialTheme.colorScheme.onSurface
  val showTitleNow = showTitle && (title.isNotBlank() || subtitle.isNotEmpty())

  CenterAlignedTopAppBar(
    title = {
      if (showTitleNow) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            if (title == stringResource(R.string.app_name)) {
              Icon(
                painterResource(R.drawable.ic_launcher_round),
                modifier = Modifier.size(20.dp),
                contentDescription = null,
                tint = Color.Unspecified,
              )
            }
            BasicText(
              text = title,
              maxLines = 1,
              color = { titleColor },
              style = MaterialTheme.typography.titleMedium,
              autoSize = TextAutoSize.StepBased(
                minFontSize = 14.sp, maxFontSize = 16.sp, stepSize = 1.sp
              ),
            )
          }
          if (subtitle.isNotEmpty()) {
            Text(
              subtitle,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.secondary,
            )
          }
        }
      }
    },
    modifier = modifier,
    scrollBehavior = scrollBehavior,
    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
      containerColor = containerColor,
      scrolledContainerColor = containerColor,
      navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
      titleContentColor = MaterialTheme.colorScheme.onSurface,
      actionIconContentColor = MaterialTheme.colorScheme.onSurface,
    ),
    navigationIcon = {
      if (showLeftIcon) {
        when (leftAction?.actionType) {
          AppBarActionType.NAVIGATE_UP -> {
            IconButton(onClick = leftAction.actionFn) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.cd_navigate_back_icon),
              )
            }
          }
          else -> {}
        }
      }
    },
    actions = {
      if (showRightAction) {
        when (rightAction?.actionType) {
          AppBarActionType.APP_SETTING -> {
            IconButton(onClick = rightAction.actionFn) {
              Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.cd_app_settings_icon),
                tint = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
          AppBarActionType.NAVIGATE_UP -> {
            TextButton(onClick = rightAction.actionFn) { Text("Done") }
          }
          else -> {}
        }
      }
    },
  )
}

//@Composable
//fun GalleryTopAppBar(
//  title: String,
//  modifier: Modifier = Modifier,
//  leftAction: AppBarAction? = null,
//  rightAction: AppBarAction? = null,
//  scrollBehavior: TopAppBarScrollBehavior? = null,
//  subtitle: String = "",
//) {
//  val titleColor = MaterialTheme.colorScheme.onSurface
//  CenterAlignedTopAppBar(
//    title = {
//      Column(horizontalAlignment = Alignment.CenterHorizontally) {
//        Row(
//          verticalAlignment = Alignment.CenterVertically,
//          horizontalArrangement = Arrangement.spacedBy(12.dp),
//        ) {
//          if (title == stringResource(R.string.app_name)) {
//            Icon(
//              painterResource(R.drawable.logo),
//              modifier = Modifier.size(20.dp),
//              contentDescription = null,
//              tint = Color.Unspecified,
//            )
//          }
//          BasicText(
//            text = title,
//            maxLines = 1,
//            color = { titleColor },
//            style = MaterialTheme.typography.titleMedium,
//            autoSize =
//              TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 16.sp, stepSize = 1.sp),
//          )
//        }
//        if (subtitle.isNotEmpty()) {
//          Text(
//            subtitle,
//            style = MaterialTheme.typography.labelSmall,
//            color = MaterialTheme.colorScheme.secondary,
//          )
//        }
//      }
//    },
//    modifier = modifier,
//    scrollBehavior = scrollBehavior,
//    // The button at the left.
//    navigationIcon = {
//      when (leftAction?.actionType) {
//        AppBarActionType.NAVIGATE_UP -> {
//          IconButton(onClick = leftAction.actionFn) {
//            Icon(
//              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
//              contentDescription = stringResource(R.string.cd_navigate_back_icon),
//            )
//          }
//        }
//
//        else -> {}
//      }
//    },
//    // The "action" component at the right.
//    actions = {
//      when (rightAction?.actionType) {
//        // Click an icon to open "app setting".
//        AppBarActionType.APP_SETTING -> {
//          IconButton(onClick = rightAction.actionFn) {
//            Icon(
//              imageVector = Icons.Rounded.Settings,
//              contentDescription = stringResource(R.string.cd_app_settings_icon),
//              tint = MaterialTheme.colorScheme.onSurface,
//            )
//          }
//        }
//
//        // Click a button to navigate up.
//        AppBarActionType.NAVIGATE_UP -> {
//          TextButton(onClick = rightAction.actionFn) { Text("Done") }
//        }
//
//        else -> {}
//      }
//    },
//  )
//}
