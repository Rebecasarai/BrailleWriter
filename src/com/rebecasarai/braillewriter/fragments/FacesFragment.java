package com.rebecasarai.braillewriter.fragments;


import android.Manifest;
import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.rebecasarai.braillewriter.viewmodel.StateViewModel;
import com.rebecasarai.braillewriter.viewmodel.SubscriptionsMainViewModel;
import com.rebecasarai.braillewriter.R;
import com.rebecasarai.braillewriter.ui.face.CameraSourcePreview;
import com.rebecasarai.braillewriter.ui.face.FaceGraphic;
import com.rebecasarai.braillewriter.ui.face.GraphicOverlay;

import java.io.IOException;
import java.util.Locale;

import timber.log.Timber;


/**
 * Fragment where the Face Detection is handleled.
 */
public class FacesFragment extends Fragment implements View.OnClickListener,TextToSpeech.OnInitListener {

    private static final String TAG = "FaceTracker";
    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    // Instance
    private static FacesFragment INSTANCE = new FacesFragment();
    private static final int GMS_CODE = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private float mFaceHappiness;
    private Face mFace;
    private boolean mFaceDetected;

    private TextToSpeech tts;
    private String toSpeak ;


    public FacesFragment() {
        // Required empty public constructor
    }

    public static FacesFragment getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FacesFragment();
        }
        return INSTANCE;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootview = inflater.inflate(R.layout.fragment_faces, container, false);
        mFaceDetected = false;
        findViews(rootview);
        toSpeak = "Ha entrado a Reconocimiento de rostros";

        StateViewModel stateVM = ViewModelProviders.of(getActivity()).get(StateViewModel.class);
        SubscriptionsMainViewModel subsVM = ViewModelProviders.of(getActivity()).get(SubscriptionsMainViewModel.class);
        if(subsVM.getIsRecentlySuscribed().getValue()){
            toSpeak = "Felicidades, se ha suscrito exitosamente. Reconozca emociones";
            subsVM.setIsRecentlySuscribed(true);
        }

        if(stateVM.getmSameFragment().getValue() ){
            toSpeak ="";
        }
        tts = new TextToSpeech(getContext(), this);


        int rc = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }

        mPreview.setOnClickListener(this);

        return rootview;
    }

    /**
     * Finds the views for the layout
     * @param rootview
     */
    private void findViews(View rootview){
        mPreview = (CameraSourcePreview) rootview.findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) rootview.findViewById(R.id.faceOverlay);
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
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
    }


    /**
     * Creates and starts the camera to later build the detector. On higher resolution
     */
    private void createCameraSource() {
        Context context = getContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Timber.w( "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(30.0f)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    public void onResume() {
        super.onResume();

        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    public void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    public void onDestroy() {

        if (mCameraSource != null) {
            mCameraSource.release();
        }

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }


    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), code, GMS_CODE);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Timber.e( "Unable to start camera source.");
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    @Override
    public void onClick(View v) {

        // If at least a face is currently detected
        if (mFace != null && mFaceDetected){
            Timber.v(" Recibe el onclick con sonrisa: %s", mFaceHappiness);

            if(mFaceHappiness > 0.5){

                toSpeak = "Está alegre";
                Timber.v("llego a feliz con: %s", mFaceHappiness);

            }if(mFaceHappiness < 0.5 && mFaceHappiness > 0.1){

                toSpeak = "Esta serio";
                Timber.v("llego a %s", "serio");
            }if(mFaceHappiness< 0.1) {

                toSpeak = "Esta muy serio. Tal vez enojado.";
                Timber.v("llego a %s", "muy muy serio");
            }

            tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
        }

    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.getDefault());

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Timber.e( "This Language is not supported");
            } else {
                tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                toSpeak = "";
            }

        } else {
            Timber.e( "Initilization Failed!");
        }

    }



    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);

        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;


        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
            mFaceDetected = false;
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFace = item;
            mFaceDetected = true;
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
            mFaceDetected = true;
            mFaceHappiness = face.getIsSmilingProbability();
            Log.v("Sonrie desde Update: ", ""+mFaceHappiness);

        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
            mFaceDetected = false;
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mFaceDetected = false;
            mOverlay.remove(mFaceGraphic);
        }
    }
}
