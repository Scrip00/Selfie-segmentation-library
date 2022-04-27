## Android selfie segmentation library by Scrip0

| Image background | Video background | Color background |
| :---: | :---: | :---: |
| ![image_background](docs/images/Backremlib/image.gif) | ![video_background](docs/images/Backremlib/video.gif) | ![color_background](docs/images/Backremlib/color.gif) |

## Import library into android project

In order to add the library into your android project, you should add `jitpack.io` in your root `buld.gradle`:

```java
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

And in module `buld.gradle`:

```java
dependencies {
    implementation 'com.github.Al3xlav:Selfie-segmentation-library:1.0.3'
}
```
