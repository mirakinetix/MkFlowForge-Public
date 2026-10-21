
package com.mirakinetix.mkflowforge;

import com.mirakinetix.mkflowforge.application.LocalTextRefiner;
import com.mirakinetix.mkflowforge.infrastructure.audio.JavaSoundRecorder;
import com.mirakinetix.mkflowforge.infrastructure.hotkey.GlobalHotkeyService;
import com.mirakinetix.mkflowforge.infrastructure.insertion.SystemTextInserter;
import com.mirakinetix.mkflowforge.infrastructure.speech.WhisperCppTranscriber;
import com.github.kwhat.jnativehook.NativeHookException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class App extends Application {

    private final LocalTextRefiner refiner = new LocalTextRefiner();
    private final JavaSoundRecorder recorder = new JavaSoundRecorder();
    private final WhisperCppTranscriber transcriber = new WhisperCppTranscriber();
    private final SystemTextInserter textInserter = new SystemTextInserter();

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");

        ImageView brandLogo = new ImageView(
                new Image(App.class.getResourceAsStream("header-300x90.png"))
        );
        brandLogo.setFitHeight(48);
        brandLogo.setPreserveRatio(true);
        brandLogo.setSmooth(true);
        brandLogo.setAccessibleText("Mirakinetix - Build. Automate. Scale.");

        VBox brandText = new VBox(3);
        Label productName = new Label("MkFlowForge");
        productName.getStyleClass().add("brand-title");

        Label publisher = new Label("Build. Automate. Scale.");
        publisher.getStyleClass().add("brand-tagline");

        brandText.getChildren().addAll(productName, publisher);

        HBox brandGroup = new HBox(12, brandLogo, brandText);
        brandGroup.setAlignment(Pos.CENTER_LEFT);

        Label mode = new Label("LOCAL-FIRST");
        mode.getStyleClass().add("status-pill");

        Hyperlink linkedInLink = new Hyperlink("LinkedIn");
        linkedInLink.getStyleClass().add("brand-link");
        linkedInLink.setAccessibleText("Open Devdutt Parab's LinkedIn profile");
        linkedInLink.setOnAction(event ->
                getHostServices().showDocument("https://www.linkedin.com/in/devdutt-parab/")
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(18, brandGroup, spacer, linkedInLink, mode);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("top-bar");
        root.setTop(header);

        Label eyebrow = new Label("VOICE WORKSPACE");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("Speak naturally. Write clearly.");
        title.getStyleClass().add("hero-title");
        title.setWrapText(true);

        Label subtitle = new Label(
                "Dictate offline, then paste recognized text into the active app."
        );
        subtitle.getStyleClass().add("hero-subtitle");
        subtitle.setWrapText(true);

        VBox hero = new VBox(10, eyebrow, title, subtitle);
        hero.getStyleClass().add("hero");

        Label editorLabel = new Label("TRANSCRIPT");
        editorLabel.getStyleClass().add("section-label");

        TextArea transcript = new TextArea();
        transcript.setPromptText(
                "Your offline transcript will appear here after recording stops..."
        );
        transcript.setWrapText(true);
        transcript.setPrefRowCount(12);
        transcript.getStyleClass().add("transcript-area");
        VBox.setVgrow(transcript, Priority.ALWAYS);

        Label feedback = new Label("Ready. Microphone recording is available.");
        feedback.getStyleClass().add("feedback-text");
        feedback.setWrapText(true);

        Button recordButton = new Button("Start recording");
        recordButton.getStyleClass().add("button-record");

        Button stopButton = new Button("Stop recording");
        stopButton.getStyleClass().add("button-stop");
        stopButton.setDisable(true);

        AtomicBoolean pasteAfterTranscription = new AtomicBoolean(false);

        Button refineButton = new Button("Refine transcript");
        refineButton.getStyleClass().add("button-primary");
        refineButton.setOnAction(event -> {
            if (transcript.getText().isBlank()) {
                feedback.setText("Enter or paste some text first.");
                return;
            }

            transcript.setText(refiner.refine(transcript.getText()));
            feedback.setText("Local text cleanup complete.");
        });

        Button copyButton = new Button("Copy text");
        copyButton.getStyleClass().add("button-secondary");
        copyButton.setOnAction(event -> {
            String text = transcript.getText();

            if (text == null || text.isBlank()) {
                feedback.setText("There is no text to copy.");
                return;
            }

            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);
            feedback.setText("Transcript copied to the clipboard.");
        });

        Button clearButton = new Button("Clear");
        clearButton.getStyleClass().add("button-secondary");
        clearButton.setOnAction(event -> {
            transcript.clear();
            feedback.setText("Editor cleared.");
        });

        recordButton.setOnAction(event -> {
            recordButton.setDisable(true);
            stopButton.setDisable(true);
            feedback.setText("Opening microphone...");

            CompletableFuture.supplyAsync(() -> {
                try {
                    return recorder.startRecording();
                } catch (Exception exception) {
                    throw new CompletionException(exception);
                }
            }).whenComplete((path, failure) -> Platform.runLater(() -> {
                if (failure != null) {
                    recordButton.setDisable(false);
                    stopButton.setDisable(true);
                    feedback.setText(
                            "Could not start recording: " + errorMessage(failure)
                    );
                    return;
                }

                stopButton.setDisable(false);
                feedback.setText(
                        "Recording in progress. Speak, then click Stop or press "
                                + GlobalHotkeyService.shortcutDescription() + "."
                );
            }));
        });

        stopButton.setOnAction(event -> {
            stopButton.setDisable(true);
            recordButton.setDisable(true);
            feedback.setText("Recording saved. Transcribing locally...");

            CompletableFuture.supplyAsync(() -> {
                final Path wav;

                try {
                    wav = recorder.stopRecording();
                } catch (Exception exception) {
                    pasteAfterTranscription.set(false);
                    throw new CompletionException(exception);
                }

                boolean pasteIntoActiveApp =
                        pasteAfterTranscription.getAndSet(false);

                try {
                    String text = transcriber.transcribe(wav);
                    Throwable insertionError = null;

                    if (pasteIntoActiveApp && !text.isBlank()) {
                        try {
                            textInserter.paste(refiner.refine(text));
                        } catch (Exception exception) {
                            insertionError = exception;
                        }
                    }

                    return new TranscriptionOutcome(
                            wav, text, null, insertionError, pasteIntoActiveApp
                    );
                } catch (Exception exception) {
                    return new TranscriptionOutcome(
                            wav, "", exception, null, pasteIntoActiveApp
                    );
                }
            }).whenComplete((outcome, failure) -> Platform.runLater(() -> {
                recordButton.setDisable(false);
                stopButton.setDisable(true);

                if (failure != null) {
                    feedback.setText(
                            "Could not finish recording: " + errorMessage(failure)
                    );
                    return;
                }

                if (outcome.error() != null) {
                    feedback.setText(
                            "WAV saved, but transcription failed: "
                                    + errorMessage(outcome.error())
                    );
                    return;
                }

                if (outcome.text().isBlank()) {
                    feedback.setText(
                            "No speech recognized. WAV saved: "
                                    + outcome.audio().getFileName()
                    );
                    return;
                }

                transcript.setText(refiner.refine(outcome.text()));

                if (outcome.insertionError() != null) {
                    feedback.setText(
                            "Transcription complete, but automatic paste failed: "
                                    + errorMessage(outcome.insertionError())
                                    + ". You can use Copy text."
                    );
                } else if (outcome.pastedIntoActiveApp()) {
                    feedback.setText(
                            "Offline transcription pasted into the active app. WAV: "
                                    + outcome.audio().getFileName()
                    );
                } else {
                    feedback.setText(
                            "Offline transcription complete. WAV: "
                                    + outcome.audio().getFileName()
                    );
                }
            }));
        });

        GlobalHotkeyService hotkeyService = new GlobalHotkeyService(() ->
                Platform.runLater(() -> {
                    if (!recordButton.isDisabled() && stopButton.isDisabled()) {
                        recordButton.fire();
                    } else if (recordButton.isDisabled() && !stopButton.isDisabled()) {
                        pasteAfterTranscription.set(true);
                        stopButton.fire();
                    } else {
                        feedback.setText(
                                "Dictation is busy. Wait until the current operation finishes."
                        );
                    }
                })
        );

        try {
            hotkeyService.start();
            feedback.setText(
                    "Ready. Press " + GlobalHotkeyService.shortcutDescription()
                            + " to dictate into the active app."
            );
        } catch (NativeHookException exception) {
            feedback.setText(
                    "Global shortcut unavailable: " + errorMessage(exception)
                            + ". Use the recording buttons."
            );
        }

        HBox actions = new HBox(
                10, recordButton, stopButton, refineButton, copyButton, clearButton
        );
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setSpacing(8);

        VBox editor = new VBox(
                14, editorLabel, transcript, actions, feedback
        );
        editor.getStyleClass().add("editor-card");
        VBox.setVgrow(transcript, Priority.ALWAYS);

        Label noteTitle = new Label("YOUR DATA STAYS LOCAL");
        noteTitle.getStyleClass().add("note-title");

        Label noteText = new Label(
                "Audio is recorded to a local temporary WAV file. "
                        + "Speech recognition runs locally. The global shortcut can paste "
                        + "recognized text into the active application."
        );
        noteText.getStyleClass().add("note-body");
        noteText.setWrapText(true);

        VBox note = new VBox(8, noteTitle, noteText);
        note.getStyleClass().add("note-card");

        VBox content = new VBox(24, hero, editor, note);
        content.getStyleClass().add("content-column");

        StackPane center = new StackPane(content);
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(34));
        root.setCenter(center);

        Scene scene = new Scene(root, 1100, 780);
        scene.getStylesheets().add(
                App.class.getResource("app.css").toExternalForm()
        );

        stage.setTitle("MkFlowForge by Mirakinetix");
        stage.setMinWidth(850);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            recorder.cancelRecording();
            hotkeyService.close();
        });
        stage.show();
    }

    private record TranscriptionOutcome(
            Path audio,
            String text,
            Throwable error,
            Throwable insertionError,
            boolean pastedIntoActiveApp
    ) {}

    private static String errorMessage(Throwable failure) {
        Throwable cause = failure;

        while ((cause instanceof CompletionException)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }

        String message = cause.getMessage();
        return message == null || message.isBlank()
                ? cause.getClass().getSimpleName()
                : message;
    }

    public static void main(String[] args) {
        launch(args);
    }
}