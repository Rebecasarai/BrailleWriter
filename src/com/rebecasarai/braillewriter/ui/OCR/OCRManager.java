package com.rebecasarai.braillewriter.ui.OCR;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.GestureDetector;
import android.view.ScaleGestureDetector;
import android.widget.Toast;

import com.google.android.gms.vision.text.TextRecognizer;
import com.rebecasarai.braillewriter.R;
import com.rebecasarai.braillewriter.classification.OcrDetectorProcessor;
import com.rebecasarai.braillewriter.ui.OCR.CameraSource;
import com.rebecasarai.braillewriter.ui.OCR.CameraSourcePreview;
import com.rebecasarai.braillewriter.ui.OCR.GraphicOverlay;
import com.rebecasarai.braillewriter.ui.OCR.OcrGraphic;

import timber.log.Timber;

public class OCRManager {

    private static final String TAG = "OCRManager";
    private Context context;
    private TextRecognizer mTextRecognizer;

    public OCRManager(Context context, GraphicOverlay<OcrGraphic> mGraphicOverlay) {
        this.context = context;
        // A text recognizer is created to find text.  An associated multi-processor instance
        // is set to receive the text recognition results, track the text, and maintain
        // graphics for each text block on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each text block.
        mTextRecognizer = new TextRecognizer.Builder(context).build();
        mTextRecognizer.setProcessor(new OcrDetectorProcessor(mGraphicOverlay));

    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the ocr detector to detect small text samples
     * at long distances.
     * <p>
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    public CameraSource createOCRCamera(boolean autoFocus, boolean useFlash) {
        //Context context = getContext();
        if (!mTextRecognizer.isOperational()) {
            // Note: The first time that an app using a Vision API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any text,
            // barcodes, or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Timber.w( "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = false;

            if (hasLowStorage) {
                Toast.makeText(context, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Timber.w( context.getString(R.string.low_storage_error));
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the text recognizer to detect small pieces of text.
        CameraSource cameraSource =
                new CameraSource.Builder(context, mTextRecognizer)
                        .setFacing(CameraSource.CAMERA_FACING_BACK)
                        .setRequestedPreviewSize(1280, 1024)
                        .setRequestedFps(2.0f)
                        .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                        .setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null)
                        .build();

        return cameraSource;
    }
}
