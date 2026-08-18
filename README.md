# freej2me-online

<h1 align="center"> FreeJ2ME Online - J2ME emulator with Internet connectivity & easy-play customizations </h1>

<div align="center">

![Java version](https://img.shields.io/badge/Java-8-44cc11?style=for-the-badge&label=Minimum%20Java%20VM)
![License](https://img.shields.io/badge/license-GPLv3-red?style=for-the-badge&label=Project%20License)

</div>

---

# :question: What is it?

**FreeJ2ME Online** is a fork of [FreeJ2ME-Plus](https://github.com/TASEmulators/freej2me-plus) - a J2ME emulator with AWT and libretro frontends.

The goal of this project is to **bring classic J2ME games back with real Internet connectivity**, so apps and games that rely on network protocols (HTTP, Socket, TCP) work like they did on actual devices back in the day - online games, chat clients, data downloads, and more.

On top of that, the project is headed toward **lots of gameplay-oriented customization**: making old games easier and more comfortable to play on modern devices - cheat-style convenience, smoother controls, and quality-of-life tweaks.

### Original authors:
#### - David Richardson [Recompile@retropie]
#### - Saket Dandawate  [Hex@retropie]

### FreeJ2ME-Plus maintainer:
#### - Paulo Sousa [AShiningRay]

---

# :sparkles: Highlights

- **Real network support**: working implementations of `HttpConnection` and `SocketConnection` built on Java's `HttpURLConnection` / `Socket`
  - `http://` and `https://`: GET/POST, request properties, header fields, response codes, timeouts, redirects
  - `socket://`: raw TCP sockets with MIDP socket options (DELAY, LINGER, KEEPALIVE, RCVBUF, SNDBUF)
  - `Connector.open*()` correctly routes to `HttpConnectionImpl` / `SocketConnectionImpl`
- **Java 8 upgrade**: source/target `1.8`, dropped the legacy `bootclasspath`
- **Spec-compliant error handling**: `Connection`, `InputConnection`, `OutputConnection`, `HttpConnection` and `ContentConnection` now declare `IOException` per the MIDP spec
- **Easy-play focus (roadmap)**: the project will keep adding gameplay-oriented customization to make retro gaming more convenient

---

# :video_game: Gameplay customizations (roadmap)

Making games easier and more fun to play is a core direction of this project. Planned and ongoing ideas include:

- **Control enhancements**: custom key mappings per game, virtual gamepad helpers, touch-to-button mapping
- **Convenience features**: in-game save/load slots, state management, fast resume
- **Difficulty helpers**: gameplay assists and tweaks that make classic (often brutal) J2ME games more accessible
- **Network convenience**: helper options to easily point games at custom servers, and cleaner handling of online features

*Expect this section to grow as features land.*

---

# :gear: :coffee: Building FreeJ2ME Online

>**Make sure you have Apache Ant installed and can run it. Then, from the freej2me directory, run the following command (yes, it's that simple):**
>```
> > ant
>```
>**That command will create two different jar files inside `build/`:**
>
>**`freej2me.jar` -> Standalone AWT jar executable, can be double-clicked right away to start**
>
>**`freej2me-lr.jar` -> Libretro executable (has to be placed on the frontend's `system/` folder, since it acts as a BIOS for the libretro core and is what runs J2ME jars)**
>
>### **NOTE: The Libretro jar file needs additional binaries to be compiled before use. Look at the additional steps below if you're going to use it.**

# :gear: :video_game: Building the Libretro core

### Building for Linux:
>**To build the libretro core, be sure you can run the `make` command, then open a terminal in freej2me's folder run the following commands from there:**
>```
># libretro core compilation
> > cd src/libretro
> > make
>```
>**This will build `freej2me_libretro.so` on `src/libretro/`, which is the core libretro will use to interface with `freej2me-lr.jar`.**
>
>**Move it to your libretro frontend's `cores/` folder, with freej2me-lr.jar on `system/` and the frontend should be able to load j2me files afterwards.**
>
> ### **NOTE: The core DOES NOT WORK on containerized/sandboxed environments unless it can call a java runtime that also resides in the same sandbox or container.**
>
> ### **NOTE ON NETWORKING: make sure your frontend allows the core to access the network if you want to play games with online features.**

<h1> </h1>

### Building for Windows:
>**To build the libretro core for windows, first you'll need mingw, or MSYS2 64. `This guide uses MSYS2` as it's easier to set up and works closer to linux syntax.**
>
>**Download MSYS2-x86_64 and install it on your computer. By default it will create a linux-like 'home' folder on C:\msys64\home\ and will put a folder with your username in there. This is where you have to move the freej2me folder to.**
>
>**With the folder placed in there you can build the core, open the MSYS2 UCRT64 terminal from your pc's start menu, and run the following commands:**
>```
># Installing 'mingw-w64' and 'make' on msys2
> > pacman -S mingw-w64-ucrt-x86_64-gcc
> > pacman -S make
>
> # libretro core compilation
> > cd freej2mefolder/src/libretro
> > make
>```
>**This will build `freej2me_libretro.dll` on `freej2mefolder/src/libretro/`.**
>
>**Move it to your libretro frontend's `cores/` folder, with freej2me-lr.jar on `system/` and the frontend should be able to load j2me files afterwards.**
>
> ### **NOTE: The windows core has been tested on Windows 7, 10 & 11 x64.**

---

# :memo: How to use the AWT frontend

Launching the AWT frontend (`freej2me.jar`) directly will bring up the standalone GUI, where you can load your application through the `File` menu, or by **dragging and dropping your JAR/JAD/KJX/MSD file onto it**.

You can also configure many aspects of the runtime, including debug options.

Alternatively it can be launched from the command line with the following arguments:

- `fullscreen` :arrow_right: `1 = yes, 0 = no`
- `width` :arrow_right: the virtual LCD's width
- `height` :arrow_right: the virtual LCD's height
- `scale` :arrow_right: for windowed mode, dictates the scale that the window starts with. The `+`/`-` keys also scale the window by an integer factor of ±1
- `keyLayout` :arrow_right: specifies which device key layout to use on boot. These can be:
  - `0  -> Default`, `1  -> LG`, `2  -> Motorola/Softbank`, `3  -> Motorola Triplets`, `4  -> Motorola V8`, `5  -> Motorola A1000`, `6  -> Nokia Keyboard`, `7  -> Sagem`, `8  -> Siemens`, `9 -> SKT`, `10 -> KDDI`
- `framerate` :arrow_right: sets the maximum FPS applications are allowed to run at (usually `10` to `60`)
- `dojaversion` :arrow_right: sets the DoJa/Star profile for the I-Appli (`10` to `200`)

Read internally as: `java -jar freej2me.jar 'file:///path/to/midlet.jar' fullscreen width height scale keyLayout framerate dojaversion`

### _Notes:_
- On Microsoft Windows, paths require an additional `/` prefix: `C:\path\to\midlet.jar` should be passed as `file:////C:\path\to\midlet.jar`
- FreeJ2ME keeps savedata and config at the working directory it is run from. Settings specified in the config file take precedence over command-line values.

---

# :link: Network support

This project adds real implementations of the standard MIDP network protocols:

| Protocol | Implementation | Description |
|---|---|---|
| `http://`, `https://` | `javax.microedition.io.HttpConnectionImpl` | GET/POST, headers, response codes, timeouts (10s connect / 30s read), redirects |
| `socket://` | `javax.microedition.io.SocketConnectionImpl` | Raw TCP sockets, connect timeout, MIDP socket options |

All `Connector.open*()` flows (Input/DataInput/Output/DataOutput) automatically route to the right implementation based on the URL prefix.

---

# :mag: Modules and external dependencies used:

- #### JLayer (MPEG Player): LGPLv2.1 License, compatible with GPLv3

- #### ObjectWeb's ASM: BSD 3-Clause License, compatible with GPLv3 when the original license is published alongside (check the 'License' tab)

- #### Libretro's API: MIT License, compatible with GPLv3

- #### Roman Lahin [rmn20](https://github.com/rmn20)'s MascotCapsuleV3 renderer (MascotME): MIT License, compatible with GPLv3

---

# :busts_in_silhouette: How to contribute

### If you're a developer:

  1) Open an Issue
  2) Try solving that issue
  3) Post on the Issue if you have a possible solution
  4) Submit a PR implementing the solution

### If you're a user:

  1) Open an Issue
  2) Explain it in as much detail as you can (FreeJ2ME Online version, jar used, md5 hash, as well as the issue with logs and images if possible)
  3) Post a save file close to where the issue manifests, or note the steps required to reproduce it
