## Android selfie segmentation library by Scrip0

**Backremlib** is a public open-source library based on [Mediapipe](https://github.com/google/mediapipe).
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

Make sure to include camera permission in your app `AndroidManifest.xml` file:

```java
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" />
```

Override `onResume` and `onPause` methods to make sure that `BackActivity` functions properly:

```java
@Override
protected void onResume() {
    backActivity.resume();
    super.onResume();
}

@Override
protected void onPause() {
    backActivity.pause();
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

There are two options to set color background:

```java
backActivity.setColor(int color);
```

and

```java
backActivity.setColorARGB(int a, int r, int g, int b);
```

For more detailed code examples, check `BackgroundRemover` [example app](https://github.com/Scrip00/Selfie-segmentation-library/tree/main/backgroundremover/app).

## How to build library by yourself

First of all, [install Bazel on your computer](https://docs.bazel.build/versions/main/install.html).

Clone the project with:

```
git clone https://github.com/Scrip00/Selfie-segmentation-library
```

Go to project `WORKSPACE` directory (cd 'path'). Do not forget to specify the `$JAVA_HOME` environment variable (export JAVA_HOME="'path to your jdk'"). I used open-jdk 11 and NDK version 22 (mediapipe may not work with higher NDK versions).

Finally, you can build the `backaar`, which will contain the .aar library with the graph and calculator, which later will be used in `backremlib`, by calling:

```
bazel build -c opt --strip=ALWAYS \
--host_crosstool_top=@bazel_tools//tools/cpp:toolchain \
--fat_apk_cpu=arm64-v8a,armeabi-v7a \
--legacy_whole_archive=0 \
--features=-legacy_whole_archive \
--copt=-fvisibility=hidden \
--copt=-ffunction-sections \
--copt=-fdata-sections \
--copt=-fstack-protector \
--copt=-Oz \
--copt=-fomit-frame-pointer \
--copt=-DABSL_MIN_LOG_LEVEL=2 \
--linkopt=-Wl,--gc-sections,--strip-all \
//mediapipe/examples/android/src/java/com/google/mediapipe/apps/backaar:backaar.aar
```

Finally, you can use generated .aar file by opening your project (or backgroundremover example) with backremlib module and importing the aar in it. One of ways to do it is to place the .aar in `libs` folder and include it in library module `build.gradle` file with:

```
repositories {
    flatDir {
        dirs 'libs'
    }
}
dependencies {
    ...
    compile(name:'backremlib', ext:'aar')
}
```

And sync the project.







































