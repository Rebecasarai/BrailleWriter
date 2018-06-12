package com.rebecasarai.braillewriter.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
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
public class ReadFragment extends Fragment implements View.OnTouchListener {
    private static final String TAG = "OCRManager";

    // Intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    // Permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    // Constants used to pass extra data in the intent
    public static final String AutoFocus = "AutoFocus";
    public static final String UseFlash = "UseFlash";

    // Instance
    private static ReadFragment INSTANCE = new ReadFragment();

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<OcrGraphic> mGraphicOverlay;

    // Helper objects for detecting taps and pinches.
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    // A TextToSpeech engine for speaking a String value.
    private TextToSpeech tts;
    private TextToSpeech tts2;
    private String toSpeak;
    private OCRManager ocr;


    public ReadFragment() {
        // Required empty public constructor
    }

    public static ReadFragment getInstance() {
        if(INSTANCE==null){
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

        boolean autoFocus = true;
        boolean useFlash = false;

        ocr = new OCRManager(getContext(), mGraphicOverlay);

        // Set good defaults for capturing text.
        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            mCameraSource = ocr.createOCRCamera(autoFocus, useFlash);
        } else {
            requestCameraPermission();
        }

        toSpeak= "Ha entrado en leer";

        /*if(model.getmSeletedFragment().getValue().getClass().equals(this.getClass())){
            toSpeak = "";
        }*/

        /*if(model.getmSameFragment().getValue()){
            toSpeak="";
            model.setmSameFragment(false);
        }*/

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

        tts2= new TextToSpeech(getContext(),new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {

                if(i != TextToSpeech.ERROR) {
                    tts2.setLanguage(Locale.getDefault());

                    //Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
                    tts2.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        });

        //Tenia CaptureGestureListener, cambiado por OnSwipeTouchListener
        gestureDetector = new GestureDetector(getContext(), new CaptureGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

       /* Snackbar.make(mGraphicOverlay, "Toca para escuchar. Estira para zoom",
                Snackbar.LENGTH_LONG)
                .show();
*/

        // Set up the Text To Speech engine.
        TextToSpeech.OnInitListener listener =
                new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(final int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            Timber.d( "Text to speech engine started successfully.");
                            tts.setLanguage(Locale.getDefault());
                        } else {
                            Timber.d("Error starting the text to speech engine.");
                        }
                    }
                };
        tts = new TextToSpeech(this.getContext(), listener);

        rootview.setOnTouchListener(this);
        return rootview;
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
            ActivityCompat.requestPermissions(getActivity(), permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final FragmentActivity thisActivity = getActivity();

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
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
            Timber.d( "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Timber.d( "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            boolean autoFocus = getActivity().getIntent().getBooleanExtra(AutoFocus, false);
            boolean useFlash = getActivity().getIntent().getBooleanExtra(UseFlash, false);
            mCameraSource = ocr.createOCRCamera(autoFocus, useFlash);
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
        builder.setTitle("Multitracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
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
                Timber.e( "Unable to start camera source.");
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    /**
     * onTap is called to speak the tapped TextBlock, if any, out loud.
     * @return true if the tap was on a TextBlock
     */
    private boolean onTap() {
        Set<OcrGraphic> mGraphics = mGraphicOverlay.getmGraphics();
        String text = "";

        //if(mGraphics !=null){
            for (OcrGraphic g : mGraphics){
                text += g.getTextBlock().getValue();
            }
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "DEFAULT");
        //}

        return !text.equals("");


    }

    @Override
    public boolean onTouch(View v, MotionEvent e) {

        boolean b = scaleGestureDetector.onTouchEvent(e);

        boolean c = gestureDetector.onTouchEvent(e);

        return b || c;//|| super.onTouchEvent(e);
    }

    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {


        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return onTap() || super.onSingleTapConfirmed(e);
        }

        // Determines the fling velocity and then fires the appropriate swipe event accordingly
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                    }
                } else {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            onSwipeDown();
                        } else {
                            onSwipeUp();
                        }
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }


        public void onSwipeRight() {
        }

        public void onSwipeLeft() {
        }

        public void onSwipeUp() {
        }

        public void onSwipeDown() {
        }
    }




    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

        /**
         * Responds to scaling events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         * as handled. If an event was not handled, the detector
         * will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example,
         * only wants to update scaling factors if the change is
         * greater than 0.01.
         */
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        /**
         * Responds to the beginning of a scaling gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         * this gesture. For example, if a gesture is beginning
         * with a focal point outside of a region where it makes
         * sense, onScaleBegin() may return false to ignore the
         * rest of the gesture.
         */
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        /**
         * Responds to the end of a scale gesture. Reported by existing
         * pointers going up.
         * <p/>
         * Once a scale has ended, {@link ScaleGestureDetector#getFocusX()}
         * and {@link ScaleGestureDetector#getFocusY()} will return focal point
         * of the pointers remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         */
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (mCameraSource != null) {
                mCameraSource.doZoom(detector.getScaleFactor());
            }
        }
    }
}