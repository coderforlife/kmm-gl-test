ANGLE Dependency
================

- Download a precompiled ANGLE framework. One source is [https://github.com/levinli303/ANGLESwiftUI/tree/main/ANGLESwiftUI]
  - I don't know how often that is updated
  - Could also compile it ourselves (that repo has an open issue that links to a build script)
- Copy the `libEGL.xcframework` and `libGLESv2.xcframework` folders to the root of the project
- Create the file "composeApp/src/nativeInterop/cinterop/angle.def" with the following content:

```ini
headers = libEGL/libEGL.h libGLESv2/libGLESv2.h
package = angle
language = C
depends = CFCGTypes CoreFoundation CoreFoundationBase CoreGraphics CoreImage CoreText CoreVideo FileProvider Foundation IOSurface ImageIO Metal ModelIO QuartzCore Security Symbols UIKit darwin posix
compilerOpts.ios = -F ../ -framework libEGL -framework libGLESv2 -I ../include
```

- Add the following to the "composeApp/build.gradle.kts" file in the `iosTarget` for-each:

```kotlin
        iosTarget.compilations.forEach {
            it.cinterops {
                val angle by creating {
                    packageName("angle")
                }
            }
        }
```

- Open the iosApp project in Xcode and add the libEGL.xcframework and libGLESv2.xcframework to the "iosApp" target
- Open iosApp project, click on iosApp target:
  - Go to General tab, and under "Frameworks, Libraries, and Embedded Content" set the frameworks to "Embed & Sign"
  - Go to Build Settings tab, and under "Search Paths" set "Framework Search Paths" (debug and release) to `$(SRCROOT)/..`
