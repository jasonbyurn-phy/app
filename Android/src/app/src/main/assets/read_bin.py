import numpy as np

path = "./embeddings/all_embeddings.bin"

# 전체를 float32로 읽기
x = np.fromfile(path, dtype=np.float32)
print("Total floats:", len(x))
print("First 10:", x[:10])
print("Any NaN?", np.isnan(x).any())
print("Mean:", np.nanmean(x))
