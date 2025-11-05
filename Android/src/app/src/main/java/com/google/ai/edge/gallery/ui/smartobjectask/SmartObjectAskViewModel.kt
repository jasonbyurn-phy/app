package com.google.ai.edge.gallery.ui.smartobjectask

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetectedObject(
    val label: String,
    val score: Float,
    val boundingBox: RectF
)

data class SmartObjectAskUiState(
    val originalImage: Bitmap? = null,
    val detectedObjects: List<DetectedObject> = emptyList(),
    val selectedObjectIndex: Int? = null,
    val croppedImage: Bitmap? = null,
    val isDetecting: Boolean = false,
    val errorMessage: String = "",
    val imageLoadTimestamp: Long = 0L,
    val resetCounter: Int = 0
)

@HiltViewModel
class SmartObjectAskViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SmartObjectAskUiState())
    val uiState = _uiState.asStateFlow()

    fun loadImage(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val bitmap = loadBitmapFromUri(uri, context)
                val timestamp = System.currentTimeMillis()
                println("📷 ViewModel loadImage 호출 - timestamp: $timestamp")
                _uiState.value = _uiState.value.copy(
                    originalImage = bitmap,
                    detectedObjects = emptyList(),
                    selectedObjectIndex = null,
                    croppedImage = null,
                    isDetecting = false,
                    errorMessage = "",
                    imageLoadTimestamp = timestamp,
                    resetCounter = _uiState.value.resetCounter + 1
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "이미지 로드 실패: ${e.message}",
                    isDetecting = false
                )
            }
        }
    }

    fun setCustomSelection(boundingBox: RectF) {
        _uiState.value = _uiState.value.copy(
            detectedObjects = listOf(DetectedObject("사용자 선택 영역", 1.0f, boundingBox)),
            selectedObjectIndex = 0
        )
        cropCustomSelection(boundingBox)
    }

    private fun cropCustomSelection(rect: RectF) {
        val bitmap = _uiState.value.originalImage ?: return

        try {
            val x = rect.left.toInt().coerceIn(0, bitmap.width - 1)
            val y = rect.top.toInt().coerceIn(0, bitmap.height - 1)
            val w = rect.width().toInt().coerceIn(1, bitmap.width - x)
            val h = rect.height().toInt().coerceIn(1, bitmap.height - y)

            val croppedBitmap = Bitmap.createBitmap(bitmap, x, y, w, h)

            _uiState.value = _uiState.value.copy(croppedImage = croppedBitmap)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "이미지 크롭 실패: ${e.message}"
            )
        }
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            detectedObjects = emptyList(),
            selectedObjectIndex = null,
            croppedImage = null,
            resetCounter = _uiState.value.resetCounter + 1
        )
        println("🔄 clearSelection 호출 - resetCounter: ${_uiState.value.resetCounter}")
    }

    private fun loadBitmapFromUri(uri: Uri, context: Context): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Failed to open stream")
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        return try {
            val exifStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = androidx.exifinterface.media.ExifInterface(exifStream)
            val orientation = exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )
            exifStream.close()

            rotateBitmap(bitmap, orientation)
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = android.graphics.Matrix()
        when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }
}