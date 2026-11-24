package com.nextfit.ui.fx;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.export.CarnetPdf;
import com.nextfit.export.QrMatrix;
import com.nextfit.model.Member;
import com.nextfit.service.MemberService;
import com.nextfit.service.PhoneAccess;
import javafx.embed.swing.SwingFXUtils;
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
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/**
 * Carnet screen: pick a member, see the carnet (photo, name, document and QR)
 * and save it as a PNG image to print. The carnet keeps the NextFit brand colors in
 * both application themes.
 */
final class CarnetPane extends HBox {

    private final MemberService members;
    private final PhoneAccess phone;
    private final Supplier<String> phoneUrl;
    private final MemberPicker picker;
    private final StackPane previewHolder = new StackPane();
    private final Label phoneInfo = new Label();
    private final Button save = new Button("Guardar carnet como imagen");
    private final Button savePdf = new Button("Guardar carnet en PDF");
    private final Button saveAllPdf = new Button("PDF de todos los socios activos");
    private VBox carnet;
    private Member current;

    CarnetPane(MemberService members, PhoneAccess phone,
               Supplier<String> phoneUrl) {
        super(24);
        this.members = members;
        this.phone = phone;
        this.phoneUrl = phoneUrl;
        this.picker = new MemberPicker("Socios", members, this::select);

        Label title = new Label("Carnet del socio");
        title.getStyleClass().add("nf-section-title");
        Label hint = new Label("Elige un socio para emitir su carnet. El QR abre el portal personal cuando el socio tiene una membresía activa.");
        hint.getStyleClass().add("nf-muted-small");
        hint.setWrapText(true);
        save.getStyleClass().add("nf-primary");
        save.setDisable(true);
        save.setOnAction(e -> export());
        savePdf.getStyleClass().add("nf-secondary");
        savePdf.setDisable(true);
        savePdf.setOnAction(e -> exportPdf(List.of(current)));
        saveAllPdf.getStyleClass().add("nf-secondary");
        saveAllPdf.setOnAction(e -> exportPdf(members.findAll().stream().filter(Member::isActive).toList()));
        previewHolder.setPadding(new Insets(10, 0, 0, 0));
        previewHolder.setAlignment(Pos.TOP_LEFT);

        HBox actions = new HBox(10, save, savePdf, saveAllPdf);
        phoneInfo.getStyleClass().add("nf-muted-small");
        phoneInfo.setWrapText(true);
        VBox right = new VBox(12, title, hint, phoneInfo, previewHolder, actions);
        HBox.setHgrow(right, Priority.ALWAYS);
        getChildren().addAll(picker, right);
        select(null);
    }

    void refresh() {
        picker.reload();
        String portal = phoneUrl.get();
        phoneInfo.setText(portal.startsWith("https://")
                ? "Para comprobar el portal en el celular, abre " + portal + "/ping"
                : "Para probar la conexión del celular (misma red Wi-Fi) abre " + portal + "/ping");
        // The address of this computer may have changed, so the QR is drawn again.
        if (current != null) {
            select(current);
        }
    }

    private void select(Member member) {
        current = member;
        previewHolder.getChildren().clear();
        save.setDisable(member == null);
        savePdf.setDisable(member == null);
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
        Label brand = new Label("NextFit");
        brand.getStyleClass().add("nf-carnet-brand");
        Label kind = new Label("CARNET DE SOCIO");
        kind.getStyleClass().add("nf-carnet-kind");
        ImageView logo = new ImageView(new Image(
                CarnetPane.class.getResource("/images/logo/nextfit-logo-128.png").toExternalForm()));
        logo.setFitWidth(42);
        logo.setFitHeight(42);
        HBox header = new HBox(12, logo, new VBox(2, brand, kind));
        header.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(member.getFullName());
        name.getStyleClass().add("nf-carnet-name");
        name.setWrapText(true);
        name.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        Label document = new Label("Documento · " + member.getDocumentNumber());
        document.getStyleClass().add("nf-carnet-text");
        document.setWrapText(true);
        VBox identity = new VBox(12, MemberAvatar.of(member, 84), name, document);
        HBox.setHgrow(identity, Priority.ALWAYS);

        ImageView qr = new ImageView(QrImage.of(qrAddress(member), 6));
        qr.setFitWidth(148);
        qr.setFitHeight(148);
        qr.setSmooth(false);
        qr.setPreserveRatio(true);
        StackPane qrSurface = new StackPane(qr);
        qrSurface.getStyleClass().add("nf-carnet-qr");
        Label scanHint = new Label("PORTAL DEL SOCIO");
        scanHint.getStyleClass().add("nf-carnet-kind");
        VBox qrBox = new VBox(8, qrSurface, scanHint);
        qrBox.setAlignment(Pos.CENTER);

        HBox body = new HBox(22, identity, qrBox);
        body.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(16, header, body);
        card.getStyleClass().add("nf-carnet");
        card.setPadding(new Insets(22));
        card.setPrefWidth(500);
        card.setMaxWidth(500);
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

    /**
     * Saves one or many carnets in a PDF to print and cut (four per A4 page, front and back).
     *
     * @param chosen the members whose carnets go in the file
     */
    private void exportPdf(List<Member> chosen) {
        if (chosen.isEmpty()) {
            FxDialogs.message(savePdf, "Sin socios", "No hay socios activos para generar carnets.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar carnets en PDF");
        chooser.setInitialFileName(chosen.size() == 1
                ? "carnet-" + chosen.get(0).getDocumentNumber() + ".pdf" : "carnets-socios.pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));
        File target = chooser.showSaveDialog(savePdf.getScene().getWindow());
        if (target == null) {
            return;
        }
        try {
            CarnetPdf.save(target.toPath(), chosen.stream().map(this::cardOf).toList());
            FxDialogs.message(savePdf, "Carnets guardados", "El PDF quedó en " + target.getAbsolutePath());
        } catch (PersistenceException e) {
            FxDialogs.error(savePdf, e);
        }
    }

    private String qrAddress(Member member) {
        return phoneUrl.get() + "/m/" + phone.tokenOf(member);
    }

    private CarnetPdf.Card cardOf(Member member) {
        String photo = member.getPhotoPath();
        return new CarnetPdf.Card(member.getFullName(), member.getDocumentNumber(),
                QrMatrix.of(qrAddress(member)),
                photo == null || photo.isBlank() ? null : Path.of(photo));
    }
}
