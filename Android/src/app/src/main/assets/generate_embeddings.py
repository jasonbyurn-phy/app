"""
✅ MobileCLIP 임베딩 사전 계산 (디버깅 강화)
"""

import json
import struct
from pathlib import Path
import numpy as np
from PIL import Image
import tensorflow as tf
import os

# ===== 설정 =====
MODEL_PATH = "mobileclip_s2_datacompdr_last.tflite"
BASE_DIR = Path(".")
OUTPUT_FILE = BASE_DIR / "embeddings" / "all_embeddings.bin"
IMAGE_SIZE = 256
EMBEDDING_DIM = 512

ARTIST_FILES = [
    "DATA/davinchi.json",
    "DATA/klimt.json",
    "DATA/vangogh.json",
]

# ===== 1. 모델 로드 =====
print("🔧 TFLite 모델 로드 중...")
interpreter = tf.lite.Interpreter(model_path=MODEL_PATH)
interpreter.allocate_tensors()

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

print(f"📊 모델 정보:")
print(f"   입력 개수: {len(input_details)}")
for i, inp in enumerate(input_details):
    print(f"   입력[{i}]: shape={inp['shape']}, dtype={inp['dtype']}")

print(f"   출력 개수: {len(output_details)}")
for i, out in enumerate(output_details):
    print(f"   출력[{i}]: shape={out['shape']}, dtype={out['dtype']}, name={out['name']}")


# ===== 2. 전처리 =====
def preprocess_image(image_path):
    img = Image.open(str(image_path)).convert('RGB')
    img = img.resize((IMAGE_SIZE, IMAGE_SIZE))
    arr = np.array(img, dtype=np.float32) / 255.0

    # ✅ MobileCLIP / OpenCLIP 표준 mean, std
    mean = np.array([0.48145466, 0.4578275, 0.40821073], dtype=np.float32)
    std  = np.array([0.26862954, 0.26130258, 0.27577711], dtype=np.float32)

    arr = (arr - mean) / std
    arr = np.expand_dims(arr, axis=0)
    arr = np.transpose(arr, (0, 3, 1, 2))  # (1, 3, 256, 256)
    return arr


# ===== 3. 임베딩 추출 =====
def get_embedding(image_path):
    try:
        arr = preprocess_image(image_path)
        
        # ⭐ 이미지 입력 (input[0])
        interpreter.set_tensor(input_details[0]['index'], arr)
        
        # ⭐ 텍스트 더미 입력 (input[1], 있다면)
        if len(input_details) > 1:
            dummy_text = np.zeros((1, 77), dtype=np.int64)
            interpreter.set_tensor(input_details[1]['index'], dummy_text)
        
        interpreter.invoke()
        
        # ⭐ 이미지 임베딩 추출
        emb = interpreter.get_tensor(output_details[IMAGE_OUTPUT_INDEX]['index']).squeeze()
        
        # 정규화
        norm = np.linalg.norm(emb)
        if norm > 0:
            emb = emb / norm
            
        return emb
    except Exception as e:
        print(f"   ❌ 실패: {image_path} - {e}")
        return None

# ===== 4. 모든 작품 처리 =====
embeddings_list = []  # ⭐ 순서 보장을 위해 리스트 사용
image_paths = []
total = success = 0

print("\n📸 이미지 임베딩 계산 시작...\n")

for json_file_rel in ARTIST_FILES:
    json_path = BASE_DIR / json_file_rel
    print(f"🔍 JSON 파일: {json_path}")
    
    if not json_path.exists():
        print(f"⚠️ 없음: {json_path}")
        continue

    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    artist = data.get("artist", "Unknown")
    artworks = data.get("artworks", [])
    print(f"📂 {artist} ({len(artworks)}개 작품)")

    for art in artworks:
        art_id = art.get("id", "")
        img_path = art.get("image_path", "")
        
        if not art_id or not img_path:
            continue

        full_path = BASE_DIR / img_path
        total += 1

        if not full_path.exists():
            # 확장자 변경 시도
            for ext in [".jpg", ".png", ".jpeg"]:
                alt = full_path.with_suffix(ext)
                if alt.exists():
                    print(f"   🔄 확장자 변경: {full_path.name} → {alt.name}")
                    full_path = alt
                    break
            else:
                print(f"   ❌ 이미지 없음: {full_path}")
                continue

        print(f"   [{total}] 처리 중: {full_path.name}")
        emb = get_embedding(full_path)
        
        if emb is not None:
            # ⭐ 고유성 체크
            if len(embeddings_list) > 0:
                similarity = np.dot(emb, embeddings_list[-1])
                if similarity > 0.99:
                    print(f"      ⚠️ 이전 임베딩과 너무 유사! (유사도: {similarity:.4f})")
            
            key = str(Path(img_path).with_suffix(''))
            image_paths.append(key)
            embeddings_list.append(emb)
            success += 1
            
            print(f"      ✅ 임베딩 처음 5개: {emb[:5]}")
        else:
            print(f"      ❌ 임베딩 실패")

print(f"\n✅ 총 {success}/{total}개 임베딩 계산 성공")

# ===== 5. 저장 =====
if success > 0:
    print(f"💾 저장 중... ({success}개)")
    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)

    all_vecs = np.array(embeddings_list, dtype=np.float32)
    
    # ⭐ 최종 검증
    print("\n🔍 최종 검증:")
    for i in range(min(3, len(all_vecs))):
        print(f"[{i}] {image_paths[i]}")
        print(f"  처음 5개: {all_vecs[i][:5]}")
        if i > 0:
            sim = np.dot(all_vecs[0], all_vecs[i])
            print(f"  vs [0] 유사도: {sim:.4f}")
    
    with open(OUTPUT_FILE, "wb") as f:
        all_vecs.tofile(f)
        f.flush()
        os.fsync(f.fileno())

    file_size = OUTPUT_FILE.stat().st_size / (1024 * 1024)
    print(f"""
✨ 완료!
📦 {OUTPUT_FILE}
📊 {success}개 × 512 floats
💾 크기: {file_size:.2f} MB
""")
else:
    print("❌ 저장할 임베딩이 없습니다!")