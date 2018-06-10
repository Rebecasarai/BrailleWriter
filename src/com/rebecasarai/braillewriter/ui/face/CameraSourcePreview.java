package com.rebecasarai.braillewriter.ui.face;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.support.v4.app.ActivityCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.CameraSource;

import java.io.IOException;

import timber.log.Timber;

/**
 * Created by rebecagonzalez on 17/12/17.
 */

public class CameraSourcePreview extends ViewGroup {
        private static final String TAG = "CameraPreview";

        private Context mContext;
        private SurfaceView mSurfaceView;
        private boolean mStartRequested;
        private boolean mSurfaceAvailable;
        private CameraSource mCameraSource;

        private GraphicOverlay mOverlay;

        public CameraSourcePreview(Context context, AttributeSet attrs) {
            super(context, attrs);
            mContext = context;
            mStartRequested = false;
            mSurfaceAvailable = false;

            mSurfaceView = new SurfaceView(context);
            mSurfaceView.getHolder().addCallback(new SurfaceCallback());
            addView(mSurfaceView);
        }

        public void start(CameraSource cameraSource) throws IOException {
            if (cameraSource == null) {
                stop();
            }

            mCameraSource = cameraSource;

            if (mCameraSource != null) {
                mStartRequested = true;
                startIfReady();
            }
        }

        public void start(CameraSource cameraSource, GraphicOverlay overlay) throws IOException {
            mOverlay = overlay;
            start(cameraSource);
        }

        public void stop() {
            if (mCameraSource != null) {
                mCameraSource.stop();
            }
        }

        public void release() {
            if (mCameraSource != null) {
                mCameraSource.release();
                mCameraSource = null;
            }
        }

        private void startIfReady() throws IOException {
            if (mStartRequested && mSurfaceAvailable) {
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {


                    return;
                }
                mCameraSource.start(mSurfaceView.getHolder());
                if (mOverlay != null) {
                    Size size = mCameraSource.getPreviewSize();
                    int min = Math.min(size.getWidth(), size.getHeight());
                    int max = Math.max(size.getWidth(), size.getHeight());
                    if (isPortraitMode()) {
                        // Intercambia los tamaños de ancho y alto cuando está en vertical, ya que será rotado 90 grados
                        mOverlay.setCameraInfo(min, max, mCameraSource.getCameraFacing());
                    } else {
                        mOverlay.setCameraInfo(max, min, mCameraSource.getCameraFacing());
                    }
                    mOverlay.clear();
                }
                mStartRequested = false;
            }
        }

        private class SurfaceCallback implements SurfaceHolder.Callback {
            @Override
            public void surfaceCreated(SurfaceHolder surface) {
                mSurfaceAvailable = true;
                try {
                    startIfReady();
                } catch (IOException e) {
                    Timber.d( "No se pudo iniciar la camara");
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surface) {
                mSurfaceAvailable = false;
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int previewWidth = 320;
            int previewHeight = 240;

            if (mCameraSource != null) {
                Size size = mCameraSource.getPreviewSize();
                if (size != null) {
                    previewWidth = size.getWidth();
                    previewHeight = size.getHeight();
                }
            }

            // Cambia los tamaños de ancho y alto cuando está en el portrait
            if (isPortraitMode()) {
                int tmp = previewWidth;
                previewWidth = previewHeight;
                previewHeight = tmp;
            }

            // Calcula el ancho y el alto de la camera preview
            final int viewWidth = right - left;
            final int viewHeight = bottom - top;

            int childWidth;
            int childHeight;
            int childXOffset = 0;
            int childYOffset = 0;
            float widthRatio = (float) viewWidth / (float) previewWidth;
            float heightRatio = (float) viewHeight / (float) previewHeight;

            // Para llenar la vista con la vista previa de la cámara, al mismo tiempo que se conserva la relación de aspecto que debe ser,
            // tabien se puede sobredimensionar un poco el child y recortar porciones
            // Escalamos según la dimensión que requiere la mayor corrección, y
            // calcular un recorte para desplazar la otra dimensión.


            if (widthRatio > heightRatio) {
                childWidth = viewWidth;
                childHeight = (int) ((float) previewHeight * widthRatio);
                childYOffset = (childHeight - viewHeight) / 2;
            } else {
                childWidth = (int) ((float) previewWidth * heightRatio);
                childHeight = viewHeight;
                childXOffset = (childWidth - viewWidth) / 2;
            }

            for (int i = 0; i < getChildCount(); ++i) {
                // Dimensión recortada. Cambiamos el child por arriba desplazando y ajustamos
                // el tamaño para mantener la relación de aspecto, que no se distorsione.
                getChildAt(i).layout(
                        -1 * childXOffset, -1 * childYOffset,
                        childWidth - childXOffset, childHeight - childYOffset);
            }

            try {
                startIfReady();
            } catch (IOException e) {
                Timber.e( "No pudo acceder a la camara");
            }
        }

        /**
         * Verifica si esta en vertical
         * @return boolean que representa si lo esta o no
         */
        private boolean isPortraitMode() {
            int orientation = mContext.getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                return false;
            }
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                return true;
            }

            Timber.d( "isPortraitMode returning false by default");
            return false;
        }
    }
