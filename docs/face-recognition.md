# Face recognition

Members can enter by looking at the camera of the reception laptop. The QR and the document keep working as an alternative.

## How it works

1. **Registration** (member file, with the member's consent): three pictures are taken; only a vector of 128 numbers per picture is stored, never the picture.
2. **Recognition**: OpenCV finds the largest face (YuNet) and turns it into 128 numbers (SFace). The numbers are compared with the stored ones by cosine similarity.
3. **Safety checks** before opening: the similarity must pass a threshold (0.45), the best member must beat the second one by a margin (0.05), the same member must appear in 3 frames in a row, and after a confirmation the member is ignored for 8 seconds so standing in front of the camera does not enter and leave again.
4. The usual access rules decide: active membership, capacity, and so on (`AccessService`).

## Layers

| Layer | Class | Role |
|---|---|---|
| model | `FaceTemplate` | The numbers of one face |
| persistence | `FaceTemplateRepository`, `CsvFaceTemplateRepository` | Storage (`data/face_templates.csv`); a database implementation can replace it |
| service | `FaceMatcher`, `FaceStabilizer`, `HeadTurnChallenge` | Pure rules, fully unit tested |
| service | `FaceEnrollmentService`, `FaceAccessService` | Registration and access, with audit |
| service | `FaceCamera`, `FaceFrame`, `FaceModels` | What the rest of the application knows about the camera |
| vision | `OpenCvFaceCamera` | The only class that knows OpenCV |

## Model files

The two model files are not in the repository (about 37 MB). Each computer that uses the camera needs them in `data/models/`:

- `face_detection_yunet_2023mar.onnx`
- `face_recognition_sface_2021dec.onnx`

Both come from the [OpenCV model zoo](https://github.com/opencv/opencv_zoo): open `models/face_detection_yunet` and `models/face_recognition_sface`, open each file and use "Download raw file".

## Checking the camera

Run `com.nextfit.vision.FaceLab` (its `main` method) from the IDE with the project folder as working directory. It reads the camera for 20 seconds and prints the frames per second, the number of values per face (128), the head turn range and the similarity between frames of the same person.

## Privacy

The face is sensitive personal data (Ley 1581 de 2012, Colombia):

- It is stored only after the member accepts it on screen.
- It can be deleted from the member file at any time, and it is deleted automatically when the member is deactivated.
- Registration and deletion are written to the audit log (`FACE_ENROLLED`, `FACE_REMOVED`).
- No picture is stored, only numbers, and nothing leaves the computer.

## Known limit

A printed photo could fool the recognizer. The registration and the entry screen ask the person to turn the head to both sides (`HeadTurnChallenge`), which a still photo cannot do. A video recording could still pass: this is declared as future work.
