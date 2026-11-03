AZ Photo Editor 2.0 adds genuine offline RealESRGAN x4plus photo enhancement with NCNN, Vulkan acceleration and CPU fallback. The APK bundles the photo model; no download or account is needed on the phone.

- 4K AI button processes a snapshot of all current edits without changing the editor history.
- 64×64 input tiles with 32-pixel overlapping context; tile cores are stitched after inference.
- Progress, cancellation after the current tile, before/after preview and Gallery export.
- Output long edge is 3,840 pixels; aspect ratio is preserved to input-grid rounding (up to four output pixels). Input is normalized to a 960-pixel long edge before x4 inference. Very small photos cannot recover real missing detail.
- Model checksums are checked at build time, inside the APK and at runtime. Errors never substitute ordinary resizing for inference.
- Existing adjustments, filters, crop, rotation, undo/redo and normal export remain available.
- Application ID remains com.photoeditor. No previous signing secrets or cached signing key were available. The signing continuity check below states whether reinstalling is required.

Validation: all three ABIs (arm64-v8a, armeabi-v7a, x86_64) contain the NCNN-linked JNI library; both model files match pinned SHA-256 hashes. Android emulator test runs the bundled photo model on CPU, checks output differs from ordinary resizing, and checks missing-model failure.

Not tested on a physical Nubia V70 Max: Vulkan driver compatibility, processing duration, peak memory, thermal behavior, Gallery app visibility and interactive cancellation. CPU enhancement may take several minutes; keep the app open. GPU driver process crashes cannot be caught by the fallback.

Model: https://github.com/xinntao/Real-ESRGAN (BSD-3-Clause). Runtime: https://github.com/Tencent/ncnn (BSD-3-Clause). License notices are bundled in assets/licenses.
