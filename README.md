
# MkFlowForge

**Local-first voice dictation for your desktop.**

MkFlowForge is a Java desktop voice-dictation application developed by **Mirakinetix**. It combines microphone recording and local speech recognition using whisper.cpp.

**Build. Automate. Scale.**

## Features

- Desktop voice-dictation interface built with JavaFX.
- Local speech recognition through whisper.cpp.
- In-app recording controls; no global keyboard hook is installed.
- Edit or copy the transcript from the app.
- Multilingual transcription with automatic detection or a selectable spoken language.
- Optional translation of recognized speech into English.
- Configurable local speech-engine and model paths.
- Maven-based build and automated tests.
- Windows and macOS are the intended desktop platforms.

## Requirements

- Java Development Kit (JDK) 25.
- Maven.
- A compatible whisper.cpp command-line executable.
- A compatible Whisper model file, such as `ggml-base.bin`.
- A working microphone and permission to access it.
- Windows or macOS for the intended desktop experience.

The speech-recognition executable and model are **not bundled with this source code**. Obtain them separately from a trusted source and comply with their respective licenses.

## Build and run from source

Clone the repository:

```powershell
git clone https://github.com/mirakinetix/MkFlowForge-Public.git
cd MkFlowForge-Public
```

Verify the project and run its tests:

```powershell
mvn clean verify
```

Launch the desktop application:

```powershell
mvn javafx:run
```

These commands require a compatible Java 25 installation and Maven. The verification command builds the project and runs the automated test suite.

## Configure local speech recognition

MkFlowForge expects a local whisper.cpp executable and a model file.

### Default locations on Windows

The application looks for the executable at:

```text
%LOCALAPPDATA%\MkFlowForge\whisper\bin-b5454\whisper-cli.exe
```

The default model location is:

```text
%LOCALAPPDATA%\MkFlowForge\whisper\models\ggml-base.bin
```

### Default locations on macOS

The application uses the following base directory under your home folder:

```text
~/Library/Application Support/MkFlowForge/whisper/
```

The executable is expected under `bin-b5454/whisper-cli`, and the model under `models/ggml-base.bin`.

### Override the paths

You can configure custom paths using environment variables.

PowerShell example for the current session:

```powershell
$env:MKFLOWFORGE_WHISPER_CLI = 'C:\path\to\whisper-cli.exe'
$env:MKFLOWFORGE_WHISPER_MODEL = 'C:\path\to\ggml-base.bin'
mvn javafx:run
```

On macOS, set the same environment variables to the full paths of your local executable and model before launching the application.

The executable must be compatible with your operating system and processor architecture.

## Privacy and local processing

MkFlowForge is designed around local speech recognition: recorded audio is passed to the locally configured whisper.cpp executable rather than a hosted transcription API. Its multilingual model can transcribe supported spoken languages; selecting a known language can help when auto-detection is unreliable. Optional translation uses whisper.cpp's English-target translation mode.

- Keep your speech-recognition executable and model on a device you control.
- Review the executable and model sources before installing them.
- Protect recordings and transcripts according to your own privacy requirements.
- The application must have microphone access to record audio.

This description covers the local transcription path; it is not a guarantee that every dependency or future version of the application is completely network-free.

## Development and testing

The project uses:

- Java 25
- JavaFX
- Maven
- JUnit 5 for automated tests

Run the verification suite with:

```powershell
mvn clean verify
```

GitHub Actions also runs the verification workflow on Windows and macOS for configured branches and pull requests.

## Current status and limitations

- This repository provides application source code, not a ready-made Windows or macOS installer.
- The whisper.cpp executable and model must be installed separately.
- Microphone access may require operating-system permission.
- Global dictation shortcuts and automatic text insertion are unavailable in this preview. Use the in-app controls to record, edit, and copy text.
- Sticky Notes posting is disabled in this preview.
- Compatibility should be validated on the target machine before relying on the application.

## Contributing

Bug reports, reproducible issues, and reviewed contributions are welcome. Before submitting changes, run:

```powershell
mvn clean verify
```

Please include your operating system, Java version, reproduction steps, and relevant error messages when reporting a problem. Do not attach private recordings, credentials, or other sensitive data.

## Project

**MkFlowForge by Mirakinetix**

- Website: https://mirakinetix.com
- LinkedIn: https://www.linkedin.com/in/devdutt-parab/
- Source repository: https://github.com/mirakinetix/MkFlowForge-Public

**Build. Automate. Scale.**
