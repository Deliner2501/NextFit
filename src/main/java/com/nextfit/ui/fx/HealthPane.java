package com.nextfit.ui.fx;

import com.nextfit.model.HealthProfile;
import com.nextfit.model.Member;
import com.nextfit.model.Measurement;
import com.nextfit.service.HealthService;
import com.nextfit.ui.Formats;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * The health tab of the member file: the BMI with its gauge, the last weight, the
 * height and the medical profile, which can be edited from a form. The calculations
 * and the rules live in {@link HealthService}.
 */
final class HealthPane extends ScrollPane {

    private static final List<String> BLOOD_TYPES = List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");

    private final Member member;
    private final HealthService health;
    private final KpiCard bmiCard = new KpiCard("IMC", "—", "", FxIcons.ANALYTICS, KpiCard.Kind.FEATURE);
    private final KpiCard weightCard = new KpiCard("Peso actual", "—", "", FxIcons.CALENDAR, KpiCard.Kind.VIOLET);
    private final KpiCard heightCard = new KpiCard("Estatura", "—", "", FxIcons.MEMBERS, KpiCard.Kind.LIME);
    private final BmiGauge gauge = new BmiGauge();
    private final Label conditions = new Label();
    private final Label allergies = new Label();

    /**
     * @param member the member whose health is shown
     * @param health the service that knows the profile and the measurements
     */
    HealthPane(Member member, HealthService health) {
        this.member = member;
        this.health = health;

        HBox kpis = new HBox(16, bmiCard, weightCard, heightCard);
        for (KpiCard card : List.of(bmiCard, weightCard, heightCard)) {
            card.setPrefWidth(1);
            card.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card, Priority.ALWAYS);
        }

        Label gaugeTitle = new Label("Índice de masa corporal");
        gaugeTitle.getStyleClass().add("nf-card-title");
        VBox gaugeCard = new VBox(14, gaugeTitle, gauge);
        gaugeCard.getStyleClass().add("nf-card");
        gaugeCard.setPadding(new Insets(20));

        Label profileTitle = new Label("Perfil médico");
        profileTitle.getStyleClass().add("nf-card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button edit = new Button("Editar perfil");
        edit.getStyleClass().add("nf-secondary");
        edit.setMinWidth(Region.USE_PREF_SIZE);
        edit.setOnAction(e -> openForm());
        HBox profileHeader = new HBox(10, profileTitle, spacer, edit);
        profileHeader.setAlignment(Pos.CENTER_LEFT);
        conditions.setWrapText(true);
        allergies.setWrapText(true);
        VBox profileCard = new VBox(12, profileHeader,
                FxDialogs.labeled("Condiciones médicas", conditions),
                FxDialogs.labeled("Alergias", allergies));
        profileCard.getStyleClass().add("nf-card");
        profileCard.setPadding(new Insets(20));

        VBox content = new VBox(16, kpis, gaugeCard, profileCard);
        content.setPadding(new Insets(16, 4, 16, 0));
        setContent(content);
        setFitToWidth(true);
        getStyleClass().add("nf-scroll");
        refresh();
    }

    /** Reads the profile and the measurements again and updates every card. */
    void refresh() {
        long id = member.getId();
        Optional<HealthProfile> profile = health.findProfile(id);
        Optional<Measurement> latest = health.latestMeasurement(id);
        Optional<Double> bmi = health.currentBmi(id);

        bmiCard.value().setText(bmi.map(Decimals::text).orElse("—"));
        bmiCard.hint().setText(bmi.map(b -> health.classify(b).getLabel())
                .orElse("Falta la estatura o una medición"));
        weightCard.value().setText(latest.map(m -> Decimals.text(m.getWeightKg()) + " kg").orElse("—"));
        weightCard.hint().setText(latest.map(m -> "Medido el " + Formats.date(m.getDate()))
                .orElse("Sin mediciones todavía"));
        heightCard.value().setText(profile.map(p -> Decimals.text(p.getHeightCm()) + " cm").orElse("—"));
        heightCard.hint().setText(profile.map(p -> "Sangre " + p.getBloodType())
                .orElse("Sin perfil de salud todavía"));
        gauge.setBmi(bmi.orElse(null));
        conditions.setText(profile.map(HealthProfile::getMedicalConditions)
                .filter(text -> !text.isBlank()).orElse("Ninguna registrada"));
        allergies.setText(profile.map(HealthProfile::getAllergies)
                .filter(text -> !text.isBlank()).orElse("Ninguna registrada"));
    }

    // Opens the form to create or edit the medical profile of the member.
    private void openForm() {
        Optional<HealthProfile> current = health.findProfile(member.getId());
        TextField heightField = new TextField(current.map(p -> Decimals.text(p.getHeightCm())).orElse(""));
        heightField.getStyleClass().add("nf-input");
        heightField.setPromptText("Ej. 175");
        heightField.setPrefHeight(44);
        ComboBox<String> bloodBox = new ComboBox<>();
        bloodBox.getItems().addAll(BLOOD_TYPES);
        bloodBox.setPromptText("Elegir");
        bloodBox.setMaxWidth(Double.MAX_VALUE);
        current.ifPresent(p -> bloodBox.setValue(p.getBloodType()));
        TextArea conditionsArea = area(current.map(HealthProfile::getMedicalConditions).orElse(""), 3);
        TextArea allergiesArea = area(current.map(HealthProfile::getAllergies).orElse(""), 2);

        VBox fields = new VBox(12,
                FxDialogs.labeled("Estatura (cm)", heightField),
                FxDialogs.labeled("Tipo de sangre", bloodBox),
                FxDialogs.labeled("Condiciones médicas (opcional)", conditionsArea),
                FxDialogs.labeled("Alergias (opcional)", allergiesArea));
        fields.setPrefWidth(440);
        FxDialogs.form(this, "Perfil médico de " + member.getFullName(), "Guardar", fields, () -> {
            double height = Decimals.parse("estatura", heightField.getText());
            health.saveProfile(new HealthProfile(member.getId(), height, bloodBox.getValue(),
                    conditionsArea.getText().trim(), allergiesArea.getText().trim()));
            refresh();
        });
    }

    private static TextArea area(String text, int rows) {
        TextArea area = new TextArea(text == null ? "" : text);
        area.getStyleClass().add("nf-textarea");
        area.setPrefRowCount(rows);
        area.setWrapText(true);
        return area;
    }
}
