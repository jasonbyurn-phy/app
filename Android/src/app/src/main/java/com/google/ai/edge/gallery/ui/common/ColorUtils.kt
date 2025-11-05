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

package com.google.ai.edge.gallery.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.theme.customColors

import kotlin.math.absoluteValue
import com.google.ai.edge.gallery.data.BuiltInTaskId

private fun positiveMod(x: Int, m: Int): Int = ((x % m) + m) % m
private fun fallbackIndex(key: String?, size: Int): Int =
  (key?.hashCode() ?: 0).let { kotlin.math.abs(it) % size }

@Composable
fun getTaskBgColor(task: Task): Color {
  val size = MaterialTheme.customColors.taskBgColors.size
  val idx = if (task.index >= 0) positiveMod(task.index, size) else fallbackIndex(task.id, size)
  return MaterialTheme.customColors.taskBgColors[idx]
}

@Composable
fun getTaskBgGradientColors(task: Task): List<Color> {
  val size = MaterialTheme.customColors.taskBgGradientColors.size
  val idx = if (task.index >= 0) positiveMod(task.index, size) else fallbackIndex(task.id, size)
  return MaterialTheme.customColors.taskBgGradientColors[idx]
}

@Composable
fun getTaskIconColor(task: Task): Color {
  val size = MaterialTheme.customColors.taskIconColors.size
  val idx = if (task.index >= 0) positiveMod(task.index, size) else fallbackIndex(task.id, size)
  return MaterialTheme.customColors.taskIconColors[idx]
}

@Composable
fun getTaskIconColor(index: Int): Color {
  val size = MaterialTheme.customColors.taskIconColors.size
  val idx = positiveMod(index, size)
  return MaterialTheme.customColors.taskIconColors[idx]
}
