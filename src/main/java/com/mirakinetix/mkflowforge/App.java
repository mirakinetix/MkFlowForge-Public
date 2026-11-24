
package com.mirakinetix.mkflowforge;

import com.mirakinetix.mkflowforge.application.LocalTextRefiner;
import com.mirakinetix.mkflowforge.application.PostItCommandParser;
import com.mirakinetix.mkflowforge.infrastructure.audio.JavaSoundRecorder;
import com.mirakinetix.mkflowforge.infrastructure.notes.WindowsStickyNotesService;
import com.mirakinetix.mkflowforge.infrastructure.speech.WhisperCppTranscriber;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class App extends Application {

    private final LocalTextRefiner refiner = new LocalTextRefiner();
    private final PostItCommandParser postItCommandParser =
            new PostItCommandParser();
    private final JavaSoundRecorder recorder = new JavaSoundRecorder();
    private final WhisperCppTranscriber transcriber = new WhisperCppTranscriber();
    private final WindowsStickyNotesService stickyNotesService =
            new WindowsStickyNotesService();

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
                "Dictate offline in your chosen language, or translate speech into English."
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

        Label languageLabel = new Label("Spoken language");
        languageLabel.getStyleClass().add("section-label");
        ComboBox<LanguageOption> languageChoice = new ComboBox<>();
        languageChoice.getItems().setAll(List.of(
                new LanguageOption("Auto-detect", "auto"),
                new LanguageOption("English", "en"),
                new LanguageOption("Hindi", "hi"),
                new LanguageOption("Marathi", "mr"),
                new LanguageOption("Gujarati", "gu"),
                new LanguageOption("Bengali", "bn"),
                new LanguageOption("Tamil", "ta"),
                new LanguageOption("Telugu", "te"),
                new LanguageOption("Kannada", "kn"),
                new LanguageOption("Malayalam", "ml"),
                new LanguageOption("Punjabi", "pa"),
                new LanguageOption("Spanish", "es"),
                new LanguageOption("French", "fr"),
                new LanguageOption("German", "de"),
                new LanguageOption("Portuguese", "pt"),
                new LanguageOption("Chinese", "zh"),
                new LanguageOption("Japanese", "ja"),
                new LanguageOption("Arabic", "ar"),
                new LanguageOption("Russian", "ru")
        ));
        languageChoice.getSelectionModel().selectFirst();
        languageChoice.setPrefWidth(175);
        languageChoice.setAccessibleText("Choose the language spoken in the recording");
        CheckBox translateEnglish = new CheckBox("Translate to English");
        translateEnglish.setAccessibleText("Translate recognized speech into English");
        HBox languageSettings = new HBox(12, languageLabel, languageChoice, translateEnglish);
        languageSettings.setAlignment(Pos.CENTER_LEFT);

        Button recordButton = new Button("Start recording");
        recordButton.getStyleClass().add("button-record");

        Button stopButton = new Button("Stop recording");
        stopButton.getStyleClass().add("button-stop");
        stopButton.setDisable(true);

        Button postNoteButton = new Button("Post as Sticky Note");
        postNoteButton.getStyleClass().add("button-secondary");
        postNoteButton.setDisable(true);

        Button installStickyNotesButton = new Button("Install Sticky Notes");
        installStickyNotesButton.getStyleClass().add("button-install");
        installStickyNotesButton.setVisible(false);
        installStickyNotesButton.setManaged(false);
        installStickyNotesButton.setOnAction(event -> {
            try {
                getHostServices().showDocument(
                        "ms-windows-store://search/?query=Microsoft%20Sticky%20Notes"
                );
            } catch (RuntimeException exception) {
                feedback.setText(
                        "Could not open Microsoft Store. Your transcript is still available to copy."
                );
            }
        });

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
            postNoteButton.setDisable(true);
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
                feedback.setText("Recording in progress. Speak, then click Stop recording.");
            }));
        });

        stopButton.setOnAction(event -> {
            stopButton.setDisable(true);
            recordButton.setDisable(true);
            postNoteButton.setDisable(true);
            feedback.setText("Recording saved. Transcribing locally...");
            LanguageOption selectedLanguage = languageChoice.getValue();
            String selectedLanguageCode = selectedLanguage == null
                    ? "auto"
                    : selectedLanguage.code();
            boolean translateToEnglish = translateEnglish.isSelected();

            CompletableFuture.supplyAsync(() -> {
                final Path wav;

                try {
                    wav = recorder.stopRecording();
                } catch (Exception exception) {
                    throw new CompletionException(exception);
                }

                try {
                    String text = refiner.refine(transcriber.transcribe(
                            wav,
                            selectedLanguageCode,
                            translateToEnglish
                    ));
                    return new TranscriptionOutcome(
                            wav, text, null, false, false, null, null
                    );
                } catch (Exception exception) {
                    return new TranscriptionOutcome(
                            wav, "", exception, false, false, null, null
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

                transcript.setText(outcome.text());

                if (outcome.postItRequested()) {
                    if (outcome.emptyPostItContent()) {
                        feedback.setText(
                                "Say the note contents first, then say “post it.”"
                        );
                    } else if (outcome.postItError() != null) {
                        feedback.setText(
                                "Transcript kept here, but Sticky Notes could not be opened: "
                                        + errorMessage(outcome.postItError())
                                        + ". You can copy the text."
                        );
                    } else {
                        showStickyNotesResult(
                                outcome.postItResult(),
                                feedback,
                                installStickyNotesButton
                        );
                    }
                } else {
                    feedback.setText(
                            "Offline transcription complete. You can edit or copy the text."
                    );
                }
            }));
        });

        FlowPane actions = new FlowPane(
                8, 8, recordButton, stopButton, refineButton, copyButton,
                postNoteButton, installStickyNotesButton, clearButton
        );
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox editor = new VBox(
                14, editorLabel, languageSettings, transcript, actions, feedback
        );
        editor.getStyleClass().add("editor-card");
        VBox.setVgrow(transcript, Priority.ALWAYS);

        Label noteTitle = new Label("YOUR DATA STAYS LOCAL");
        noteTitle.getStyleClass().add("note-title");

        Label noteText = new Label(
                "Audio is recorded to a local temporary WAV file. "
                        + "Speech recognition runs locally. Sticky Notes posting is currently disabled."
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
        javafx.scene.layout.FlowPane brandFooter = new javafx.scene.layout.FlowPane(8, 4);
        brandFooter.setAlignment(Pos.CENTER);
        brandFooter.setPadding(new Insets(8));
        String[][] brandLinks = {
                {"MIRAKINETIX", "https://mirakinetix.com/"},
                {"LinkedIn", "https://www.linkedin.com/in/devdutt-parab/"},
                {"dev dparab.me", "https://dparab.me/"}
        };
        for (String[] link : brandLinks) {
            if (!brandFooter.getChildren().isEmpty()) brandFooter.getChildren().add(new Label("·"));
            Hyperlink button = new Hyperlink(link[0]);
            button.setOnAction(event -> getHostServices().showDocument(link[1]));
            brandFooter.getChildren().add(button);
        }
        root.setBottom(brandFooter);

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
        });
        stage.show();
    }

    private record TranscriptionOutcome(
            Path audio,
            String text,
            Throwable error,
            boolean postItRequested,
            boolean emptyPostItContent,
            WindowsStickyNotesService.Result postItResult,
            Throwable postItError
    ) {}

    private record LanguageOption(String name, String code) {
        @Override
        public String toString() {
            return name;
        }
    }

    private void sendToStickyNotes(
            String text,
            Label feedback,
            Button postButton,
            Button installButton
    ) {
        postButton.setDisable(true);
        feedback.setText("Opening Microsoft Sticky Notes...");

        CompletableFuture.supplyAsync(() -> {
            try {
                return stickyNotesService.createNote(text);
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }
        }).whenComplete((result, failure) -> Platform.runLater(() -> {
            postButton.setDisable(false);

            if (failure != null) {
                installButton.setVisible(false);
                installButton.setManaged(false);
                feedback.setText(
                        "Transcript kept here, but Sticky Notes could not be opened: "
                                + errorMessage(failure)
                                + ". You can copy the text."
                );
                return;
            }

            showStickyNotesResult(result, feedback, installButton);
        }));
    }

    private static void showStickyNotesResult(
            WindowsStickyNotesService.Result result,
            Label feedback,
            Button installButton
    ) {
        installButton.setVisible(
                result == WindowsStickyNotesService.Result.NOT_INSTALLED
        );
        installButton.setManaged(
                result == WindowsStickyNotesService.Result.NOT_INSTALLED
        );

        if (result == WindowsStickyNotesService.Result.CREATED) {
            feedback.setText("Note added to Microsoft Sticky Notes.");
        } else if (result == WindowsStickyNotesService.Result.NOT_INSTALLED) {
            feedback.setText(
                    "Sticky Notes isn’t installed. Your text is still here; "
                            + "copy it or install Sticky Notes and try again."
            );
        } else {
            feedback.setText(
                    "Sticky Notes posting is available on Windows. "
                            + "Your transcript is still here to copy."
            );
        }
    }

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
