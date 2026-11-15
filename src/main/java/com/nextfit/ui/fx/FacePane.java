package com.nextfit.ui.fx;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Member;
import com.nextfit.service.AccessResult;
import com.nextfit.service.MemberService;
import com.nextfit.service.face.FaceAccessService;
import com.nextfit.service.face.FaceCamera;
import com.nextfit.service.face.FaceCameraException;
import com.nextfit.service.face.FaceEntryFlow;
import com.nextfit.service.face.FaceEnrollmentService;
import com.nextfit.service.face.FaceFrame;
import com.nextfit.service.face.FaceModels;
import com.nextfit.service.face.FaceSampleCollector;
import com.nextfit.vision.OpenCvFaceCamera;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The "Rostro" screen: entering with the face (recognition + head turn) and registering or deleting the
 * face of a member. The camera runs in its own thread; everything that touches the screen or the
 * services goes back to the JavaFX thread.
 */
final class FacePane extends HBox {

    private static final String CONSENT = """
            Se guardará únicamente un código numérico de tu rostro (no la foto) para dejarte entrar sin carnet.
            Solo se usa para el control de acceso del gimnasio y no se comparte con nadie.
            Puedes pedir que lo borremos cuando quieras, y se borra solo cuando dejas de ser socio.
            (Ley 1581 de 2012, protección de datos personales)

            ¿El socio autoriza el registro de su rostro?""";

    private final FaceAccessService access;
    private final FaceEnrollmentService enrollment;
    private final Path dataDir;
    private final Supplier<String> actor;

    private final ImageView preview = new ImageView();
    private final Label hint = new Label("La cámara está apagada");
    private final Label outcome = new Label();
    private final Button entryButton = new Button("Iniciar entrada con rostro");
    private final Button enrollButton = new Button("Registrar rostro");
    private final Button removeButton = new Button("Eliminar rostro");
    private final Label status = new Label("Elige un socio");
    private final MemberPicker picker;

    /** The result stays on screen only for a few seconds. */
    private final PauseTransition clearOutcome = new PauseTransition(Duration.seconds(6));
    private volatile boolean running;
    private Thread worker;

    FacePane(FaceAccessService access, FaceEnrollmentService enrollment, MemberService members,
             Path dataDir, Supplier<String> actor, Function<Member, List<String>> tags) {
        super(24);
        this.access = access;
        this.enrollment = enrollment;
        this.dataDir = dataDir;
        this.actor = actor;
        this.picker = new MemberPicker("Registrar el rostro de un socio", members, m -> updateStatus(), tags);
        setPadding(new Insets(0, 4, 4, 4));
        getChildren().addAll(buildCameraColumn(), buildEnrollColumn());
        // The tab content leaves the scene when another tab or module is shown: the camera must stop.
        clearOutcome.setOnFinished(e -> outcome.setText(""));
        sceneProperty().addListener((obs, old, now) -> {
            if (now == null) {
                stop();
                say("");
            }
        });
    }

    /** Called when the screen is shown again. */
    void refresh() {
        say("");
        access.refresh();
        picker.reload();
        updateStatus();
    }

    /** Turns the camera off. */
    void stop() {
        running = false;
        entryButton.setText("Iniciar entrada con rostro");
        entryButton.setDisable(false);
        preview.setImage(null);
        hint.setText("La cámara está apagada");
        updateStatus();
    }

    // ---------------------------------------------------------------- layout

    private VBox buildCameraColumn() {
        Label title = new Label("Entrada con el rostro");
        title.getStyleClass().add("nf-section-title");
        Label help = new Label("El socio se coloca frente a la cámara, gira la cabeza a ambos lados "
                + "y se confirma su entrada.");
        help.getStyleClass().add("nf-muted-small");
        help.setWrapText(true);

        preview.setFitWidth(480);
        preview.setFitHeight(360);
        preview.setPreserveRatio(true);
        StackPane frame = new StackPane(preview);
        frame.getStyleClass().add("nf-access-card");
        frame.setMinSize(480, 360);
        frame.setPadding(new Insets(8));

        hint.getStyleClass().add("nf-access-message");
        hint.setWrapText(true);
        outcome.getStyleClass().add("nf-access-verdict");
        outcome.setWrapText(true);

        entryButton.getStyleClass().add("nf-primary");
        entryButton.setOnAction(e -> {
            if (running) {
                stop();
            } else {
                startEntry();
            }
        });
        VBox box = new VBox(12, title, help, frame, hint, outcome, entryButton);
        box.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox buildEnrollColumn() {
        status.getStyleClass().add("nf-muted-small");
        status.setWrapText(true);
        enrollButton.getStyleClass().add("nf-primary");
        enrollButton.setOnAction(e -> startEnrollment());
        removeButton.getStyleClass().add("nf-secondary");
        removeButton.setOnAction(e -> removeFace());
        HBox buttons = new HBox(10, enrollButton, removeButton);
        VBox box = new VBox(12, picker, status, buttons);
        box.setPrefWidth(380);
        box.setMinWidth(340);
        VBox.setVgrow(picker, Priority.ALWAYS);
        return box;
    }

    /** Shows a result and removes it after a few seconds. */
    private void say(String text) {
        outcome.setText(text);
        clearOutcome.stop();
        if (!text.isEmpty()) {
            clearOutcome.playFromStart();
        }
    }

    private void updateStatus() {
        if (picker == null) {
            return;
        }
        picker.refreshTags();
        Member member = picker.selected();
        boolean has = member != null;
        boolean enrolled = has && enrollment.isEnrolled(member.getId());
        status.setText(!has ? "Elige un socio"
                : enrolled ? member.getFullName() + " ya tiene su rostro registrado."
                : member.getFullName() + " no tiene rostro registrado.");
        enrollButton.setDisable(!has || !member.isActive() || running);
        removeButton.setDisable(!enrolled || running);
    }

    // ---------------------------------------------------------------- entry

    private void startEntry() {
        Optional<FaceCamera> camera = openCamera();
        if (camera.isEmpty()) {
            return;
        }
        access.refresh();
        running = true;
        entryButton.setText("Detener cámara");
        enrollButton.setDisable(true);
        removeButton.setDisable(true);
        say("");
        FaceEntryFlow flow = new FaceEntryFlow(access::match, Clock.systemDefaultZone());
        runCamera(camera.get(), frame -> {
            FaceEntryFlow.Step step = flow.feed(frame);
            showFrame(frame, step.hint());
            if (step.phase() == FaceEntryFlow.Phase.DONE) {
                Long id = step.memberId();
                Platform.runLater(() -> {
                    showResult(access.finish(id));
                    stop();
                });
                return false;
            } else if (step.unknown()) {
                Platform.runLater(() -> {
                    showResult(access.reject(step.hint()));
                    stop();
                });
                return false;
            }
            return true;
        });
    }

    private void showResult(AccessResult result) {
        say(switch (result.outcome()) {
            case ENTRY -> "ENTRADA: " + result.member().getFullName();
            case EXIT -> "SALIDA: " + result.member().getFullName();
            case DENIED -> "DENEGADO: " + result.message();
        });
    }

    // ---------------------------------------------------------------- enrollment

    private void startEnrollment() {
        Member member = picker.selected();
        if (member == null) {
            return;
        }
        if (!FxDialogs.confirm(this, "Consentimiento", CONSENT, "Sí, autoriza")) {
            return;
        }
        Optional<FaceCamera> camera = openCamera();
        if (camera.isEmpty()) {
            return;
        }
        running = true;
        entryButton.setDisable(true);
        enrollButton.setDisable(true);
        removeButton.setDisable(true);
        say("Registrando a " + member.getFullName());
        clearOutcome.stop();
        FaceSampleCollector collector = new FaceSampleCollector(Clock.systemDefaultZone());
        runCamera(camera.get(), frame -> {
            collector.offer(frame);
            showFrame(frame, collector.hint(frame));
            if (collector.complete()) {
                List<float[]> samples = collector.samples();
                Platform.runLater(() -> finishEnrollment(member, samples));
                return false;
            }
            return true;
        });
    }

    private void finishEnrollment(Member member, List<float[]> samples) {
        stop();
        try {
            enrollment.enroll(member, samples, actor.get());
            access.refresh();
            say("Rostro registrado: " + member.getFullName());
        } catch (NextFitException e) {
            say("");
            FxDialogs.error(this, e);
        }
        updateStatus();
    }

    private void removeFace() {
        Member member = picker.selected();
        if (member == null || !FxDialogs.confirm(this, "Eliminar rostro",
                "Se borrará el rostro registrado de " + member.getFullName() + ".", "Eliminar")) {
            return;
        }
        enrollment.remove(member, actor.get());
        access.refresh();
        say("Rostro eliminado: " + member.getFullName());
        updateStatus();
    }

    // ---------------------------------------------------------------- camera thread

    private Optional<FaceCamera> openCamera() {
        Optional<FaceModels> models = FaceModels.locate(dataDir);
        if (models.isEmpty()) {
            FxDialogs.message(this, "Faltan los modelos", FaceModels.missingMessage(dataDir));
            return Optional.empty();
        }
        try {
            return Optional.of(OpenCvFaceCamera.open(models.get(), 0));
        } catch (FaceCameraException e) {
            FxDialogs.message(this, "No se pudo abrir la cámara", e.getMessage());
            return Optional.empty();
        }
    }

    /** Reads pictures until {@code onFrame} returns false or the screen asks to stop. */
    private void runCamera(FaceCamera camera, java.util.function.Predicate<FaceFrame> onFrame) {
        worker = new Thread(() -> {
            try (camera) {
                while (running) {
                    if (!onFrame.test(camera.next())) {
                        break;
                    }
                }
            } catch (FaceCameraException e) {
                Platform.runLater(() -> {
                    stop();
                    hint.setText("Error de la cámara: " + e.getMessage());
                });
            }
        }, "nextfit-face-camera");
        worker.setDaemon(true);
        worker.start();
    }

    private final AtomicBoolean drawing = new AtomicBoolean();

    /** Shows a picture and a text, skipping pictures when the screen is still busy with the last one. */
    private void showFrame(FaceFrame frame, String text) {
        if (!drawing.compareAndSet(false, true)) {
            return;
        }
        Platform.runLater(() -> {
            try {
                if (!running) {
                    return;
                }
                if (frame.preview() != null && frame.preview().length > 0) {
                    preview.setImage(new Image(new ByteArrayInputStream(frame.preview())));
                }
                hint.setText(text);
            } finally {
                drawing.set(false);
            }
        });
    }
}
