MetalANGLE Dependency
=====================

- create the file "composeApp/src/nativeInterop/cinterop/MetalANGLE.def" with the following content:

```ini
headers = MGLKit.h
headerFilter = MGL*.h *gl*.h
package = metal_angle
language = Objective-C
depends = CFCGTypes CFNetwork CoreFoundation CoreFoundationBase CoreGraphics CoreImage CoreText CoreVideo EAGL FileProvider Foundation IOSurface ImageIO Metal ModelIO OpenGLESCommon QuartzCore Security Symbols UIKit UserNotifications darwin posix

compilerOpts.osx = -framework ../MetalANGLE/macosx/MetalANGLE -I ../MetalANGLE/include
compilerOpts.ios_x64 = -framework ../MetalANGLE/macosx/MetalANGLE -I ../MetalANGLE/include
compilerOpts.ios_arm64 = -framework ../MetalANGLE/iphoneos/MetalANGLE -I ../MetalANGLE/include
compilerOpts.ios_simulator_arm64 = -framework ../MetalANGLE/iphonesimulator/MetalANGLE -I ../MetalANGLE/include
```

- Add the following to the "composeApp/build.gradle.kts" file in the `iosTarget` for-each:

```kotlin
        iosTarget.compilations.forEach {
            it.cinterops {
                val MetalANGLE by creating {
                    packageName("metal_angle")
                }
            }
        }
```

- Open the iosApp project in Xcode and add the MetalANGLE.framework to the "iosApp" target
  - TODO: you seem to only be able to add the framework once here - how to make this auto-select iphoneos or iphonesimulator? (maybe already does this with the below steps)
- Open iosApp project, click on iosApp target:
  - Go to General tab, and under "Frameworks, Libraries, and Embedded Content" set "MetalANGLE.framework" to "Embed & Sign"
  - Go to Build Settings tab, and under "Search Paths" set "Framework Search Paths" (debug and release) to `$(SRCROOT)/../MetalANGLE/$(PLATFORM_NAME)`
    - If there is a hardcoded path here to a specific version of MetalANGLE, remove it

In the future we would like to move to real ANGLE but there are two issues:
1. Cannot find precompiled binaries for this at all... but we could compile it ourselves
2. Needs the MLKit wrapper to be able to use it in iOS easily, possibly already available at [github.com/rednblackgames/MGLKit]
