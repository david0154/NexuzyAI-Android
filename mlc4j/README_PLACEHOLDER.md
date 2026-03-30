# mlc4j — MLC-LLM Android Library Module

This directory is a **placeholder**. You must populate it by following these steps:

## Quick Setup

```bash
# 1. Clone the official MLC-LLM repo (do this OUTSIDE NexuzyAI-Android folder)
git clone --recursive https://github.com/mlc-ai/mlc-llm.git

# 2. Copy mlc4j module into this project
cp -r mlc-llm/android/mlc4j  NexuzyAI-Android/mlc4j

# 3. Download prebuilt native .so libraries (arm64-v8a)
cd NexuzyAI-Android/mlc4j
python3 prepare_libs.py

# 4. In NexuzyAI-Android/settings.gradle:
# Uncomment:  include ':mlc4j'

# 5. In NexuzyAI-Android/app/build.gradle:
# Uncomment:  implementation project(':mlc4j')

# 6. In MLCEngineWrapper.kt:
# Set:        MLC_AVAILABLE = true
# Uncomment the 3 import lines at the top
```

## What prepare_libs.py does

It downloads prebuilt `.so` shared libraries from MLC-LLM's CI:
- `libtvm_runtime.so`
- `libmlc_llm.so`  
- `libmlc_llm_jni.so`

These contain the TVM runtime + compiled Vulkan/OpenCL GPU kernels for ARM Android.

## Model Download

After first launch, the app downloads Qwen2-1.5B-Instruct weights from HuggingFace automatically (~1.5GB).
Or pre-download manually:
```bash
git lfs install
git clone https://huggingface.co/mlc-ai/Qwen2-1.5B-Instruct-q4f16_1-MLC
# Push to device:
adb push Qwen2-1.5B-Instruct-q4f16_1-MLC /sdcard/Android/data/ai.nexuzy.assistant/files/
```

## Alternative: Build from Source

If `prepare_libs.py` fails, build from source:
```bash
# Requires: NDK r26+, CMake 3.24+, Python 3.10+
pip install mlc-llm
cd mlc-llm
python3 cmake/gen_cmake_config.py
cd build && cmake .. -DCMAKE_BUILD_TYPE=Release -DANDROID_ABI=arm64-v8a
make -j$(nproc)
```

See full docs: https://llm.mlc.ai/docs/deploy/android.html
