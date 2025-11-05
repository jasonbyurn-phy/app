"""
✅ 임베딩 품질 검증 (경로 구분자 수정)
"""
import numpy as np
from pathlib import Path
import json

# .bin 로드
embeddings = np.fromfile("embeddings/all_embeddings.bin", dtype=np.float32)
embeddings = embeddings.reshape(-1, 512)

print(f"총 {len(embeddings)}개 임베딩 로드")

# ⭐ 77개 전체 이미지 경로 생성
image_paths = []

# JSON 파일에서 순서대로 추출
ARTIST_FILES = [
    "DATA/davinchi.json",
    "DATA/klimt.json",
    "DATA/vangogh.json",
]

BASE_DIR = Path(".")

for json_file_rel in ARTIST_FILES:
    json_path = BASE_DIR / json_file_rel
    
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)
    
    for art in data.get("artworks", []):
        img_path = art.get("image_path", "")
        if img_path:
            # ⭐ 확장자 제거 + 경로를 / 로 통일
            key = str(Path(img_path).with_suffix('')).replace('\\', '/')
            image_paths.append(key)

print(f"총 {len(image_paths)}개 이미지 경로 생성")

# Self_Portrait_with_Felt_Hat 검색
target_path = "DATA/vangogh/29_Self_Portrait_with_Felt_Hat"

if target_path not in image_paths:
    print(f"❌ {target_path}를 image_paths에서 찾을 수 없음!")
    print("사용 가능한 vangogh 작품:")
    for p in image_paths:
        if "vangogh" in p:
            print(f"  - {p}")
    exit()

target_idx = image_paths.index(target_path)
target_emb = embeddings[target_idx]

print(f"\n🎯 Target: {image_paths[target_idx]} (index: {target_idx})")
print(f"  Embedding 처음 5개: {target_emb[:5]}")
print(f"  L2 Norm: {np.linalg.norm(target_emb):.4f}")

# 모든 임베딩과 유사도 계산
similarities = []
for i, emb in enumerate(embeddings):
    sim = np.dot(target_emb, emb)
    similarities.append((i, sim, image_paths[i]))

# Top 20 출력
similarities.sort(key=lambda x: x[1], reverse=True)
print("\n🔍 Top 20 유사 이미지:")
for rank, (idx, sim, path) in enumerate(similarities[:20]):
    marker = "✅" if idx == target_idx else "  "
    print(f"{marker} [{rank}] {path} (score: {sim:.4f})")

# ⭐ 추가: 정답이 몇 위인지 확인
target_rank = next((i for i, (idx, _, _) in enumerate(similarities) if idx == target_idx), None)
if target_rank is not None:
    print(f"\n🎯 정답 순위: {target_rank + 1}위 (Top {target_rank + 1})")
    if target_rank >= 20:
        print(f"⚠️ 정답이 Top 20 밖에 있습니다! (실제 유사도: {similarities[target_rank][1]:.4f})")
else:
    print("\n❌ 정답을 찾을 수 없습니다!")