package com.google.ai.edge.gallery.ui.common.chat

import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ModelDownloadStatusType
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.common.ModelPageAppBar
import com.google.ai.edge.gallery.ui.modelmanager.ModelInitializationStatusType
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "AGChatView"

@Composable
fun ChatView(
  task: Task,
  viewModel: ChatViewModel,
  modelManagerViewModel: ModelManagerViewModel,
  onSendMessage: (Model, List<ChatMessage>) -> Unit,
  onRunAgainClicked: (Model, ChatMessage) -> Unit,
  onBenchmarkClicked: (Model, ChatMessage, Int, Int) -> Unit,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  onResetSessionClicked: (Model) -> Unit = {},
  onStreamImageMessage: (Model, ChatMessageImage) -> Unit = { _, _ -> },
  onStopButtonClicked: (Model) -> Unit = {},
  showStopButtonInInputWhenInProgress: Boolean = false,
) {
  val uiState by viewModel.uiState.collectAsState()
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
  val selectedModel = modelManagerUiState.selectedModel

  var selectedImageIndex by remember { mutableIntStateOf(-1) }
  var allImageViewerImages by remember { mutableStateOf<List<Bitmap>>(listOf()) }
  var showImageViewer by remember { mutableStateOf(false) }

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var navigatingUp by remember { mutableStateOf(false) }

  val handleNavigateUp = {
    navigatingUp = true
    navigateUp()
    scope.launch(Dispatchers.Default) {
      for (model in task.models) {
        modelManagerViewModel.cleanupModel(context = context, task = task, model = model)
      }
    }
  }

  val curDownloadStatus = modelManagerUiState.modelDownloadStatus[selectedModel.name]
  LaunchedEffect(curDownloadStatus, selectedModel.name) {
    if (!navigatingUp) {
      if (curDownloadStatus?.status == ModelDownloadStatusType.SUCCEEDED) {
        Log.d(TAG, "Initializing model '${selectedModel.name}' from ChatView launched effect")
        modelManagerViewModel.initializeModel(context, task = task, model = selectedModel)
      }
    }
  }

  BackHandler {
    val modelInitializationStatus =
      modelManagerUiState.modelInitializationStatus[selectedModel.name]
    val isModelInitializing =
      modelInitializationStatus?.status == ModelInitializationStatusType.INITIALIZING
    if (!isModelInitializing && !uiState.inProgress) {
      handleNavigateUp()
    }
  }

  // ── 변경 1) 배경 이미지를 Scaffold 뒤에 먼저 깔기 + Scaffold 투명 ──
  Scaffold(
    modifier = modifier,
    topBar = {
      // ── 변경 2) 상단 앱바의 배경을 투명으로 (ModelPageAppBar가 지원할 경우) ──
      ModelPageAppBar(
        task = task,
        model = selectedModel,
        modelManagerViewModel = modelManagerViewModel,
        canShowResetSessionButton = true,
        isResettingSession = uiState.isResettingSession,
        inProgress = uiState.inProgress,
        modelPreparing = uiState.preparing,
        onResetSessionClicked = onResetSessionClicked,
        onConfigChanged = { old, new ->
          viewModel.addConfigChangedMessage(
            oldConfigValues = old,
            newConfigValues = new,
            model = selectedModel,
          )
        },
        onBackClicked = { handleNavigateUp() },
        onModelSelected = { prevModel, curModel ->
          if (prevModel.name != curModel.name) {
            modelManagerViewModel.cleanupModel(context = context, task = task, model = prevModel)
          }
          modelManagerViewModel.selectModel(model = curModel)
        },
        // ↓↓↓ 만약 ModelPageAppBar에 이 파라미터가 없다면, 그 파일에서 containerColor 추가 후 전달하세요.
        containerColor = Color.Transparent
      )
    },
    containerColor = Color.Transparent, // ★ Scaffold 배경 투명
  ) { innerPadding ->
    Box {
      // ★ 맨 뒤: 배경 이미지
      Image(
        painter = painterResource(R.drawable.chat_bg),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
      )

      // (선택) 하단 가독성 스크림
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              0.0f to Color.Transparent,
              0.70f to Color.Transparent,
              1.0f to Color(0x66000000)
            )
          )
      )

      val curModelDownloadStatus = modelManagerUiState.modelDownloadStatus[selectedModel.name]

      // ★ 기존 화면색 칠하던 부분 제거 → 투명
      Column(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        AnimatedContent(
          targetState = curModelDownloadStatus?.status == ModelDownloadStatusType.SUCCEEDED
        ) { targetState ->
          when (targetState) {
            true ->
              ChatPanel(
                modelManagerViewModel = modelManagerViewModel,
                task = task,
                selectedModel = selectedModel,
                viewModel = viewModel,
                innerPadding = innerPadding,
                navigateUp = navigateUp,
                onSendMessage = onSendMessage,
                onRunAgainClicked = onRunAgainClicked,
                onBenchmarkClicked = onBenchmarkClicked,
                onStreamImageMessage = onStreamImageMessage,
                onStreamEnd = { averageFps ->
                  viewModel.addMessage(
                    model = selectedModel,
                    message = ChatMessageInfo(
                      content = "Live camera session ended. Average FPS: $averageFps"
                    ),
                  )
                },
                onStopButtonClicked = { onStopButtonClicked(selectedModel) },
                onImageSelected = { bitmaps, selectedBitmapIndex ->
                  selectedImageIndex = selectedBitmapIndex
                  allImageViewerImages = bitmaps
                  showImageViewer = true
                },
                modifier = Modifier.weight(1f),
                showStopButtonInInputWhenInProgress = showStopButtonInInputWhenInProgress,
              )
            false ->
              ModelDownloadStatusInfoPanel(
                model = selectedModel,
                task = task,
                modelManagerViewModel = modelManagerViewModel,
              )
          }
        }
      }

      // 이미지 뷰어
      AnimatedVisibility(
        visible = showImageViewer,
        enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }) + fadeOut(),
      ) {
        val pagerState =
          rememberPagerState(
            pageCount = { allImageViewerImages.size },
            initialPage = selectedImageIndex,
          )
        val scrollEnabled = remember { mutableStateOf(true) }
        Box(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
          HorizontalPager(
            state = pagerState,
            userScrollEnabled = scrollEnabled.value,
            modifier = Modifier
              .fillMaxSize()
              .background(Color.Black.copy(alpha = 0.95f)),
          ) { page ->
            allImageViewerImages[page].let { image ->
              ZoomableImage(bitmap = image.asImageBitmap(), pagerState = pagerState)
            }
          }

          IconButton(
            onClick = { showImageViewer = false },
            colors = IconButtonDefaults.iconButtonColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.offset(x = (-8).dp, y = 8.dp).align(Alignment.TopEnd),
          ) {
            Icon(
              Icons.Rounded.Close,
              contentDescription = stringResource(R.string.cd_close_image_viewer_icon),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}
