package com.rebecasarai.braillewriter.fragments;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.support.v4.app.Fragment;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.rebecasarai.braillewriter.R;
import com.rebecasarai.braillewriter.utils.ImageUtils;
import com.rebecasarai.braillewriter.ui.objectRecognition.OverlayView;

import java.nio.ByteBuffer;

import timber.log.Timber;


/**
 * A simple {@link Fragment} subclass.
 */
public abstract class CameraFragment extends Fragment
        implements ImageReader.OnImageAvailableListener, Camera.PreviewCallback {

    private static final int PERMISSIONS_REQUEST = 1;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private boolean debug = false;

    private Handler handler;
    private HandlerThread handlerThread;
    private boolean useCamera2API;
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;

    protected int previewWidth = 0;
    protected int previewHeight = 0;

    private Runnable postInferenceCallback;
    private Runnable imageConverter;

    public View rootview;

    public CameraFragment() {
        // Required empty public constructor
    }

    public View getRootview() {
        return rootview;
    }

    public void setRootview(View rootview) {
        this.rootview = rootview;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootview =  inflater.inflate(R.layout.fragment_camera, container, false);

        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }
        return rootview;

    }


        private byte[] lastPreviewFrame;

        protected int[] getRgbBytes() {
                imageConverter.run();
                return rgbBytes;
        }

        protected int getLuminanceStride() {
                return yRowStride;
        }

        protected byte[] getLuminance() {
                return yuvBytes[0];
        }

        /**
         * Callback for android.hardware.Camera API
         */
        @Override
        public void onPreviewFrame(final byte[] bytes, final Camera camera) {
                if (isProcessingFrame) {
                        Timber.w("Dropping frame!");
                        return;
                }

                try {
                        // Initialize the storage bitmaps once when the resolution is known.
                        if (rgbBytes == null) {
                                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                                previewHeight = previewSize.height;
                                previewWidth = previewSize.width;
                                rgbBytes = new int[previewWidth * previewHeight];
                                onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
                        }
                } catch (final Exception e) {
                        Timber.e(e, "Exception!");
                        return;
                }

                isProcessingFrame = true;
                lastPreviewFrame = bytes;
                yuvBytes[0] = bytes;
                yRowStride = previewWidth;

                imageConverter =
                        new Runnable() {
                                @Override
                                public void run() {
                                        ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
                                }
                        };

                postInferenceCallback =
                        new Runnable() {
                                @Override
                                public void run() {
                                        camera.addCallbackBuffer(bytes);
                                        isProcessingFrame = false;
                                }
                        };
                processImage();
        }

        /**
         * Callback for Camera2 API
         */
        @Override
        public void onImageAvailable(final ImageReader reader) {
                //We need wait until we have some size from onPreviewSizeChosen
                if (previewWidth == 0 || previewHeight == 0) {
                        return;
                }
                if (rgbBytes == null) {
                        rgbBytes = new int[previewWidth * previewHeight];
                }
                try {
                        final Image image = reader.acquireLatestImage();

                        if (image == null) {
                                return;
                        }

                        if (isProcessingFrame) {
                                image.close();
                                return;
                        }
                        isProcessingFrame = true;
                        Trace.beginSection("imageAvailable");
                        final Plane [] planes = image.getPlanes();
                        fillBytes(planes, yuvBytes);
                        yRowStride = planes[0].getRowStride();
                        final int uvRowStride = planes[1].getRowStride();
                        final int uvPixelStride = planes[1].getPixelStride();

                        imageConverter =
                                new Runnable() {
                                        @Override
                                        public void run() {
                                                ImageUtils.convertYUV420ToARGB8888(
                                                        yuvBytes[0],
                                                        yuvBytes[1],
                                                        yuvBytes[2],
                                                        previewWidth,
                                                        previewHeight,
                                                        yRowStride,
                                                        uvRowStride,
                                                        uvPixelStride,
                                                        rgbBytes);
                                        }
                                };

                        postInferenceCallback =
                                new Runnable() {
                                        @Override
                                        public void run() {
                                                image.close();
                                                isProcessingFrame = false;
                                        }
                                };

                        processImage();
                } catch (final Exception e) {
                        Timber.e(e, "Exception!");
                        Trace.endSection();
                        return;
                }
                Trace.endSection();
        }

        @Override
        public synchronized void onStart() {
                Timber.d("onStart " + this);
                super.onStart();
        }

        @Override
        public synchronized void onResume() {
                Timber.d("onResume " + this);
                super.onResume();

                handlerThread = new HandlerThread("inference");
                handlerThread.start();
                handler = new Handler(handlerThread.getLooper());
        }

        @Override
        public synchronized void onPause() {
                Timber.d("onPause " + this);

                handlerThread.quitSafely();
                try {
                        handlerThread.join();
                        handlerThread = null;
                        handler = null;
                } catch (final InterruptedException e) {
                        Timber.e(e, "Exception!");
                }

                super.onPause();
        }

        @Override
        public synchronized void onStop() {
                Timber.d("onStop " + this);
                super.onStop();
        }

        @Override
        public synchronized void onDestroy() {
                Timber.d("onDestroy " + this);
                super.onDestroy();
        }

        protected synchronized void runInBackground(final Runnable r) {
                if (handler != null) {
                        handler.post(r);
                }
        }

        @Override
        public void onRequestPermissionsResult(
                final int requestCode, final String[] permissions, final int[] grantResults) {
                if (requestCode == PERMISSIONS_REQUEST) {
                        if (grantResults.length > 0
                                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                                setFragment();
                        } else {
                                requestPermission();
                        }
                }
        }

        private boolean hasPermission() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        return getActivity().checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED &&
                                getActivity().checkSelfPermission(PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED;
                } else {
                        return true;
                }
        }

        private void requestPermission() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA) ||
                                shouldShowRequestPermissionRationale(PERMISSION_STORAGE)) {
                                Toast.makeText(getContext(), "Camera AND storage permission are required for this demo", Toast.LENGTH_LONG).show();
                        }
                        requestPermissions(new String[] {PERMISSION_CAMERA, PERMISSION_STORAGE}, PERMISSIONS_REQUEST);
                }
        }

        // Returns true if the device supports the required hardware level, or better.
        private boolean isHardwareLevelSupported(
                CameraCharacteristics characteristics, int requiredLevel) {
                int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                        return requiredLevel == deviceLevel;
                }
                // deviceLevel is not LEGACY, can use numerical sort
                return requiredLevel <= deviceLevel;
        }

        private String chooseCamera() {
                final CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
                try {
                        for (final String cameraId : manager.getCameraIdList()) {
                                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                                // We don't use a front facing camera in this sample.
                                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                                        continue;
                                }

                                final StreamConfigurationMap map =
                                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                                if (map == null) {
                                        continue;
                                }

                                // Fallback to camera1 API for internal cameras that don't have full support.
                                // This should help with legacy situations where using the camera2 API causes
                                // distorted or otherwise broken previews.
                                useCamera2API = (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                                        || isHardwareLevelSupported(characteristics,
                                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
                                Timber.i("Camera API lv2?: %s", useCamera2API);
                                return cameraId;
                        }
                } catch (CameraAccessException e) {
                        Timber.e(e, "Not allowed to access camera");
                }

                return null;
        }

        protected void setFragment() {
                String cameraId = chooseCamera();
                if (cameraId == null) {
                        Toast.makeText(getContext(), "No Camera Detected", Toast.LENGTH_SHORT).show();
                        //finish();
                }

                Fragment fragment;
                if (useCamera2API) {
                        CameraConnectionFragment camera2Fragment =
                                CameraConnectionFragment.newInstance(
                                        new CameraConnectionFragment.ConnectionCallback() {
                                                @Override
                                                public void onPreviewSizeChosen(final Size size, final int rotation) {
                                                        previewHeight = size.getHeight();
                                                        previewWidth = size.getWidth();
                                                        CameraFragment.this.onPreviewSizeChosen(size, rotation);
                                                }
                                        },
                                        this,
                                        getLayoutId(),
                                        getDesiredPreviewFrameSize());

                        camera2Fragment.setCamera(cameraId);
                        fragment = camera2Fragment;
                } else {
                        fragment =
                                new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
                }

                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container, fragment)
                        .commit();
        }

        protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
                // Because of the variable row stride it's not possible to know in
                // advance the actual necessary dimensions of the yuv planes.
                for (int i = 0; i < planes.length; ++i) {
                        final ByteBuffer buffer = planes[i].getBuffer();
                        if (yuvBytes[i] == null) {
                                Timber.d("Initializing buffer %d at size %d", i, buffer.capacity());
                                yuvBytes[i] = new byte[buffer.capacity()];
                        }
                        buffer.get(yuvBytes[i]);
                }
        }

        public boolean isDebug() {
                return debug;
        }

        public void requestRender() {
                final OverlayView overlay = (OverlayView) rootview.findViewById(R.id.debug_overlay);
                if (overlay != null) {
                        overlay.postInvalidate();
                }
        }

        public void addCallback(final OverlayView.DrawCallback callback) {
                final OverlayView overlay = (OverlayView) rootview.findViewById(R.id.debug_overlay);
                if (overlay != null) {
                        overlay.addCallback(callback);
                }
        }

        public void onSetDebug(final boolean debug) {}


        protected void readyForNextImage() {
                if (postInferenceCallback != null) {
                        postInferenceCallback.run();
                }
        }

        protected int getScreenOrientation() {
                switch (getActivity().getWindowManager().getDefaultDisplay().getRotation()) {
                        case Surface.ROTATION_270:
                                return 270;
                        case Surface.ROTATION_180:
                                return 180;
                        case Surface.ROTATION_90:
                                return 90;
                        default:
                                return 0;
                }
        }

        protected abstract void processImage();

        protected abstract void onPreviewSizeChosen(final Size size, final int rotation);
        protected abstract int getLayoutId();
        protected abstract Size getDesiredPreviewFrameSize();
}