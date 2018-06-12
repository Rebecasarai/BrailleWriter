package com.rebecasarai.braillewriter.fragments;

import android.Manifest;
import android.app.Dialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.rebecasarai.braillewriter.MainViewModel;
import com.rebecasarai.braillewriter.R;
import com.rebecasarai.braillewriter.ui.OCR.CameraSource;
import com.rebecasarai.braillewriter.ui.OCR.CameraSourcePreview;
import com.rebecasarai.braillewriter.ui.OCR.GraphicOverlay;
import com.rebecasarai.braillewriter.ui.OCR.OCRManager;
import com.rebecasarai.braillewriter.ui.OCR.OcrGraphic;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import timber.log.Timber;


/**
 * A simple {@link Fragment} subclass.
 */
public class ReadFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "OCRManager";

    // Intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    // Permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private boolean mAutoFocus = true;
    private boolean mUseFlash = false;

    // Instance
    private static ReadFragment INSTANCE = new ReadFragment();

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<OcrGraphic> mGraphicOverlay;

    // A TextToSpeech engine for speaking a String value.
    private TextToSpeech tts;
    private TextToSpeech tts2;
    private String toSpeak;
    private OCRManager ocr;


    public ReadFragment() {
        // Required empty public constructor
    }

    public static ReadFragment getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ReadFragment();
        }
        return INSTANCE;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootview = inflater.inflate(R.layout.fragment_read, container, false);

        MainViewModel model = ViewModelProviders.of(getActivity()).get(MainViewModel.class);

        mPreview = (CameraSourcePreview) rootview.findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay<OcrGraphic>) rootview.findViewById(R.id.graphicOverlay);

        ocr = new OCRManager(getContext(), mGraphicOverlay);

        toSpeak = "Ha entrado en leer";

        model.getmSameFragment().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                Timber.e("ENTRO SAME FRAGMENT");
            }
        });
        model.getmSeletedFragment().observe(this, new Observer<Fragment>() {
            @Override
            public void onChanged(@Nullable Fragment fragment) {
                Timber.e("ENTRO Selected fragment");
            }
        });

        tts2 = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {

                if (i != TextToSpeech.ERROR) {
                    tts2.setLanguage(Locale.getDefault());

                    //Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
                    tts2.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        });

        // Set up the Text To Speech engine.
        TextToSpeech.OnInitListener listener =
                new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(final int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            Timber.d("Text to speech engine started successfully.");
                            tts.setLanguage(Locale.getDefault());
                        } else {
                            Timber.d("Error starting the text to speech engine.");
                        }
                    }
                };
        tts = new TextToSpeech(this.getContext(), listener);

        rootview.setOnClickListener(this);
        return rootview;
    }


    /**
     * Restarts the camera.
     */
    @Override
    public void onResume() {
        super.onResume();
        requestCameraPermission();
    }

    /**
     * Stops the camera.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    public void onDestroy() {


        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        if (mPreview != null) {
            mPreview.release();
        }
        super.onDestroy();
    }


    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCameraSource();
        } else {
            final String[] permissions = new String[]{Manifest.permission.CAMERA};
            ActivityCompat.requestPermissions(getActivity(), permissions, RC_HANDLE_CAMERA_PERM);
        }
    }


    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Timber.d("Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Timber.d("Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            startCameraSource();
            return;
        }

        Timber.e("Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        requestCameraPermission();


        /*DialogInterface.OnClickListener okayListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                requestCameraPermission();
            }
        };

        // TODO: add appropiate messages.
        // This will show if permission is not granted
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Permisos")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, okayListener)
                .show();*/
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        mCameraSource = ocr.createOCRCamera(mAutoFocus, mUseFlash);

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Timber.e("Unable to start camera source.");
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    @Override
    public void onClick(View v) {
        Set<OcrGraphic> mGraphics = mGraphicOverlay.getmGraphics();
        String text = "";

        for (OcrGraphic g : mGraphics) {
            text += g.getTextBlock().getValue();
        }
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "DEFAULT");
    }
}