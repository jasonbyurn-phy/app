package com.google.ai.edge.gallery.ui.splash

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * 요구사항:
 * - 흰 화면 표시 → 0.5s 대기 → 좌상→우하 붓터치(수직 진폭 진동 6회, 총 0.5s)로 누적 리빌
 * - 붓터치 종료 후 0.5s 대기 → 남은 흰색만 0.5s 동안 페이드아웃
 * - 이미 드러난 영역은 BlendMode.Clear 누적으로 계속 투명(배경 보임)
 */
@Composable
fun BrushStrokeRevealOverlay(
    modifier: Modifier = Modifier,
    // 타이밍 파라미터 (요구값으로 기본 설정)
    preDelayMs: Int = 500,            // 시작 전 대기
    sweepDurationMs: Int = 500,       // 붓질(전체) 시간
    oscillations: Int = 6,            // 붓질 중 진동 횟수
    postDelayBeforeFadeMs: Int = 0, // 붓질 후 페이드 시작 전 대기
    fadeOutMs: Int = 500,             // 남은 흰색 페이드 시간
    onFinished: (() -> Unit)? = null
) {
    // 진행 0..1
    val progress = remember { Animatable(0f) }
    // 오버레이 알파(1→0 페이드)
    val overlayAlpha = remember { Animatable(1f) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 1) 흰 화면 유지 대기
        delay(preDelayMs.toLong())

        // 2) 붓질 진행 (0 → 1)
        progress.animateTo(
            1f,
            animationSpec = tween(durationMillis = sweepDurationMs, easing = FastOutSlowInEasing)
        )

        // 3) 붓질 종료 후 대기
        delay(postDelayBeforeFadeMs.toLong())

        // 4) 남은 흰색 페이드아웃
        overlayAlpha.animateTo(
            0f,
            animationSpec = tween(durationMillis = fadeOutMs, easing = FastOutSlowInEasing)
        )

        finished = true
        onFinished?.invoke()
    }

    if (finished) return

    // ===== 공통 오버레이 수정자: 최상단 + 현재 알파 =====
    val overlayModifier = modifier
        .fillMaxSize()
        .zIndex(10f)
        .graphicsLayer(alpha = overlayAlpha.value)

    // ===== AGSL(33+) / 폴백 분기 =====
    if (Build.VERSION.SDK_INT >= 33) {
        // ── AGSL 기반 붓질(질감/가장자리 거칠기 포함) ──
        val shader = remember {
            android.graphics.RuntimeShader(
                """
        uniform float2 iResolution;   // px
        uniform float  progress;      // 0..1
        uniform float  bandWidth;     // 진행축 밴드 절반폭(상대)
        uniform float  strokeHalf;    // 수직축 붓 두께 절반폭(상대)
        uniform float  wobbleAmp;     // 좌↔우 지그재그 진폭(상대)
        uniform float  feather;       // 경계 부드러움(상대)
        uniform float  bristleAmp;    // 붓결 거칠기
        uniform float  noiseScale;    // 붓결 빈도
        uniform float  cycles;        // 지그재그 횟수

        float hash(float2 p){ return fract(sin(dot(p,float2(127.1,311.7)))*43758.5453); }
        float noise(float2 p){
          float2 i=floor(p); float2 f=fract(p);
          float a=hash(i);
          float b=hash(i+float2(1.0,0.0));
          float c=hash(i+float2(0.0,1.0));
          float d=hash(i+float2(1.0,1.0));
          float2 u=f*f*(3.0-2.0*f);
          return mix(a,b,u.x)+ (c-a)*u.y*(1.0-u.x) + (d-b)*u.x*u.y;
        }
        float fbm(float2 p){
          float v=0.0, a=0.5;
          for(int i=0;i<4;i++){ v+=a*noise(p); p*=2.0; a*=0.5; }
          return v;
        }

        half4 main(float2 frag){
          float2 res = iResolution;
          float2 uv = frag / res;                 // 0..1
          float u = (uv.x + uv.y) * 0.5;          // 진행축(TL->BR)
          float v = (uv.x - uv.y);                // 수직축

          // 붓결
          float n = fbm(uv * noiseScale);
          float rough = (n - 0.5) * bristleAmp;

          // 지그재그(좌↔우): 진행에 따라 흔들림(진동 cycles회)
          float serp = wobbleAmp * sin(6.2831853 * cycles * u);

          // 진행 근처 얇은 시간 밴드만
          float band = 1.0 - smoothstep(progress - bandWidth, progress + bandWidth, u);

          // 수직축 붓 두께 + 거친 엣지
          float dist = abs(v - serp);
          float edge = smoothstep(strokeHalf + rough, strokeHalf + rough + feather, dist);
          float clear = band * (1.0 - edge);      // 1=투명(배경 보임), 0=흰색

          float alpha = 1.0 - clear;
          return half4(1.0, 1.0, 1.0, clamp(alpha, 0.0, 1.0));
        }
        """.trimIndent()
            )
        }

        // 감성 튜닝(필요시만 조정)
        val bandWidth   = 0.030f   // 진행축 밴드 폭(작을수록 “지나가는 부분”만 보임)
        val strokeHalf  = 0.5f   // 붓 두께 절반폭(상대)
        val wobbleAmp   = 0.2f    // 좌↔우 진동 진폭(상대)
        val feather     = 0.7f   // 경계 부드러움
        val bristleAmp  = 0.70f    // 붓결 거칠기
        val noiseScale  = 5.0f     // 붓결 빈도
        val cyclesF     = oscillations.toFloat()

        androidx.compose.foundation.layout.Box(
            overlayModifier.drawWithCache {
                shader.setFloatUniform("iResolution", size.width, size.height)
                shader.setFloatUniform("progress", progress.value)
                shader.setFloatUniform("bandWidth", bandWidth)
                shader.setFloatUniform("strokeHalf", strokeHalf)
                shader.setFloatUniform("wobbleAmp", wobbleAmp)
                shader.setFloatUniform("feather", feather)
                shader.setFloatUniform("bristleAmp", bristleAmp)
                shader.setFloatUniform("noiseScale", noiseScale)
                shader.setFloatUniform("cycles", cyclesF)
                val brush = ShaderBrush(shader)
                onDrawWithContent { drawRect(brush) }   // 흰 오버레이 + 마스크
            }
        )
    } else {
        // ── 폴백: 얇은 띠 여러 개를 Clear로 긁어 '간이 붓질' ──
        val bandWidth   = 0.030f
        val strokeHalf  = 0.090f
        val wobbleAmp   = 0.22f
        val featherPx   = 18f
        val stripes     = 9

        androidx.compose.foundation.layout.Box(
            overlayModifier.drawWithCache {
                val w = size.width
                val h = size.height
                val len = hypot(w, h)
                val pnx = -h / len   // 대각선 수직 단위벡터
                val pny =  w / len

                onDrawWithContent {
                    // 전체 흰색으로 덮고
                    drawRect(Color.White)

                    // 진행 근처 얇은 "시간 밴드"만 처리
                    val uMin = (progress.value - bandWidth).coerceIn(0f, 1f)
                    val uMax = (progress.value + bandWidth).coerceIn(0f, 1f)

                    // 밴드 중심 지그재그(좌↔우) — 총 진동 수 반영
                    val serp = { u: Float ->
                        wobbleAmp * sin((2f * PI.toFloat() * oscillations.toFloat() * u))
                    }

                    // 대각선 좌표 0..1 → 화면 좌표
                    fun pointOnDiag(u: Float) = Offset(w * u, h * u)

                    // 여러 얇은 띠를 대각선에 수직으로 겹쳐 파서(BlendMode.Clear) 거친 붓결 느낌
                    val steps = 28
                    for (i in 0..steps) {
                        val u = uMin + (uMax - uMin) * (i / steps.toFloat())
                        val c = pointOnDiag(u)
                        val s = serp(u)

                        // 수직 방향으로 지그재그 오프셋
                        val cx = c.x + pnx * s * w * 0.5f
                        val cy = c.y + pny * s * h * 0.5f

                        // 중심 주변으로 얇은 띠들을 조금씩 어긋나게 배치
                        for (k in -stripes..stripes) {
                            val jitter = (k * 1.7f) + (sin(u * 1000f + k * 13f) * 1.3f)
                            val offX = cx + pnx * jitter * 2f
                            val offY = cy + pny * jitter * 2f

                            // 회전 변환
                            withTransform({
                                rotate(degrees = 45f, pivot = Offset(offX, offY))
                            }) {
                                drawRect(
                                    color = Color.Transparent,
                                    topLeft = Offset(-w, offY - strokeHalf * h / 2f),
                                    size = Size(w * 2f, strokeHalf * h + featherPx),
                                    blendMode = BlendMode.Clear
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
