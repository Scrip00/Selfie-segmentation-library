## Android selfie segmentation library by Scrip0

**Backremlib** is a public open-source library, based on [Mediapipe](https://github.com/google/mediapipe).
It is made with Java OpenCV and uses custom [portrait_segmentation graph](https://github.com/Scrip00/Selfie-segmentation-library/tree/main/mediapipe/graphs/portrait_segmentation) and [background_masking_calculator](https://github.com/Scrip00/Selfie-segmentation-library/blob/main/mediapipe/calculators/image/background_masking_calculator.cc) to perform the masking operation on GPU.

| Image background | Video background | Color background |
| :---: | :---: | :---: |
| ![image_background](docs/images/Backremlib/image.gif) | ![video_background](docs/images/Backremlib/video.gif) | ![color_background](docs/images/Backremlib/color.gif) |

## Import library into android project

In order to add the library into your android project, you should add `jitpack.io` in your root `build.gradle`:

```java
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

And in module `build.gradle`:

```java
dependencies {
    implementation 'com.github.Scrip00:Selfie-segmentation-library:1.0.3'
}
```


