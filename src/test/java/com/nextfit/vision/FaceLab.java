package com.nextfit.vision;

import com.nextfit.service.face.FaceCameraException;
import com.nextfit.service.face.FaceFrame;
import com.nextfit.service.face.FaceMatcher;
import com.nextfit.service.face.FaceModels;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manual check of the camera and the models (not an automatic test, it needs a camera).
 * Run its main method from the IDE with the project folder as working directory and follow the
 * three steps printed on the console. It saves facelab.jpg with the last picture.
 */
public final class FaceLab {

    private static final long STEP_MILLIS = 8_000;
    /** Seconds to get in position before the third step starts measuring. */
    private static final long GRACE_MILLIS = 5_000;

    private FaceLab() {
    }

    public static void main(String[] args) throws Exception {
        Path data = Path.of("data");
        Optional<FaceModels> models = FaceModels.locate(data);
        if (models.isEmpty()) {
            System.out.println("FALLA: " + FaceModels.missingMessage(data));
            return;
        }
        System.out.println("Modelos encontrados. Abriendo la camara...");
        try (OpenCvFaceCamera camera = OpenCvFaceCamera.open(models.get(), 0)) {
            System.out.println("Camara abierta.");
            System.out.println();
            System.out.println("PASO 1 (8 s): mira de frente a la camara, quieto, con movimientos pequenos.");
            Step front = run(camera, null);
            float[] reference = front.average();
            System.out.println("PASO 2 (8 s): gira la cabeza despacio a la izquierda y a la derecha.");
            Step turn = run(camera, reference);
            System.out.println("PASO 3: OPCIONAL. Tienes 5 s para SALIR de la imagen y que OTRA persona se ponga frente a la camara (solo ella en la imagen). Luego se miden 8 s.");
            waitFor(camera, GRACE_MILLIS);
            System.out.println("   >>> midiendo ahora, no te pongas en la imagen <<<");
            Step other = run(camera, reference);

            System.out.println();
            System.out.println("================ RESULTADO ================");
            System.out.printf("Valores por rostro: %d (debe ser 128)%n", reference == null ? 0 : reference.length);
            System.out.printf("Paso 1 (de frente): %d cuadros, %.1f por segundo, rostro en %d%n",
                    front.frames, front.frames * 1000.0 / STEP_MILLIS, front.withFace);
            System.out.printf("   parecido con tu rostro promedio: minimo %.2f, promedio %.2f (esperado: mayor que 0.6)%n",
                    front.minSim, front.avgSim());
            System.out.printf("Paso 2 (girando): giro de %.2f a %.2f (debe pasar de -0.20 a 0.20)%n", turn.minYaw, turn.maxYaw);
            System.out.printf("   cuadros casi de frente: %d, parecido minimo %.2f, promedio %.2f%n",
                    turn.frontalCount, turn.minFrontalSim, turn.avgFrontalSim());
            if (other.withFace > 0) {
                System.out.printf("Paso 3 (otra persona): rostro en %d cuadros, casi de frente %d%n", other.withFace, other.frontalCount);
                System.out.printf("   parecido con tu rostro: mediana %.2f, maximo %.2f; cuadros con parecido de 0.45 o mas: %d (esperado: mediana menor que 0.35 y 0 cuadros)%n",
                        other.median(), other.maxFrontalSim, other.above(0.45));
            } else {
                System.out.println("Paso 3 (otra persona): sin rostro, omitido");
            }
            System.out.println("Se guardo la ultima imagen en facelab.jpg");
        } catch (FaceCameraException e) {
            System.out.println("FALLA: " + e.getMessage());
        }
    }

    /** What happened during one step. */
    private static final class Step {
        int frames;
        int withFace;
        double minYaw;
        double maxYaw;
        double minSim = 1;
        double sumSim;
        int simCount;
        int frontalCount;
        double minFrontalSim = 1;
        double sumFrontalSim;
        double maxFrontalSim = -1;
        final List<float[]> frontalVectors = new ArrayList<>();
        final List<Double> frontalSims = new ArrayList<>();

        double median() {
            if (frontalSims.isEmpty()) {
                return 0;
            }
            List<Double> sorted = new ArrayList<>(frontalSims);
            java.util.Collections.sort(sorted);
            return sorted.get(sorted.size() / 2);
        }

        int above(double limit) {
            int count = 0;
            for (double d : frontalSims) {
                if (d >= limit) {
                    count++;
                }
            }
            return count;
        }

        double avgSim() {
            return simCount == 0 ? 0 : sumSim / simCount;
        }

        double avgFrontalSim() {
            return frontalCount == 0 ? 0 : sumFrontalSim / frontalCount;
        }

        /** The average of the vectors of the frontal frames, used as the reference face. */
        float[] average() {
            if (frontalVectors.isEmpty()) {
                return null;
            }
            float[] sum = new float[frontalVectors.get(0).length];
            for (float[] v : frontalVectors) {
                for (int i = 0; i < sum.length; i++) {
                    sum[i] += v[i];
                }
            }
            return sum;
        }
    }

    /** Keeps reading the camera without measuring, so the person has time to get in position. */
    private static void waitFor(OpenCvFaceCamera camera, long millis) {
        long end = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < end) {
            camera.next();
        }
    }

    private static Step run(OpenCvFaceCamera camera, float[] reference) throws Exception {
        Step step = new Step();
        long end = System.currentTimeMillis() + STEP_MILLIS;
        while (System.currentTimeMillis() < end) {
            FaceFrame frame = camera.next();
            step.frames++;
            Files.write(Path.of("facelab.jpg"), frame.preview());
            if (!frame.hasFace()) {
                continue;
            }
            step.withFace++;
            step.minYaw = Math.min(step.minYaw, frame.yaw());
            step.maxYaw = Math.max(step.maxYaw, frame.yaw());
            if (frame.isFrontal()) {
                step.frontalVectors.add(frame.vector());
            }
            if (reference != null) {
                double sim = FaceMatcher.cosine(reference, frame.vector());
                if (frame.isFrontal()) {
                    step.frontalCount++;
                    step.frontalSims.add(sim);
                    step.sumFrontalSim += sim;
                    step.minFrontalSim = Math.min(step.minFrontalSim, sim);
                    step.maxFrontalSim = Math.max(step.maxFrontalSim, sim);
                }
            }
        }
        if (reference == null && !step.frontalVectors.isEmpty()) {
            float[] avg = step.average();
            for (float[] v : step.frontalVectors) {
                double sim = FaceMatcher.cosine(avg, v);
                step.minSim = Math.min(step.minSim, sim);
                step.sumSim += sim;
                step.simCount++;
            }
        }
        return step;
    }
}
