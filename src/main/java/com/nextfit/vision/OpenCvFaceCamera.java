package com.nextfit.vision;

import com.nextfit.service.face.FaceCamera;
import com.nextfit.service.face.FaceCameraException;
import com.nextfit.service.face.FaceFrame;
import com.nextfit.service.face.FaceModels;
import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.objdetect.FaceRecognizerSF;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.util.Locale;

/**
 * Camera that uses OpenCV: YuNet finds the faces and SFace turns the largest one into 128 numbers.
 * It works offline. This is the only class that knows OpenCV; the rest of NextFit sees {@link FaceCamera}.
 */
public final class OpenCvFaceCamera implements FaceCamera {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;
    private static final float MIN_SCORE = 0.9f;
    /** Faces narrower than this (pixels) are too far or too small to recognize well. */
    private static final double MIN_FACE_WIDTH = 90;
    private static final Scalar GREEN = new Scalar(80, 220, 120);

    private static boolean loaded;

    private final VideoCapture capture;
    private final FaceDetectorYN detector;
    private final FaceRecognizerSF recognizer;

    private OpenCvFaceCamera(VideoCapture capture, FaceDetectorYN detector, FaceRecognizerSF recognizer) {
        this.capture = capture;
        this.detector = detector;
        this.recognizer = recognizer;
    }

    /**
     * Turns the camera on.
     *
     * @param models      the model files
     * @param cameraIndex the camera to use (0 is the first one, usually the laptop camera)
     * @return the camera, ready to take pictures
     * @throws FaceCameraException when OpenCV, the models or the camera cannot be used
     */
    public static OpenCvFaceCamera open(FaceModels models, int cameraIndex) {
        loadLibrary();
        FaceDetectorYN detector;
        FaceRecognizerSF recognizer;
        try {
            detector = FaceDetectorYN.create(models.detector().toString(), "", new Size(WIDTH, HEIGHT),
                    MIN_SCORE, 0.3f, 5000);
            recognizer = FaceRecognizerSF.create(models.recognizer().toString(), "");
        } catch (RuntimeException e) {
            throw new FaceCameraException("No se pudieron cargar los modelos de reconocimiento: " + e.getMessage(), e);
        }
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        VideoCapture capture = windows ? new VideoCapture(cameraIndex, Videoio.CAP_DSHOW) : new VideoCapture(cameraIndex);
        if (!capture.isOpened()) {
            capture.release();
            throw new FaceCameraException("No se pudo abrir la cámara. Cierra las aplicaciones que la usen "
                    + "(Zoom, Teams, Cámara) y revisa el permiso de cámara de Windows.");
        }
        capture.set(Videoio.CAP_PROP_FRAME_WIDTH, WIDTH);
        capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, HEIGHT);
        return new OpenCvFaceCamera(capture, detector, recognizer);
    }

    private static synchronized void loadLibrary() {
        if (loaded) {
            return;
        }
        try {
            OpenCV.loadLocally();
            loaded = true;
        } catch (Throwable e) {
            throw new FaceCameraException("No se pudo cargar OpenCV: " + e.getMessage(), e);
        }
    }

    @Override
    public FaceFrame next() {
        Mat frame = new Mat();
        Mat faces = new Mat();
        Mat aligned = new Mat();
        Mat feature = new Mat();
        MatOfByte jpeg = new MatOfByte();
        try {
            if (!capture.read(frame) || frame.empty()) {
                throw new FaceCameraException("No se pudo leer la imagen de la cámara.");
            }
            Core.flip(frame, frame, 1); // mirror, like a mirror: easier to place yourself
            detector.setInputSize(new Size(frame.cols(), frame.rows()));
            detector.detect(frame, faces);

            int best = -1;
            double bestArea = 0;
            float[] row = new float[15];
            for (int i = 0; i < faces.rows(); i++) {
                faces.get(i, 0, row);
                double area = (double) row[2] * row[3];
                if (row[14] >= MIN_SCORE && row[2] >= MIN_FACE_WIDTH && area > bestArea) {
                    best = i;
                    bestArea = area;
                }
            }

            float[] vector = null;
            double yaw = 0;
            if (best >= 0) {
                faces.get(best, 0, row);
                recognizer.alignCrop(frame, faces.row(best), aligned);
                recognizer.feature(aligned, feature);
                vector = new float[(int) feature.total()];
                feature.get(0, 0, vector);
                double eyes = Math.max(1.0, Math.abs(row[6] - row[4]));
                yaw = (row[8] - (row[4] + row[6]) / 2.0) / eyes;
                Imgproc.rectangle(frame, new Point(row[0], row[1]), new Point(row[0] + row[2], row[1] + row[3]), GREEN, 2);
            }
            Imgcodecs.imencode(".jpg", frame, jpeg);
            return new FaceFrame(jpeg.toArray(), vector, yaw);
        } finally {
            frame.release();
            faces.release();
            aligned.release();
            feature.release();
            jpeg.release();
        }
    }

    @Override
    public void close() {
        capture.release();
    }
}
