#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p vendor app/src/main/assets/models
curl --fail --location --retry 3 https://github.com/Tencent/ncnn/releases/download/20240410/ncnn-20240410-android-vulkan.zip -o vendor/ncnn.zip
echo '352f7f6b11e862c72b72de4b8133e0b237b43d030c079aa8ed653f3c8e944580  vendor/ncnn.zip' | sha256sum -c -
unzip -q -o vendor/ncnn.zip -d vendor
rm -rf vendor/ncnn
mv vendor/ncnn-20240410-android-vulkan vendor/ncnn
curl --fail --location --retry 3 https://github.com/xinntao/Real-ESRGAN/releases/download/v0.2.5.0/realesrgan-ncnn-vulkan-20220424-ubuntu.zip -o vendor/model.zip
for ext in param bin; do
  unzip -p vendor/model.zip "models/realesrgan-x4plus.$ext" > "app/src/main/assets/models/realesrgan-x4plus.$ext"
done
(cd app/src/main/assets/models && sha256sum -c ../../../../../scripts/model-sha256.txt)
