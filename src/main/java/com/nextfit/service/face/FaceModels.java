package com.nextfit.service.face;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * The two model files the recognition needs (a face detector and a face recognizer). They are not part
 * of the repository: each computer keeps them in the folder {@code data/models}.
 *
 * @param detector   the face detector file
 * @param recognizer the face recognizer file
 */
public record FaceModels(Path detector, Path recognizer) {

    public static final String DETECTOR_FILE = "face_detection_yunet_2023mar.onnx";
    public static final String RECOGNIZER_FILE = "face_recognition_sface_2021dec.onnx";

    /**
     * @param dataDir the data folder of NextFit
     * @return the models, or empty when any of the files is missing
     */
    public static Optional<FaceModels> locate(Path dataDir) {
        Path folder = dataDir.resolve("models");
        Path detector = folder.resolve(DETECTOR_FILE);
        Path recognizer = folder.resolve(RECOGNIZER_FILE);
        if (Files.isRegularFile(detector) && Files.isRegularFile(recognizer)) {
            return Optional.of(new FaceModels(detector, recognizer));
        }
        return Optional.empty();
    }

    /**
     * @param dataDir the data folder of NextFit
     * @return the text to show when the models are missing
     */
    public static String missingMessage(Path dataDir) {
        return "Faltan los modelos de reconocimiento. Copia " + DETECTOR_FILE + " y " + RECOGNIZER_FILE
                + " en la carpeta " + dataDir.resolve("models") + " (mira docs/face-recognition.md).";
    }
}
