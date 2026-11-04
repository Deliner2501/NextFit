package com.nextfit.ui.fx;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.Member;
import com.nextfit.service.CarnetCode;
import com.nextfit.service.MemberService;
import com.nextfit.service.SubscriptionService;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Carnet screen: pick a member, see the carnet (photo, name, document and QR)
 * and save it as a PNG image to print. The carnet keeps its own light colors in
 * both themes, because it is meant to be printed on paper.
 */
final class CarnetPane extends HBox {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d/MM/yyyy", Locale.forLanguageTag("es-CO"));

    private final SubscriptionService subscriptions;
    private final MemberPicker picker;
    private final StackPane previewHolder = new StackPane();
    private final Button save = new Button("Guardar carnet como imagen");
    private VBox carnet;
    private Member current;

    CarnetPane(MemberService members, SubscriptionService subscriptions) {
        super(24);
        this.subscriptions = subscriptions;
        this.picker = new MemberPicker("Socios", members, this::select);

        Label title = new Label("Carnet del socio");
        title.getStyleClass().add("nf-section-title");
        Label hint = new Label("Elige un socio para ver su carnet. El QR contiene solo su número de documento.");
        hint.getStyleClass().add("nf-muted-small");
        hint.setWrapText(true);
        save.getStyleClass().add("nf-primary");
        save.setDisable(true);
        save.setOnAction(e -> export());
        previewHolder.setPadding(new Insets(10, 0, 0, 0));
        previewHolder.setAlignment(Pos.TOP_LEFT);

        VBox right = new VBox(12, title, hint, previewHolder, save);
        HBox.setHgrow(right, Priority.ALWAYS);
        getChildren().addAll(picker, right);
        select(null);
    }

    void refresh() {
        picker.reload();
    }

    private void select(Member member) {
        current = member;
        previewHolder.getChildren().clear();
        save.setDisable(member == null);
        if (member == null) {
            Label empty = new Label("Selecciona un socio de la lista");
            empty.getStyleClass().add("nf-muted-small");
            previewHolder.getChildren().add(empty);
            carnet = null;
            return;
        }
        carnet = buildCarnet(member);
        previewHolder.getChildren().add(carnet);
    }

    private VBox buildCarnet(Member member) {
        Label brand = new Label("NEXTFIT");
        brand.getStyleClass().add("nf-carnet-brand");
        Label kind = new Label("CARNET DE SOCIO");
        kind.getStyleClass().add("nf-carnet-kind");
        VBox header = new VBox(2, brand, kind);

        Label name = new Label(member.getFullName());
        name.getStyleClass().add("nf-carnet-name");
        name.setWrapText(true);
        Label document = new Label("Documento " + member.getDocumentNumber());
        document.getStyleClass().add("nf-carnet-text");
        String validity = subscriptions.findCurrentFor(member.getId())
                .map(s -> "Membresía hasta " + s.getEndDate().format(DAY))
                .orElse("Sin membresía vigente");
        Label until = new Label(validity);
        until.getStyleClass().add("nf-carnet-text");
        VBox data = new VBox(6, name, document, until);

        VBox identity = new VBox(14, MemberAvatar.of(member, 120), data);
        identity.setAlignment(Pos.TOP_LEFT);

        ImageView qr = new ImageView(QrImage.of(CarnetCode.encode(member.getDocumentNumber()), 8));
        qr.setFitWidth(170);
        qr.setFitHeight(170);
        qr.setSmooth(false);
        qr.setPreserveRatio(true);
        Label scanHint = new Label("Presenta este código en la entrada");
        scanHint.getStyleClass().add("nf-carnet-text");
        VBox qrBox = new VBox(8, qr, scanHint);
        qrBox.setAlignment(Pos.CENTER);

        HBox body = new HBox(28, identity, qrBox);
        body.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(18, header, body);
        card.getStyleClass().add("nf-carnet");
        card.setPadding(new Insets(24));
        card.setPrefWidth(620);
        card.setMinWidth(620);
        card.setMaxWidth(620);
        return card;
    }

    private void export() {
        if (carnet == null || current == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar carnet");
        chooser.setInitialFileName("carnet-" + current.getDocumentNumber() + ".png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagen PNG", "*.png"));
        File target = chooser.showSaveDialog(save.getScene().getWindow());
        if (target == null) {
            return;
        }
        try {
            // Three times the screen size, so the printed carnet is sharp.
            SnapshotParameters params = new SnapshotParameters();
            params.setTransform(new Scale(3, 3));
            WritableImage image = carnet.snapshot(params, null);
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", target);
            FxDialogs.message(save, "Carnet guardado", "La imagen quedó en " + target.getAbsolutePath());
        } catch (IOException e) {
            FxDialogs.error(save, new PersistenceException("No se pudo guardar el carnet", e));
        }
    }
}
