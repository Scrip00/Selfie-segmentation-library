## Android selfie segmentation library by Scrip0

**Backremlib** is a public open-source library, based on [Mediapipe](https://github.com/google/mediapipe).
It is made with Java OpenCV and uses custom [portrait_segmentation graph](https://github.com/Scrip00/Selfie-segmentation-library/tree/main/mediapipe/graphs/portrait_segmentation) and [background_masking_calculator](https://github.com/Scrip00/Selfie-segmentation-library/blob/main/mediapipe/calculators/image/background_masking_calculator.cc) to perform the masking operation on mobile GPU, increasing its performance.

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

## Code examples

To start selfie segmentation experience, create new object `BackActivity` and specify the `Context` and a `ViewGroup` where you want the library output to be displayed:

```java
BackActivity backActivity = new BackActivity(context, viewGroup);
```

Make sure to include camera permission in your module `AndroidManifest.xml` file:

```java
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" />
```

Override `onResume` and `onPause` methods to make sure that `BackActivity` functions properly:

```java
@Override
protected void onResume() {
    activity.resume();
    super.onResume();
}

@Override
protected void onPause() {
    activity.pause();
    super.onPause();
}
```

Finally, you can set background image with:

```java
backActivity.setImageBackground(Bitmap background, boolean crop);
```

Parameter `crop` specifies if the image will be croppped (crop = true) to fit the `ViewGroup` or stretched (crop = false).

To clear the background, call:

```java
backActivity.setImageBackground(null, false);
```

If you want to set video background, call:

```java
backActivity.setVideo(String path, boolean crop);
```

Where `path` is an absolute path to the video file.

Do not forget to put

```java
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

In your module `AndroidManifest.xml` file.








































