package com.rebecasarai.braillewriter.fragments;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ImageReader;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.rebecasarai.braillewriter.R;
import com.rebecasarai.braillewriter.classification.ObjectRecognition;
import com.rebecasarai.braillewriter.utils.ImageUtils;

import com.rebecasarai.braillewriter.classification.Classifier;
import com.rebecasarai.braillewriter.classification.ResultsView;
import com.rebecasarai.braillewriter.classification.TensorFlowImageClassifier;
import com.rebecasarai.braillewriter.ui.objectRecognition.OverlayView;

import java.util.List;
import java.util.Locale;
import java.util.Vector;

import timber.log.Timber;

/**
 * A simple {@link Fragment} subclass.
 */
public class ObjectRecognitionFragment extends CameraFragment implements ImageReader.OnImageAvailableListener, TextToSpeech.OnInitListener, View.OnClickListener {
    private final String TAG = this.getClass().getSimpleName();

    private List<ObjectRecognition> resultsAudio;


    // A TextToSpeech engine for speaking a String value.
    private TextToSpeech tts;
    private String toSpeak;
    protected static final boolean SAVE_PREVIEW_BITMAP = false;

    private ResultsView resultsView;

    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private long lastProcessingTimeMs;

    // These are the settings for the original v1 Inception model. If you want to
    // use a model that's been produced from the TensorFlow for Poets codelab,
    // you'll need to set IMAGE_SIZE = 299, IMAGE_MEAN = 128, IMAGE_STD = 128,
    // INPUT_NAME = "Mul", and OUTPUT_NAME = "final_result".
    // You'll also need to update the MODEL_FILE and LABEL_FILE paths to point to
    // the ones you produced.
    //
    // To use v3 Inception model, strip the DecodeJpeg Op from your retrained
    // model first:
    //
    // python strip_unused.py \
    // --input_graph=<retrained-pb-file> \
    // --output_graph=<your-stripped-pb-file> \
    // --input_node_names="Mul" \
    // --output_node_names="final_result" \
    // --input_binary=true
    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 117;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME = "input";
    private static final String OUTPUT_NAME = "output";


    private static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
    private static final String LABEL_FILE =
            "file:///android_asset/imagenet_comp_graph_label_strings.txt";


    private static final boolean MAINTAIN_ASPECT = false;

    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);


    private Integer sensorOrientation;
    private Classifier classifier;
    private Matrix frameToCropTransform;



    @Override
    public int getLayoutId() {
        return R.layout.camera_connection_fragment;
    }

    @Override
    public Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    private static final float TEXT_SIZE_DIP = 10;

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {

        toSpeak = "Ha entrado a reconocer";

        tts = new TextToSpeech(getContext(), this);


        final float textSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());

        classifier =
                TensorFlowImageClassifier.create(
                        getActivity().getAssets(),
                        MODEL_FILE,
                        LABEL_FILE,
                        INPUT_SIZE,
                        IMAGE_MEAN,
                        IMAGE_STD,
                        INPUT_NAME,
                        OUTPUT_NAME);

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        Timber.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        Timber.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);

        frameToCropTransform = ImageUtils.getTransformationMatrix(
                previewWidth, previewHeight,
                INPUT_SIZE, INPUT_SIZE,
                sensorOrientation, MAINTAIN_ASPECT);

        Matrix cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        renderDebug(canvas);
                    }
                });

        OverlayView screen_overlay = getRootview().findViewById(R.id.debug_overlay);


        screen_overlay.setOnClickListener(this);

    }


    @Override
    public void processImage() {
        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        final long startTime = SystemClock.uptimeMillis();
                        final List<ObjectRecognition> results = classifier.recognizeImage(croppedBitmap);
                        resultsAudio = results;
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                        Timber.v("Detect: %s", results);
                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        if (resultsView == null) {
                            resultsView = (ResultsView) getRootview().findViewById(R.id.results);
                        }
                        resultsView.setResults(results);
                        requestRender();
                        readyForNextImage();
                    }
                });
    }

    @Override
    public void onSetDebug(boolean debug) {
        classifier.enableStatLogging(debug);
    }

    private void renderDebug(final Canvas canvas) {
        if (!isDebug()) {
            return;
        }
        final Bitmap copy = cropCopyBitmap;
        if (copy != null) {
            final Matrix matrix = new Matrix();
            final float scaleFactor = 2;
            matrix.postScale(scaleFactor, scaleFactor);
            matrix.postTranslate(
                    canvas.getWidth() - copy.getWidth() * scaleFactor,
                    canvas.getHeight() - copy.getHeight() * scaleFactor);
            canvas.drawBitmap(copy, matrix, new Paint());

            final Vector<String> lines = new Vector<String>();
            if (classifier != null) {
                String statString = classifier.getStatString();
                String[] statLines = statString.split("\n");
                for (String line : statLines) {
                    lines.add(line);
                }
            }

            lines.add("Frame: " + previewWidth + "x" + previewHeight);
            lines.add("Crop: " + copy.getWidth() + "x" + copy.getHeight());
            lines.add("View: " + canvas.getWidth() + "x" + canvas.getHeight());
            lines.add("Rotation: " + sensorOrientation);
            lines.add("Inference time: " + lastProcessingTimeMs + "ms");

            //borderedText.drawLines(canvas, 10, canvas.getHeight() - 10, lines);
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

    public static ObjectRecognitionFragment newInstance() {
        ObjectRecognitionFragment fragment = new ObjectRecognitionFragment();
        return fragment;
    }

    @Override
    public void onClick(View v) {

        float max = 0;

        for(int i = 0; i < resultsAudio.size(); i++){
            Log.v("Hasta ahora:",resultsAudio.toString());
            final ObjectRecognition myRecognizedObject = (ObjectRecognition)resultsAudio.get(i);

            if(myRecognizedObject.getConfidence() > max){
                max = myRecognizedObject.getConfidence();
                toSpeak = myRecognizedObject.getTitle();

                // Write a message to the database
                final DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("objects");

                // References the right node from the database to run transaction
                mDatabase.child(myRecognizedObject.getTitle()).runTransaction(new Transaction.Handler() {

                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        ObjectRecognition objectRecognized = mutableData.getValue(ObjectRecognition.class);
                        if(objectRecognized == null){
                            // mutableData.setValue(objectRecognized);
                            mutableData.setValue(myRecognizedObject);
                            return Transaction.success(mutableData);
                        }

                        objectRecognized.setTimes(objectRecognized.getTimes()+1);
                        mutableData.setValue(objectRecognized);
                        return Transaction.success(mutableData);

                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                        Timber.d( "Transaction:onComplete:" + databaseError);
                    }
                });

            }
        }
        tts = new TextToSpeech(getActivity(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {

                if(i != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.getDefault());

                    //Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
                    tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        });
    }

    @Override
    public synchronized void onDestroy() {

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}