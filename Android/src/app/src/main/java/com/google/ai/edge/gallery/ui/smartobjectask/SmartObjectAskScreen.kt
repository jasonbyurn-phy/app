package com.google.ai.edge.gallery.ui.smartobjectask

import android.content.Context  // ⭐ 추가!
import kotlinx.coroutines.launch
import com.google.ai.edge.gallery.MobileCLIPHelper
import android.graphics.Bitmap
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.RectF
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.max
import kotlin.math.min
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.ui.llmchat.LlmChatViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.ui.llmchat.ChatViewWrapper
import com.google.ai.edge.gallery.ui.llmchat.LlmAskImageViewModel
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs

// tts
import com.google.ai.edge.gallery.tts.TtsManager
import androidx.compose.ui.geometry.Rect as ComposeRect
import kotlinx.coroutines.delay
import android.widget.Toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.LiveData
import java.lang.reflect.Field
import java.lang.reflect.Method
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
// 🔗 RAG 추가
import com.google.ai.edge.gallery.rag.RagService
import android.util.Log

// IMAGE RAG
import com.google.ai.edge.gallery.rag.ImageRagIndex
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.DisposableEffect

// prompt
import com.google.ai.edge.gallery.prompt.PromptManager
import com.google.ai.edge.gallery.prompt.PromptManager.PromptStyle

import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider

// for image embedding
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Interpreter.Options
import org.tensorflow.lite.support.common.FileUtil

// ✅ 유지/추가
import com.google.ai.edge.gallery.rag.ImageEmbedderApi

// 임계치 상수 (원하면 상단에 두고 재사용)
private const val SIMILARITY_THRESHOLD = 0.998f

// embedding
// ====== (파일-프라이빗) 리플렉션 도우미들 ======
private fun String.upperFirst(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

private fun Any?.asStringOrNull(): String? = when (this) {
    null -> null
    is CharSequence -> this.toString()
    else -> null
}

private fun Any?.tryGetFieldAnyCase(name: String): Any? {
    val target = this ?: return null
    var c: Class<*>? = target::class.java
    while (c != null) {
        val f = runCatching { c.declaredFields.firstOrNull { it.name.equals(name, true) } }.getOrNull()
        if (f != null) {
            runCatching { f.isAccessible = true }.getOrNull()
            return runCatching { f.get(target) }.getOrNull()
        }
        c = c.superclass
    }
    return null
}

private fun Any?.tryCallGetterAnyCase(name: String): Any? {
    val target = this ?: return null
    val candidates = listOf(name, "get${name.upperFirst()}")
    var c: Class<*>? = target::class.java
    while (c != null) {
        val m = runCatching {
            c.declaredMethods.firstOrNull { it.parameterCount == 0 && candidates.any { n -> n.equals(it.name, true) } }
                ?: c.methods.firstOrNull { it.parameterCount == 0 && candidates.any { n -> n.equals(it.name, true) } }
        }.getOrNull()
        if (m != null) {
            runCatching { m.isAccessible = true }.getOrNull()
            return runCatching { m.invoke(target) }.getOrNull()
        }
        c = c.superclass
    }
    return null
}

private fun unwrapStateWrappers(any: Any?): Any? {
    var cur = any ?: return null
    repeat(3) {
        val tmp = cur
        cur = when (tmp) {
            is kotlinx.coroutines.flow.StateFlow<*> -> tmp.value ?: return@repeat
            is androidx.lifecycle.LiveData<*> -> tmp.value ?: return@repeat
            is Result<*> -> (tmp as Result<Any?>).getOrNull() ?: return@repeat
            else -> {
                val next = tmp.tryGetFieldAnyCase("value") ?: tmp.tryCallGetterAnyCase("value")
                ?: tmp.tryGetFieldAnyCase("data") ?: tmp.tryCallGetterAnyCase("data")
                ?: tmp.tryGetFieldAnyCase("result") ?: tmp.tryCallGetterAnyCase("result")
                ?: tmp.tryGetFieldAnyCase("state") ?: tmp.tryCallGetterAnyCase("state")
                next ?: return@repeat
            }
        }
    }
    return cur
}

private fun extractRoleLower(objIn: Any?): String? {
    val obj = unwrapStateWrappers(objIn) ?: return null
    val keys = listOf("role", "sender", "author", "from", "speaker")
    for (k in keys) {
        (obj.tryGetFieldAnyCase(k) as? CharSequence)?.toString()?.lowercase()?.let { return it }
        (obj.tryCallGetterAnyCase(k) as? CharSequence)?.toString()?.lowercase()?.let { return it }
        runCatching {
            obj.tryGetFieldAnyCase(k)?.let {
                it::class.java.methods.firstOrNull { m -> m.name == "name" && m.parameterCount == 0 }
                    ?.invoke(it) as? String
            }
        }.getOrNull()?.lowercase()?.let { return it }
    }
    return null
}

private fun deepFindAnyTextish(objIn: Any?, depth: Int = 0): String? {
    if (objIn == null || depth > 7) return null
    val obj = unwrapStateWrappers(objIn) ?: return null

    (obj as? CharSequence)?.toString()?.trim()?.let { if (it.isNotEmpty()) return it }

    val textKeys = listOf(
        "finalText","resolvedText","responseText","displayText","markdown","plainText",
        "text","content","body","message","title","answer","output","reasoning","summary"
    )
    for (k in textKeys) {
        (obj.tryGetFieldAnyCase(k) as? CharSequence)?.toString()?.trim()?.let { if (it.isNotEmpty()) return it }
        (obj.tryCallGetterAnyCase(k) as? CharSequence)?.toString()?.trim()?.let { if (it.isNotEmpty()) return it }
    }
    listOf("getText","getContent","toMarkdown","asString").forEach { m ->
        runCatching {
            obj::class.java.methods.firstOrNull {
                it.name.equals(m, true) && it.parameterCount == 0 && it.returnType == String::class.java
            }?.invoke(obj) as? String
        }.getOrNull()?.trim()?.let { if (it.isNotEmpty()) return it }
    }

    val listKeys = listOf(
        "parts","items","elements","spans","candidates","messages","history","conversation",
        "buffer","chunks","nodes","children","paragraphs","segments","turns","entries"
    )
    for (k in listKeys) {
        val v = obj.tryGetFieldAnyCase(k) ?: obj.tryCallGetterAnyCase(k)
        if (v is List<*>) for (p in v.asReversed()) {
            deepFindAnyTextish(p, depth + 1)?.let { if (it.isNotBlank()) return it }
        }
    }

    if (obj is Map<*, *>) {
        val keyOrder = listOf("finalText","text","content","body","message","markdown","plainText","title","answer","output")
        for (k in keyOrder) (obj[k] as? CharSequence)?.toString()?.trim()?.let { if (it.isNotEmpty()) return it }
        obj.values.forEach { deepFindAnyTextish(it, depth + 1)?.let { t -> if (t.isNotBlank()) return t } }
    }

    extractRoleLower(obj)?.let {
        val tkeys = listOf("text","content","body","message","markdown","plainText","answer","output")
        for (k in tkeys) {
            ((obj.tryGetFieldAnyCase(k) as? CharSequence) ?: (obj.tryCallGetterAnyCase(k) as? CharSequence))
                ?.toString()?.trim()?.let { if (it.isNotEmpty()) return it }
        }
    }

    val stateKeys = listOf(
        "uiState","chatUiState","state","data","adapter","chat","store","holder",
        "conversationState","displayState","uiModel","buffer","stream","response","result"
    )
    for (k in stateKeys) {
        val v = obj.tryGetFieldAnyCase(k) ?: obj.tryCallGetterAnyCase(k)
        deepFindAnyTextish(v, depth + 1)?.let { if (it.isNotBlank()) return it }
        if (v is List<*>) fromMessageListPickLastAssistant(v as List<Any?>)?.let { return it }
    }

    return null
}

private fun fromMessageListPickLastAssistant(listIn: List<Any?>): String? {
    val list = listIn.map { unwrapStateWrappers(it) }
    val assistantKeys = setOf("assistant", "model", "ai", "bot", "system")
    val reversed = list.asReversed()
    val roleMatch = reversed.firstOrNull { m -> extractRoleLower(m) in assistantKeys }
    deepFindAnyTextish(roleMatch)?.let { if (it.isNotBlank()) return it }
    return deepFindAnyTextish(reversed.firstOrNull())
}

private fun extractLastAssistantTextViaReflection(vm: Any?): String? {
    if (vm == null) return null

    val rootKeys = listOf("messages","conversation","history","items","candidates","log")
    for (k in rootKeys) {
        val v = unwrapStateWrappers(vm.tryGetFieldAnyCase(k) ?: vm.tryCallGetterAnyCase(k))
        if (v is List<*>) fromMessageListPickLastAssistant(v as List<Any?>)?.let { return it }
        deepFindAnyTextish(v)?.let { if (it.isNotBlank()) return it }
    }

    run {
        val getters = listOf("getChatUiState","chatUiState","getUiState","uiState","getState","state")
        for (g in getters) {
            val st = vm.tryCallGetterAnyCase(g) ?: continue
            val inner = unwrapStateWrappers(st) ?: continue
            deepFindAnyTextish(inner)?.let { if (it.isNotBlank()) return it }
            for (k in listOf("messages","conversation","history","items","candidates","log","turns","entries","parts")) {
                val v = (inner as Any).tryGetFieldAnyCase(k) ?: inner.tryCallGetterAnyCase(k)
                val un = unwrapStateWrappers(v)
                when (un) {
                    is List<*> -> fromMessageListPickLastAssistant(un as List<Any?>)?.let { return it }
                    else -> deepFindAnyTextish(un)?.let { if (it.isNotBlank()) return it }
                }
            }
        }
    }

    val stateKeys = listOf(
        "uiState","chatUiState","state","data","adapter","chat","store","holder",
        "conversationState","displayState","uiModel","buffer","stream","response","result"
    )
    for (k in stateKeys) {
        val v = unwrapStateWrappers(vm.tryGetFieldAnyCase(k) ?: vm.tryCallGetterAnyCase(k))
        if (v is List<*>) fromMessageListPickLastAssistant(v as List<Any?>)?.let { return it }
        deepFindAnyTextish(v)?.let { if (it.isNotBlank()) return it }
    }

    vm::class.java.declaredFields.forEach { f ->
        runCatching { f.isAccessible = true }.getOrNull()
        val v = unwrapStateWrappers(runCatching { f.get(vm) }.getOrNull())
        if (v is List<*>) fromMessageListPickLastAssistant(v as List<Any?>)?.let { return it }
        deepFindAnyTextish(v)?.let { if (it.isNotBlank()) return it }
    }
    vm::class.java.methods.filter { it.parameterCount == 0 }.forEach { m ->
        val v = runCatching { m.invoke(vm) }.getOrNull()?.let { unwrapStateWrappers(it) }
        if (v is List<*>) fromMessageListPickLastAssistant(v as List<Any?>)?.let { return it }
        deepFindAnyTextish(v)?.let { if (it.isNotBlank()) return it }
    }
    return null
}

private const val DEFAULT_QUESTION = "무슨 그림이야?"
// ==================== 화면 ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartObjectAskScreen(
    modelManagerViewModel: ModelManagerViewModel,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SmartObjectAskViewModel = hiltViewModel(),
    askImageViewModel: LlmAskImageViewModel = hiltViewModel(),
) {

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // for image embedding
    // 🟢 교체
    val mobileCLIP = remember { MobileCLIPHelper(context) }

    // for crop
    var showChatView by remember { mutableStateOf(false) }
    var croppedImageForChat: Bitmap? by remember { mutableStateOf(null) }

    // 입력 방식 선택
    var showInputMethodDialog by remember { mutableStateOf(false) }

    var showVoiceRecorder by remember { mutableStateOf(false) }
    var showTextInput by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }

    // for tts
    val chatVm: LlmChatViewModel = hiltViewModel()
    var lastAnswerText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val tts = remember { TtsManager(context) }
    var isTtsSpeaking by remember { mutableStateOf(false) }   // 🔴 읽기/중지 토글 상태
    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }
    // 이미지 소스 다이얼로그
    var showImageSourceDialog by remember { mutableStateOf(false) }

    // 카메라 URI
    var cameraImageUri: Uri? by remember { mutableStateOf(null) }

    // 카메라 런처
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraImageUri
        if (success && uri != null) {
            viewModel.clearSelection()
            viewModel.loadImage(uri, context)
        }
    }

    // 카메라 권한 요청
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }
    
    // 프롬프트 스타일 상태
    var selectedPromptStyle by remember {
        mutableStateOf(PromptStyle.DOCENT_VANGOGH_KO)
    }

    fun openImageSourceDialog() { showImageSourceDialog = true }

    fun launchCamera() {
        val permission = Manifest.permission.CAMERA
        when {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                cameraImageUri = uri
                cameraLauncher.launch(uri)
            }
            else -> cameraPermissionLauncher.launch(permission)
        }
    }

    // 음성 인식 런처
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isRecording = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(
                android.speech.RecognizerIntent.EXTRA_RESULTS
            )
            recognizedText = matches?.firstOrNull() ?: ""
            if (recognizedText.isNotEmpty()) {
                showVoiceRecorder = false
                showChatView = true
            } else {
                showVoiceRecorder = false
            }
        } else {
            showVoiceRecorder = false
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.clearSelection()
            viewModel.loadImage(it, context)
        }
    }

    // 화면이 살아있는 동안 askImageViewModel에서 마지막 답변을 계속 캐싱
    AssistantAnswerTapMany(
        vms = listOf(chatVm, askImageViewModel, viewModel),   // 우선순위: chatVm → ask → smart VM
        active = true,
    ) { txt: String ->
        lastAnswerText = txt
    }

    // 녹음 시작
    LaunchedEffect(showVoiceRecorder) {
        if (showVoiceRecorder && !isRecording) {
            isRecording = true
            val intent =
                android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .apply {
                        putExtra(
                            android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                        putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "질문을 말씀해주세요")
                    }
            speechRecognizerLauncher.launch(intent)
        }
    }

    // 이미지 소스 선택 다이얼로그
    if (showImageSourceDialog) {
        ImageSourceSelectionDialog(
            onDismiss = { showImageSourceDialog = false },
            onCameraSelected = {
                showImageSourceDialog = false
                launchCamera()
            },
            onGallerySelected = {
                showImageSourceDialog = false
                imagePickerLauncher.launch("image/*")
            }
        )
    }

    // 입력 방식 선택 다이얼로그
    if (showInputMethodDialog) {
        InputMethodSelectionDialog(
            onDismiss = { showInputMethodDialog = false },
            onVoiceSelected = {
                showInputMethodDialog = false
                showVoiceRecorder = true
            },
            onTextSelected = {
                showInputMethodDialog = false
                showTextInput = true
            }
        )
    }

    // 텍스트 입력 다이얼로그
    if (showTextInput) {
        TextInputDialog(
            onDismiss = {
                showTextInput = false
                croppedImageForChat = null
            },
            onConfirm = { text
                -> recognizedText = text
                showTextInput = false
                showChatView = true }
        )
    }

    // 🔗 RAG 서비스 준비
    val rag = remember(context) { RagService(context) }
    LaunchedEffect(Unit) { rag.indexAssets() }

    val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
//
//    // 화면 벗어날 때 정리
//    DisposableEffect(Unit) { onDispose { imageEmbedder.close() } }

    // 음성 인식 결과 처리 + 🔗 RAG 프롬프트 주입 + ChatView 화면
    if (showChatView && croppedImageForChat != null) {
        LaunchedEffect(showChatView, croppedImageForChat, recognizedText) {
            val selectedModel = modelManagerUiState.selectedModel ?: return@LaunchedEffect

            val crop = croppedImageForChat!!
            fun computeQ(raw: String?): String =
                raw?.trim().orEmpty().ifBlank { DEFAULT_QUESTION }
//            val submitQuestion: (String?) -> Unit = { raw ->
//                val q = computeQ(raw)
//                recognizedText = q
//                showChatView = true
//            }
            val qq = computeQ(recognizedText) // default_question
            val img = Bitmap.createScaledBitmap(crop, 224, 224, true)
            // 🔍 디버깅
            Log.d("SmartObjectAsk", "=== CLIP 검색 디버깅 ===")
            Log.d("SmartObjectAsk", "이미지 크기: ${img.width}x${img.height}")
            // ⭐⭐⭐ 여기부터 추가! ⭐⭐⭐
            // 🧪 테스트 1: 임베딩이 정상인가?
            val testEmbedding = try {
                mobileCLIP.getImageEmbedding(img)
            } catch (e: Exception) {
                Log.e("SmartObjectAsk", "❌ 임베딩 생성 실패: ${e.message}")
                null
            }

            if (testEmbedding != null) {
                Log.d("SmartObjectAsk", "✅ 임베딩 생성 성공")
                Log.d("SmartObjectAsk", "  크기: ${testEmbedding.size}")
                Log.d("SmartObjectAsk", "  최소: ${testEmbedding.minOrNull()}")
                Log.d("SmartObjectAsk", "  최대: ${testEmbedding.maxOrNull()}")
                Log.d("SmartObjectAsk", "  평균: ${testEmbedding.average()}")
                Log.d(
                    "SmartObjectAsk",
                    "  0 개수: ${testEmbedding.count { it == 0f }}/${testEmbedding.size}"
                )
            } else {
                Log.e("SmartObjectAsk", "❌ 임베딩이 null입니다!")
            }

            // ✅ 유사 이미지 검색 (MobileNetV4 1000D 코사인)
            val similar: List<Pair<String, Float>> = try {
                val result = mobileCLIP.searchSimilarImages(img, topK = 5)

                Log.d("SmartObjectAsk", "✅ 이미지 검색 성공 - 결과 개수: ${result.size}")
                val queryEmbedding = mobileCLIP.getImageEmbedding(img)
                Log.d("SmartObjectAsk", "🔢 쿼리 임베딩 처음 5개: ${queryEmbedding.take(5)}")
                Log.d(
                    "SmartObjectAsk",
                    "🔢 임베딩 norm: ${kotlin.math.sqrt(queryEmbedding.map { it * it }.sum())}"
                )

                val maxScore = result.firstOrNull()?.second ?: Float.NEGATIVE_INFINITY
                if (maxScore <= SIMILARITY_THRESHOLD) {
                    Log.w(
                        "SmartObjectAsk",
                        "⚠️ 최대 유사도 $maxScore ≤ $SIMILARITY_THRESHOLD → 결과 무시(빈 리스트)"
                    )
                    emptyList()
                } else {
                    result
                }
            } catch (e: Exception) {
                Log.e("SmartObjectAsk", "❌ 이미지 검색 실패: ${e.message}", e)
                emptyList()
            }

            if (similar.isEmpty()) {
                Log.w("SmartObjectAsk", "⚠️ 이미지 검색 결과가 비어있습니다!")
            } else {
                Log.d("SmartObjectAsk", "🔍 Top ${similar.size}개 결과:")
                similar.forEachIndexed { i, (id, score) ->
                    Log.d("SmartObjectAsk", "  [$i] $id (score: $score)")
                }
            }

            // ⬇️ 최상위 결과 경로(id) 추출 (index.txt에 저장된 키: 확장자 제거된 경로)
            val topImagePath = similar.firstOrNull()?.first
            Log.d("SmartObjectAsk", "📤 PromptManager에 전달: $topImagePath")
            val normPath = topImagePath?.replace('\\', '/')?.replace(Regex("/+"), "/")
            Log.d("SmartObjectAsk", "📤 정규화 경로: $normPath")

            // 예) "DATA/vangogh/starry_night"
            val parts = normPath?.split('/')?.filter { it.isNotBlank() }

            // ✅ topImagePath에서 작가/작품 추출 (방어 포함)
            val artistName = parts?.getOrNull(1)?.also {
                Log.d("SmartObjectAsk", "🎨 작가 감지: $it")
            } ?: run {
                Log.w("SmartObjectAsk", "⚠️ 작가 추출 실패 (parts=$parts)")
                "작가 모름"
            }

            val topImageName = parts?.getOrNull(2)?.also {
                Log.d("SmartObjectAsk", "🖼️ 작품명 감지: $it")
            } ?: run {
                // fallback: 파일명만 남은 경우나 단일 토큰인 경우
                val last = parts?.lastOrNull()?.substringAfterLast('/')?.substringAfterLast('\\') ?: "unknown"
                val name = last.substringBeforeLast('.') // 혹시 확장자 남아있으면 제거
                Log.w("SmartObjectAsk", "⚠️ 작품명 추출 실패 → fallback=$name")
                name.ifBlank { "unknown" }
            }

            // ✅ 텍스트 RAG 컨텍스트 생성 (작가 + 작품명으로 1개 문서)
            val fCtx: String = try {
                val query = listOf(artistName, topImageName)
                    .filter { !it.isNullOrBlank() }
                    .joinToString(" ")
                    .trim()
                val values = rag.buildContext(query, k = 1)
                Log.d("SmartObjectAsk", "context=${values.take(800)}")
                values
            } catch (e: Exception) {
                Log.e("SmartObjectAsk", "no context: ${e.message}", e)
                ""
            }

            val prompt = PromptManager.build(
                style = selectedPromptStyle,
                question = qq,
                context = fCtx,
//                imageHint = "크롭된 이미지가 작품 일부라면 붓질/색채/구도 관찰 포인트를 1~2문장으로 덧붙이기",
                options = PromptManager.Options(
                    language = "ko",
                    tone = "friendly",
                    cite = true,
                    maxBullets = 5,
                    maxOutputChars = 250
                )
            )
            Log.d("SmartObjectAsk", "Prompt: $prompt")
            askImageViewModel.generateResponse(
                model = selectedModel,
                input = prompt,
                images = listOf(crop),
                onError = { Log.e("ImageRAG", "generateResponse error") }
            )
        }
        // 채팅 + “듣기/중지” 토글 버튼
        Box(modifier = Modifier.fillMaxSize()) {

            ChatViewWrapper(
                viewModel = askImageViewModel,
                modelManagerViewModel = modelManagerViewModel,
                taskId = BuiltInTaskId.LLM_ASK_IMAGE,
                navigateUp = {
                    isTtsSpeaking = false
                    tts.stop()
                    navigateUp()
                },
                modifier = Modifier.matchParentSize(),
            )
            val fabX = 8.dp    // +면 오른쪽/아래로, -면 왼쪽/위로
            val fabY = 56.dp
            val interaction = remember { MutableInteractionSource() }
            GhostFab(
                isTtsSpeaking = isTtsSpeaking,
                onClick = {
                    scope.launch {
                        if (isTtsSpeaking || tts.isSpeaking()) {
                            isTtsSpeaking = false
                            tts.stop()
                            return@launch
                        }
                        val selectedModel = modelManagerUiState.selectedModel
                        val fromVm =
                            selectedModel?.let { askImageViewModel.peekLastAssistantText(it) }
                                ?.trim()
                        val speakText = when {
                            !fromVm.isNullOrBlank() -> fromVm
                            lastAnswerText.isNotBlank() -> lastAnswerText
                            else -> sequenceOf<Any?>(chatVm, askImageViewModel, viewModel)
                                .map { vm ->
                                    extractLastAssistantTextViaReflection(vm).orEmpty().trim()
                                }
                                .firstOrNull { it.isNotEmpty() }
                        }
                        if (speakText.isNullOrBlank()) {
                            Toast.makeText(context, "아직 읽을 답변이 없습니다.", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        isTtsSpeaking = true
                        tts.speak(speakText)
                        launch {
                            while (tts.isSpeaking()) delay(200)
                            isTtsSpeaking = false
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = fabX, y = fabY)
            )
            return   // ← 채팅 뷰 그린 뒤 조기 반환
        }
    }

    // 음성 녹음 다이얼로그
    if (showVoiceRecorder) {
        VoiceRecorderDialog(
            isRecording = isRecording,
            onDismiss = {
                showVoiceRecorder = false
//                croppedImageForChat = null
                isRecording = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Muse:Eye") },
                navigationIcon = {
                    IconButton(onClick = {
                        isTtsSpeaking = false
                        tts.stop()
                        navigateUp()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = { showImageSourceDialog = true }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "이미지 선택")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ✅ 프롬프트 선택 UI
            PromptStyleSelector(
                selected = selectedPromptStyle,
                onSelected = { selectedPromptStyle = it }
            )
            Spacer(Modifier.height(8.dp))

            // 본문
            if (uiState.originalImage == null) {
                EmptyStateContent(onSelectImage = { openImageSourceDialog() })
            } else {
                ImageAnalysisContent(
                    uiState = uiState,
                    onSetCustomSelection = { rect -> viewModel.setCustomSelection(rect) },
                    onClearSelection = { viewModel.clearSelection() },
                    onAskQuestion = {
                        croppedImageForChat = uiState.croppedImage
                        showInputMethodDialog = true
                    },
                    onSelectNewImage = { openImageSourceDialog() }
                )
            }
        }
    }
}

// 🆕 이미지 소스 선택 다이얼로그
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageSourceSelectionDialog(
    onDismiss: () -> Unit,
    onCameraSelected: () -> Unit,
    onGallerySelected: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "이미지 가져오기",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 카메라 버튼
                OutlinedCard(
                    onClick = onCameraSelected,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "📷 카메라로 촬영하기",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "직접 사진을 찍습니다",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 갤러리 버튼
                OutlinedCard(
                    onClick = onGallerySelected,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "🖼️ 갤러리에서 선택하기",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "저장된 사진을 선택합니다",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

// 입력 방식 선택 다이얼로그
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputMethodSelectionDialog(
    onDismiss: () -> Unit,
    onVoiceSelected: () -> Unit,
    onTextSelected: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "질문 방식을 선택하세요",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedCard(
                    onClick = onVoiceSelected,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "🎤 음성으로 질문하기",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "마이크를 사용해 질문합니다",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedCard(
                    onClick = onTextSelected,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "⌨️ 텍스트로 질문하기",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "키보드로 직접 입력합니다",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

// 텍스트 입력 다이얼로그
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "질문을 입력하세요",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("예: 무슨 그림이에요?") },
                minLines = 3,
                maxLines = 5
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text)
                    }
                },
//                enabled = text.isNotBlank()
            ) {
                Text("질문하기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceRecorderDialog(
    isRecording: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = if (isRecording) "듣고 있습니다..." else "음성 인식 준비 중",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isRecording) {
                    Text(
                        "질문을 말씀해주세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun EmptyStateContent(onSelectImage: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "이미지를 선택하세요",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "이미지에서 원하는 영역을 드래그해서\n선택해보세요.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSelectImage,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("이미지 선택", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ImageAnalysisContent(
    uiState: SmartObjectAskUiState,
    onSetCustomSelection: (RectF) -> Unit,
    onClearSelection: () -> Unit,
    onAskQuestion: () -> Unit,
    onSelectNewImage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 안내 메시지
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✏️", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "이미지를 드래그해서 질문할 영역을 선택하세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 이미지 + 드래그 선택
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            uiState.originalImage?.let { bitmap ->
                DraggableImageSelector(
                    bitmap = bitmap,
                    currentSelection = uiState.detectedObjects.firstOrNull()?.boundingBox,
                    resetCounter = uiState.resetCounter,
                    onSelectionChanged = { rect ->
                        onSetCustomSelection(rect)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 선택 초기화 버튼
        if (uiState.croppedImage != null) {
            OutlinedButton(
                onClick = onClearSelection,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("선택 초기화")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 크롭된 이미지 미리보기
        if (uiState.croppedImage != null) {
            Text(
                "선택된 영역",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Image(
                    bitmap = uiState.croppedImage!!.asImageBitmap(),
                    contentDescription = "크롭된 영역",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAskQuestion,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("이 영역에 대해 질문하기", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onSelectNewImage,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("다른 이미지 선택")
            }
        }

        if (uiState.errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    uiState.errorMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraggableImageSelector(
    bitmap: Bitmap,
    currentSelection: RectF?,
    resetCounter: Int,
    onSelectionChanged: (RectF) -> Unit
) {
    Log.d("DragSelector", "recompose, resetCounter=$resetCounter")

    var dragStart by remember(resetCounter) { mutableStateOf<Offset?>(null) }
    var dragEnd   by remember(resetCounter) { mutableStateOf<Offset?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
    ) {
        val boxWidth = constraints.maxWidth.toFloat()
        val boxHeight = constraints.maxHeight.toFloat()
        val scaleX = boxWidth / bitmap.width
        val scaleY = boxHeight / bitmap.height

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "선택된 이미지",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(resetCounter) {
                    detectDragGestures(
                        onDragStart = { offset: Offset ->
                            dragStart = offset
                            dragEnd = offset
                        },
                        onDrag = { change: PointerInputChange, dragAmount: Offset ->
                            change.consume()
                            dragEnd = dragEnd?.plus(dragAmount)
                        },
                        onDragEnd = {
                            val start = dragStart
                            val end = dragEnd
                            dragStart = null
                            dragEnd = null

                            if (start == null || end == null) return@detectDragGestures

                            val left   = min(start.x, end.x) / scaleX
                            val top    = min(start.y, end.y) / scaleY
                            val right  = max(start.x, end.x) / scaleX
                            val bottom = max(start.y, end.y) / scaleY

                            if ((right - left) > 10f && (bottom - top) > 10f) {
                                onSelectionChanged(RectF(left, top, right, bottom))
                            } else {
                                Log.w("DragSelector", "ignored tiny selection")
                            }
                        },
                        onDragCancel = {
                            dragStart = null
                            dragEnd = null
                        }
                    )
                }

        ) {
            currentSelection?.let { rectF ->
                val selRect: ComposeRect = ComposeRect(
                    left = rectF.left * scaleX,
                    top  = rectF.top  * scaleY,
                    right = rectF.right * scaleX,
                    bottom = rectF.bottom * scaleY
                )

                drawRect(
                    color = Color.Blue.copy(alpha = 0.3f),
                    topLeft = Offset(selRect.left, selRect.top),
                    size = Size(selRect.width, selRect.height)
                )
                drawRect(
                    color = Color.Blue,
                    topLeft = Offset(selRect.left, selRect.top),
                    size = Size(selRect.width, selRect.height),
                    style = Stroke(width = 4f)
                )
            }

            val start = dragStart
            val end = dragEnd
            if (start != null && end != null) {
                val left = min(start.x, end.x)
                val top = min(start.y, end.y)
                val width = abs(end.x - start.x)
                val height = abs(end.y - start.y)

                drawRect(
                    color = Color.Green.copy(alpha = 0.3f),
                    topLeft = Offset(left, top),
                    size = Size(width, height)
                )
                drawRect(
                    color = Color.Green,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptStyleSelector(
    selected: PromptStyle,
    onSelected: (PromptStyle) -> Unit
) {
    val options = listOf(
        PromptStyle.AD_EXPRESS,
        PromptStyle.AD_EMOTION,
        PromptStyle.AD_CONTEXT,
        PromptStyle.J_STORY,
        PromptStyle.J_EMOTION,
        PromptStyle.J_QUIZ,
        PromptStyle.DOCENT_VANGOGH_KO,
    )

    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                "프롬프트 스타일",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selected.koreanLabel(),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    label = { Text("선택") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    singleLine = true
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt.koreanLabel()) },
                            onClick = {
                                onSelected(opt)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
// 듣기 버튼
@Composable
fun GhostFab(
    isTtsSpeaking: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(56.dp)                         // 최소 터치 영역(48dp 이상 권장)
            .clip(CircleShape)                   // 모양만 둥글게, 배경은 안 그림
            .background(Color.Transparent)       // 완전 투명
            .clickable(                          // ✅ ripple/오버레이 모두 없음
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isTtsSpeaking) Icons.Filled.Close else Icons.Filled.VolumeUp,
            contentDescription = null,
            tint = Color.White
        )
    }
}
@Composable
private fun AssistantAnswerTapMany(
    vms: List<Any?>,
    active: Boolean,
    onText: (String) -> Unit
) {
    val vmRefs by rememberUpdatedState(vms)
    val onTextRef by rememberUpdatedState(onText)

    LaunchedEffect(active, vmRefs) {
        if (!active) return@LaunchedEffect
        var last = ""
        while (true) {
            var cur = ""
            for (vm in vmRefs) {
                cur = extractLastAssistantTextViaReflection(vm).orEmpty().trim()
                if (cur.isNotEmpty()) break
            }
            if (cur.isNotEmpty() && cur != last) {
                last = cur
                onTextRef(cur)
            }
            delay(250)
        }
    }
}

// 한국어 라벨 매퍼
private fun PromptStyle.koreanLabel(): String = when (this) {
    PromptStyle.AD_EXPRESS         -> "성인 기본형 설명중심"
    PromptStyle.AD_EMOTION         -> "성인 감상 유도형"
    PromptStyle.AD_CONTEXT         -> "성인 비교 맥락형"
    PromptStyle.J_STORY            -> "어린이 이야기형"
    PromptStyle.J_EMOTION          -> "어린이 감정공감형"
    PromptStyle.J_QUIZ             -> "어린이 퀴즈형"
    PromptStyle.DOCENT_VANGOGH_KO  -> "전문가"
}
