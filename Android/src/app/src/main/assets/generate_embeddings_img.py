import numpy as np
import tensorflow as tf
from pathlib import Path
from PIL import Image
import json, os

# ===== 설정 =====
MODEL_PATH = "mobilenetv4_conv_small_e2400_r224_in1k_float32.tflite"
BASE_DIR = Path("C:/app/app_final/Android/src/app/src/main/assets/")
OUTPUT_FILE = BASE_DIR / "embeddings" / "all_embeddings_img.bin"
INDEX_FILE  = BASE_DIR / "embeddings" / "all_embeddings_img.index.txt"
ARTIST_FILES = ["DATA/vangogh.json"]

# 전처리
NORM_MODE = "minus1to1"        # "minus1to1" | "imagenet"
FALLBACK_IMAGE_SIZE = 224

# 투영 옵션
TARGET_DIM = 1000
PROJECTION = "rp"              # "rp"(빠름) | "pca"(느림; 코퍼스 기반)
RANDOM_SEED = 42

def resolve_input_size_and_layout(interpreter):
    inp = interpreter.get_input_details()[0]
    shape = [int(x) for x in inp["shape"]]
    if len(shape)==4 and shape[1]>1 and shape[2]>1 and shape[3] in (1,3,4):
        return (shape[1], shape[2], shape[3]), True
    if len(shape)==4 and shape[2]>1 and shape[3]>1 and shape[1] in (1,3,4):
        return (shape[2], shape[3], shape[1]), False
    return (FALLBACK_IMAGE_SIZE, FALLBACK_IMAGE_SIZE, 3), True

def pick_output_index(output_details):
    # MobileNetV4 분류 모델은 보통 (1,1000) 로짓이 단일 출력
    # 여러 개면 마지막 차원이 가장 큰 걸 우선
    if len(output_details)==1:
        return 0
    sizes = [(i, int(np.prod(od["shape"]))) for i, od in enumerate(output_details)]
    return max(sizes, key=lambda x: x[1])[0]

def preprocess(img_path, target_hw_c, nhwc=True):
    H,W,C = target_hw_c
    img = Image.open(str(img_path)).convert("RGB").resize((W,H))
    arr = np.asarray(img, dtype=np.float32)/255.0
    if NORM_MODE=="minus1to1":
        arr = arr*2.0 - 1.0
    elif NORM_MODE=="imagenet":
        mean = np.array([0.485,0.456,0.406], np.float32)
        std  = np.array([0.229,0.224,0.225], np.float32)
        arr = (arr-mean)/std
    arr = arr[None, ...]  # (1,H,W,C)
    return arr if nhwc else np.transpose(arr,(0,3,1,2))

def l2_normalize(v: np.ndarray) -> np.ndarray:
    n = float(np.linalg.norm(v))
    return v if n<1e-12 else v/n

# --- Random Projection 생성(결정적) ---
def make_random_projection(in_dim, out_dim, seed=RANDOM_SEED):
    rng = np.random.default_rng(seed)
    W = rng.normal(0.0, 1.0/np.sqrt(in_dim), size=(in_dim, out_dim)).astype(np.float32)
    return W

def main():
    print("🔧 Load TFLite:", MODEL_PATH)
    itp = tf.lite.Interpreter(model_path=str(BASE_DIR / MODEL_PATH), num_threads=4)
    itp.allocate_tensors()
    in_det  = itp.get_input_details()
    out_det = itp.get_output_details()

    target, nhwc = resolve_input_size_and_layout(itp)
    out_idx = pick_output_index(out_det)
    print(f"Input(H,W,C)={target}, NHWC={nhwc}, output_idx={out_idx}, out_shape={out_det[out_idx]['shape']}")

    # 출력 차원 파악
    dummy = np.zeros((1, target[0], target[1], target[2]), dtype=np.float32)
    if not nhwc:
        dummy = np.transpose(dummy, (0,3,1,2))
    itp.set_tensor(in_det[0]['index'], dummy)
    itp.invoke()
    out = itp.get_tensor(out_det[out_idx]['index']).squeeze().astype(np.float32)
    if out.ndim>1: out = out.reshape(-1)
    out_dim = out.shape[0]
    print(f"Detected raw embedding/logit dim = {out_dim}")

    # 투영 행렬(필요시)
    proj_W = None
    if out_dim != TARGET_DIM:
        print(f"⚠️ output dim {out_dim} → TARGET {TARGET_DIM}. Using {PROJECTION} projection.")
        if PROJECTION=="rp":
            proj_W = make_random_projection(out_dim, TARGET_DIM)
        else:
            # PCA는 코퍼스 전체 모은 뒤 한 번에 수행해야 해서,
            # 여기선 RP 기본 사용을 권장. (원하면 추후 PCA 모드 구현)
            proj_W = make_random_projection(out_dim, TARGET_DIM)

    embeddings, keys = [], []
    total=success=0

    for jf_rel in ARTIST_FILES:
        jf = BASE_DIR / jf_rel
        if not jf.exists():
            print("skip (no json):", jf)
            continue
        data = json.load(open(jf, "r", encoding="utf-8"))
        for art in data.get("artworks", []):
            art_id  = art.get("id","")
            img_rel = art.get("image_path","")
            if not art_id or not img_rel: continue
            total += 1
            img_path = BASE_DIR / img_rel
            if not img_path.exists():
                ok = False
                for ext in [".jpg",".jpeg",".png",".webp"]:
                    alt = img_path.with_suffix(ext)
                    if alt.exists(): img_path=alt; ok=True; break
                if not ok:
                    print("❌ missing:", img_path)
                    continue
            try:
                arr = preprocess(img_path, target, nhwc)
                itp.set_tensor(in_det[0]['index'], arr)
                itp.invoke()
                vec = itp.get_tensor(out_det[out_idx]['index']).squeeze().astype(np.float32)
                if vec.ndim>1: vec = vec.reshape(-1)

                # 필요시 512로 투영
                if proj_W is not None:
                    vec = vec @ proj_W    # (out_dim,) @ (out_dim,512) → (512,)

                vec = l2_normalize(vec)
                if vec.shape[0] != TARGET_DIM:
                    print("❌ wrong dim:", vec.shape)
                    continue

                key = str(Path(img_rel).with_suffix(''))
                embeddings.append(vec)
                keys.append(key)
                success += 1
                if success<=3:
                    print("✅", key, "first5:", vec[:5])
            except Exception as e:
                print("❌ fail:", e)

    print(f"\nDone: {success}/{total}")
    if success==0: return

    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    all_vecs = np.vstack(embeddings).astype(np.float32)
    with open(OUTPUT_FILE, "wb") as f:
        all_vecs.tofile(f); f.flush(); os.fsync(f.fileno())
    with open(INDEX_FILE, "w", encoding="utf-8") as f:
        for k in keys: f.write(k+"\n")
    print("Saved:", OUTPUT_FILE, INDEX_FILE, all_vecs.shape)

if __name__ == "__main__":
    main()
