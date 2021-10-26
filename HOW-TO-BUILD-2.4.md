# Building JOGL 2.4 for MacOS M1 with ARM64

This explain how to build JOGL 2.4 for Mac M1 with ARM64 processor.

## Repositories

To allow keeping track of edits, Jogamp repository have been mirror here
* https://github.com/jzy3d/gluegen
* https://github.com/jzy3d/jcpp, should be check out as gluegen submodule, see [Gluegen HowToBuild](https://jogamp.org/gluegen/doc/HowToBuild.html)
* https://github.com/jzy3d/jogl
* https://github.com/jzy3d/jogl-utils
* https://github.com/jzy3d/jogl-demos (Existing copy)
* https://github.com/jzy3d/jogamp-scripting (Existing copy, see branch 2.4)

May be usefull
* https://github.com/jzy3d/jogl-demos-maven (not from Jogamp)

## Read How To

* https://jogamp.org/gluegen/doc/HowToBuild.html
* https://jogamp.org/jogl/doc/HowToBuild.html
* https://forum.jogamp.org/JOGL-for-Mac-ARM-Silicon-td4040887.html


## JDK requirements

* An [ARM-aware JDK 8](https://www.azul.com/downloads/?version=java-8-lts&os=macos&architecture=arm-64-bit&package=jdk) is required to provide a rt.jar reference while building Gluegen and JOGL
* An [ARM-aware JDK 1](https://www.azul.com/downloads/?version=java-11-lts&os=macos&architecture=arm-64-bit&package=jdk) is required to be able to read OpenJFX classes which are provided in JOGL dependencies.



## Gluegen

Reminder : as stated in build doc, keep the same console for building Gluegen and then JOGL.

### Edit build scripts

#### Let ant detect ARM64

As [suggested by Manu](https://forum.jogamp.org/JOGL-for-Mac-ARM-Silicon-td4040887.html)
> There was no problem to compile Gluegen but the generated .dylib file was for x86_64, not arm64. Therefore, I replaced `x86_64` by `arm64` in `gluegen/make/gluegen-cpptasks-base.xml` file in the two following lines:

```xml
<compilerarg value="x86_64" if="use.macosx64"/>
<linkerarg value="x86_64" if="use.macosx64"/>
```
> and that was enough to get `libgluegen_rt.dylib` for arm64 architecture (info checked with "lipo" command).
I even succeed to create a "universal" .dylib file which combines both architectures with the command:
`lipo libgluegen_rt-arm64.dylib libgluegen_rt-x86_64.dylib -output libgluegen_rt.dylib -create`

But let's package after building JOGL.

### Configure

```
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-11.jdk/Contents/Home/
```

### Build

```
ant -Dtarget.sourcelevel=1.8 -Dtarget.targetlevel=1.8 -Dtarget.rt.jar=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/jre/lib/rt.jar
```

### Output

* Generated jars are in build/
* Generated native libs in build/obj/

### Testing

#### Check lib file is for appropriate architecture

```
lipo ../../gluegen/build/obj/libgluegen_rt.dylib -archs
```
should output
> `arm64`

#### Run unit tests

```
ant -Dtarget.sourcelevel=1.8 -Dtarget.targetlevel=1.8 -Dtarget.rt.jar=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/jre/lib/rt.jar junit.run
```


## JOGL

Keep the same console open for building JOGL.

### Edit build scripts and dependencies

#### Missing swt

Get from Maven, then copy jar in make/lib/swt/cocoa-macosx-aarch64

Edit `build-common.xml` as follow

```xml
<!--<property name="swt-cocoa-macosx-x86_64.jar" value="${project.root}/make/lib/swt/cocoa-macosx-x86_64/swt.jar"/>
<condition property="swt.jar" value="${swt-cocoa-macosx-x86_64.jar}">
  <and>
    <istrue value="${isOSX}" />
    <or>
      <os arch="AMD64" />
      <os arch="x86_64" />
    </or>
  </and>
</condition>-->
<property name="swt-cocoa-macosx-aarch64.jar" value="${project.root}/make/lib/swt/cocoa-macosx-aarch64/swt.jar"/>
<condition property="swt.jar" value="${swt-cocoa-macosx-aarch64.jar}">
  <and>
    <istrue value="${isOSX}" />
    <!--<or>
      <os arch="arm64" />
    </or>-->
  </and>
</condition>
```

### Build

```
cd ../../jogl/make
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-11.jdk/Contents/Home/
ant -Dtarget.sourcelevel=1.8 -Dtarget.targetlevel=1.8 -Dtarget.rt.jar=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/jre/lib/rt.jar
```

Using JDK 8 to provide rt.jar but using JDK 11 to build is made on purpose!

### Testing

#### Run tests

```
ant -Dtarget.sourcelevel=1.8 -Dtarget.targetlevel=1.8 -Dtarget.rt.jar=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/jre/lib/rt.jar junit.run
```

## Package

### Setup packaging tool

* Clone jogamp-scripting next to jogl and gluegen.
* [Unpack the existing 2.4 archive](https://github.com/jzy3d/jogamp-scripting/blob/2.4-to-jzy3d-maven-repo/maven/README.txt#L34)

### Make a single DYLIB file for x86-64 + arm64

```shell
# Move existing pre-build lib to a better named place
mkdir jogamp-scripting/input/jogamp-all-platforms/lib/macosx-universal/lib-x86-64

mv jogamp-scripting/input/jogamp-all-platforms/lib/macosx-universal/lib/* jogamp-scripting/input/jogamp-all-platforms/lib/macosx-universal/lib-x86-64

# Copy what we built next
mkdir jogamp-scripting/input/jogamp-all-platforms/lib/macosx-universal/lib-arm64/
cp jogl/build/lib/* jogamp-scripting/input/jogamp-all-platforms/lib/macosx-universal/lib-arm64/
cp gluegen/build/obj/libgluegen_rt.dylib jogamp-scripting/input/jogamp-all-platforms/lib/macosx-universal/lib-arm64/

# Merge these libraries
cd jogamp-scripting/input/jogamp-all-platforms/lib/macosx-universal/

lipo lib-arm64/libgluegen_rt.dylib           lib-x86-64/libgluegen_rt.dylib          -output libgluegen_rt.dylib          -create   
lipo lib-arm64/libjogl_cg.dylib              lib-x86-64/libjogl_cg.dylib             -output libjogl_cg.dylib             -create   
#lipo lib-arm64/libjogl_desktop.dylib         lib-x86-64/libjogl_desktop.dylib        -output libjogl_desktop.dylib        -create   
lipo lib-arm64/libjogl_mobile.dylib          lib-x86-64/libjogl_mobile.dylib         -output libjogl_mobile.dylib         -create   
#lipo lib-arm64/libnativewindow_awt.dylib     lib-x86-64/libnativewindow_awt.dylib    -output libnativewindow_awt.dylib    -create   
lipo lib-arm64/libnativewindow_macosx.dylib  lib-x86-64/libnativewindow_macosx.dylib -output libnativewindow_macosx.dylib -create   
lipo lib-arm64/libnewt_head.dylib            lib-x86-64/libnewt_head.dylib           -output libnewt_head.dylib           -create   

# commented dylib are actually empty in arm64
lipo libjogl_cg.dylib -archs

```


## Make a Maven archive

https://github.com/jzy3d/jogamp-scripting/blob/2.4-to-jzy3d-maven-repo/maven/README.txt



## Troubleshooting 2.4

### At build time

* CLI fail while building `NativeTaglet.java`, either renamed to `NativeTaglet_java` or rather use JDK 11 for building as indicated.
* `CStructAnnotationProcessor.java:88: error: cannot find symbol @SupportedSourceVersion(SourceVersion.RELEASE_11)`. Either edit source to change to `@SupportedSourceVersion(SourceVersion.RELEASE_8)`, or use JDK 11 for building as indicated.
* `com.sun.javafx.tk.TKStage` : class file has wrong version 54.0, should be 52.0 > Was built [with java 10](https://en.wikipedia.org/wiki/Java_class_file#General_layout), not 8 as required. Use JDK 10 or above to compile JOGL
* Build hangs with `Unable to install ServerSocket: Address already in use (Bind failed)` : on Mac simply `lsof | 59999` then `kill -9 PID`

### At test time

* Test do not finish because 7z is not a known file or directory : `brew install p7zip`

### At runtime

#### Log messages and exception

* Exception stating `mach-o, but wrong architecture` : you are not running the JOGL program from an ARM-aware JDK/JRE. Using Azul Zulu may help
* Log message stating `FALLBACK (log once): Fallback to SW vertex for line stipple` : thrown by Apple M1 OpenGL driver [when you use a double in a fragment shader](https://forum.jogamp.org/jogl-message-macOS-BigSur-arm-td4041124.html)

#### Known issues

* [Performance regression in context.makeCurrent() from 2.3.2 to 2.4](https://forum.jogamp.org/Major-performance-regression-in-context-makeCurrent-from-2-3-2-to-2-4-0-td4041078.html)
* [Old SWT GLCanvas crashed with 2.4, new CanvasNewtSWT has issue with focus](https://forum.jogamp.org/JOGL-does-not-work-on-versions-higher-than-eclipse-2021-03-td4041328.html)
* [Retina display hang 50% of time on 2.4](https://forum.jogamp.org/JOGL-for-Mac-ARM-Silicon-td4040887.html)
