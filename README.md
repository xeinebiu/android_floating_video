## Floating Video Player

This library uses Exo Player as Video Player with ability to use it on a Floating window.

![](./docs/preview.png)
---

### Installation

Step 1. Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://www.jitpack.io' }
		}
	}

Step 2. Add the dependency

	dependencies {
            implementation 'com.github.xeinebiu:android_floating_video:1.4.0'
	}
---

---

### Starting the service

```kotlin
    val stream = Stream(
    Uri.parse("video url here"),
    HashMap()
)
VideoFloatingService.play(
    this,
    VideoItem("demo", listOf(stream))
)
```

### Change logs

    1.4.0
        - Upgrade Gradle Plugin
        - Play the stream which matches the window size by default
        - Move Window dimensions to resources for overwrite flexibility
        - Do not include custom user agent by default
    1.3.0
        - Update dependencies
    1.2.1
        - Bug fixes & improvements
    1.1.0
        - Support Subtitles
    1.0.2
        - Fix: Service crashes when is already running and another stream is provided
    1.0.1
        - Support custom headers for each Stream
    1.0.0
        - Initial Release
