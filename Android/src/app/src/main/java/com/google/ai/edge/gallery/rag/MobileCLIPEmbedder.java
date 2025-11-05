package com.google.ai.edge.gallery.rag;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ai.edge.gallery.MobileCLIPHelper;

/**
 * MobileCLIP 모델을 Mediapipe 인터페이스에 맞춰 래핑하는 어댑터.
 * MediapipeImageEmbedder는 폴백용으로만 사용.
 */
public final class MobileCLIPEmbedder implements ImageEmbedderApi, AutoCloseable {

    private final MobileCLIPHelper mobileCLIP;
    private final MediapipeImageEmbedder fallbackEmbedder;

    /** MobileCLIP + Fallback 생성자 */
    public MobileCLIPEmbedder(@NonNull Context context, @Nullable String modelAssetPath) {
        this.mobileCLIP = new MobileCLIPHelper(context);

        // ⚠️ Mediapipe fallback은 거의 쓰이지 않지만 null일 경우 대비
        String fallbackModel = (modelAssetPath == null || modelAssetPath.isEmpty())
                ? "mobileclip_s2_datacompdr_last.tflite"
                : modelAssetPath;

        // MediapipeImageEmbedder는 mobileclip 모델을 직접 못 쓰지만, 타입 호환을 위해 유지
        this.fallbackEmbedder = new MediapipeImageEmbedder(context, fallbackModel);
    }

    /** SmartObjectAskScreen 호환용: context만 받는 오버로드 */
    public MobileCLIPEmbedder(@NonNull Context context) {
        this(context, "mobileclip_s2_datacompdr_last.tflite");
    }

    @Override
    @NonNull
    public float[] embed(@NonNull Bitmap bitmap) {
        try {
            // MobileCLIP 직접 호출
            float[] clipEmbedding = mobileCLIP.getImageEmbedding(bitmap);
            if (clipEmbedding != null && clipEmbedding.length > 0) {
                return clipEmbedding;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 실패 시 mediapipe fallback (mobilenet)
        return fallbackEmbedder.embed(bitmap);
    }

    @Override
    public void close() {
        try { mobileCLIP.close(); } catch (Throwable ignored) {}
        try { fallbackEmbedder.close(); } catch (Throwable ignored) {}
    }
}
