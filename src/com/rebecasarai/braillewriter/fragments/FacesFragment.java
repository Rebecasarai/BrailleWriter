package com.rebecasarai.braillewriter.fragments;


import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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
import com.rebecasarai.braillewriter.MainViewModel;
import com.rebecasarai.braillewriter.R;
import com.rebecasarai.braillewriter.subscription.SubscriptionManagerProvider;
import com.rebecasarai.braillewriter.ui.face.CameraSourcePreview;
import com.rebecasarai.braillewriter.ui.face.FaceGraphic;
import com.rebecasarai.braillewriter.ui.face.GraphicOverlay;

import java.io.IOException;
import java.util.Locale;

import timber.log.Timber;


/**
 * A simple {@link Fragment} subclass.
 */
public class FacesFragment extends Fragment implements View.OnClickListener,TextToSpeech.OnInitListener {

    private static final String TAG = "FaceTracker";

    private CameraSource mCameraSource = null;

    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    // Instance
    private static FacesFragment INSTANCE = new FacesFragment();

    private static final int GMS_CODE = 9001;
    // permission request codes need to be < 256
    private static final int CAMERA_PERM_CODE = 2;
    private float mFaceHappiness;
    private Face mFace;
    private boolean mFaceDetected;

    private TextToSpeech tts;
    private String toSpeak ;
    private SubscriptionManagerProvider mSubscriptionProvider;


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
/*
        mSubscriptionProvider = (SubscriptionManagerProvider) getActivity();
        if(mSubscriptionProvider.getSubsV3Manager().isRecentlySuscribed()){
            toSpeak = "Felicidades, se ha suscrito exitosamente. Reconozca emociones";
            mSubscriptionProvider.getSubsV3Manager().setRecentlySuscribed(false);
        }*/



        MainViewModel model = ViewModelProviders.of(getActivity()).get(MainViewModel.class);
        //model.getIsRecentlySuscribed().observe();
        if(model.getIsRecentlySuscribed().getValue()){
            toSpeak = "Felicidades, se ha suscrito exitosamente. Reconozca emociones";
//            mSubscriptionProvider.getSubsV3Manager().setRecentlySuscribed(false);
            model.setIsRecentlySuscribed(true);
        }

        if( model.getmSameFragment().getValue()!= null && model.getmSameFragment().getValue()){
            toSpeak="";
            model.getmSameFragment().setValue(false);
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
        Timber.w( "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(getActivity(), permissions, CAMERA_PERM_CODE);
            return;
        }

        final FragmentActivity thisActivity = getActivity();
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        CAMERA_PERM_CODE);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
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
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != CAMERA_PERM_CODE) {
            Timber.d( "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Timber.d( "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Timber.e( "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Face Tracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
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
            Log.v("Onclik", " Recibe el onclick con sonrisa: " + mFaceHappiness);

            if(mFaceHappiness > 0.5){

                toSpeak = "Está alegre";
                Log.v("llego a feliz con:", ""+mFaceHappiness);

            }if(mFaceHappiness < 0.5 && mFaceHappiness > 0.1){

                toSpeak = "Esta serio";
                Log.v("llego a ", "serio");
            }if(mFaceHappiness< 0.1) {

                toSpeak = "Esta muy serio. Tal vez enojado.";

                Log.v("llego a ", "muy muy serio");
            }

        }

        tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);

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

            mFaceHappiness = face.getIsSmilingProbability();
            Log.v("Sonrie desde Update: ", ""+mFaceHappiness);

            /*mLeftEyeOpen = face.getIsLeftEyeOpenProbability();
            mRightEyeOpen = face.getIsRightEyeOpenProbability();

            Log.v("Ojo cerrado izq", ""+mLeftEyeOpen);
            Log.v("Ojo cerrado der", ""+mRightEyeOpen);
            Log.v("Face: ", face.toString());*/
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
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
