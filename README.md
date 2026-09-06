# FaceGrid

FaceGrid is an on-device Android app that samples a selected portrait video, detects clearly visible faces, tracks continuous appearances, groups those appearances into identities, and renders one generous representative crop per person.

## Build and run

1. Open the project in Android Studio with an Android SDK that includes API 37, or run `./gradlew assembleDebug` from the project root.
2. Install the debug APK on an Android 8.0+ device or emulator.
3. Choose a video through the app. Videos remain outside the project and are selected through the Android system picker.

The supplied debug APK is intentionally arm64-only (`arm64-v8a`) to keep the Select TF Ops package practical to distribute. Build without the `abiFilters` block if an x86 emulator or another ABI is required.

The processing pipeline uses ML Kit Face Detection and runs from a coroutine on `Dispatchers.Default`. Gallery writes use `MediaStore`; sharing uses the standard Android share sheet.

## Face embedding model

The bundled model is `app/src/main/assets/ghostfacenet_float16.tflite`, a TFLite conversion of [GhostFaceNet](https://github.com/HamadYA/GhostFaceNets) from [this conversion repository](https://github.com/MujtabaBhatti09/FaceNet-Models-and-Conversions-from-Keras-to-tflite). Its SHA-256 is `1c533b62dd09277c8550e738f12d6a3c778c3aa6888152cb431e569cd3809c22`. The expected runtime contract is input `[1, 112, 112, 3]` `FLOAT32` and output `[1, 512]` `FLOAT32`. The input is five-point ML Kit landmark aligned to the standard 112x112 ArcFace template, then normalized as `(pixel - 127.5) / 128.0`.

GhostFaceNet is an ArcFace-style face-recognition embedder and is materially stronger for cross-appearance identity grouping than the previous 192-dimensional MobileFaceNet asset. The app includes TensorFlow Lite Select TF Ops because this converted model contains Flex operations. The source conversion repository describes the float16 model as MIT-licensed; verify the upstream model-weight terms before distributing the APK beyond this assignment.

If the asset cannot be loaded, FaceGrid uses a deterministic lightweight visual descriptor behind the same `FaceEmbedder` interface. This keeps the end-to-end flow runnable, but it is not a substitute for a face-recognition model and can merge different people.

## Tunable defaults

All tuning values are in `data/processing/ProcessingConfig.kt`. The default sample rate is 5 fps and the identity cosine-similarity threshold is 0.50. Each continuous segment embeds its strongest three quality-gated frames and averages them, then clusters segment embeddings while preventing simultaneous faces from sharing an identity. A conservative singleton-recovery pass compares the strongest candidate crop against each compatible identity and requires a nearest-candidate margin, which handles difficult shared-frame poses without assuming a fixed number of people or appearances. Duplicate boxes are suppressed using IoU/containment, and one-frame segments are discarded as likely whip-pan noise.

## Limitations

- ML Kit face detection and the bundled TFLite model are device-side, but processing time and memory use grow with video duration and face count.
- The tracker intentionally closes an appearance when a qualified face cannot be matched in the next sampled frame.
