package com.rebecasarai.braillewriter.ui.face;

import android.util.Log;

import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;


/**
 * Face tracker for each detected individual. This maintains a face graphic within the app's
 * associated face overlay.
 */
public class GraphicFaceTrackerProcessor extends Tracker<Face> {
    private GraphicOverlay mOverlay;
    private FaceGraphic mFaceGraphic;
    private Face mFace;
    private boolean mFaceDetected;
    private float mFaceHappiness;

    public GraphicFaceTrackerProcessor(GraphicOverlay overlay) {
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

    public GraphicOverlay getmOverlay() {
        return mOverlay;
    }

    public void setmOverlay(GraphicOverlay mOverlay) {
        this.mOverlay = mOverlay;
    }

    public FaceGraphic getmFaceGraphic() {
        return mFaceGraphic;
    }

    public void setmFaceGraphic(FaceGraphic mFaceGraphic) {
        this.mFaceGraphic = mFaceGraphic;
    }

    public Face getmFace() {
        return mFace;
    }

    public void setmFace(Face mFace) {
        this.mFace = mFace;
    }

    public boolean ismFaceDetected() {
        return mFaceDetected;
    }

    public void setmFaceDetected(boolean mFaceDetected) {
        this.mFaceDetected = mFaceDetected;
    }

    public float getmFaceHappiness() {
        return mFaceHappiness;
    }

    public void setmFaceHappiness(float mFaceHappiness) {
        this.mFaceHappiness = mFaceHappiness;
    }
}

