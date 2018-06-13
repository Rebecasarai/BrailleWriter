package com.rebecasarai.braillewriter.fragments;

/*
 * Copyright 2017 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import timber.log.Timber;

import com.rebecasarai.braillewriter.R;
import com.rebecasarai.braillewriter.ui.objectRecognition.AutoFitTextureView;
import com.rebecasarai.braillewriter.utils.ImageUtils;


import java.io.IOException;
import java.util.List;

public class LegacyCameraConnectionFragment extends Fragment {
  private Camera camera;
  private Camera.PreviewCallback imageListener;
  private Size desiredSize;
  private int rotation;

  /**
   * The layout identifier to inflate for this Fragment.
   */
  private int layout;

  @SuppressLint("ValidFragment")
  public LegacyCameraConnectionFragment(
          final Camera.PreviewCallback imageListener, final int layout, final Size desiredSize, int rotation) {
    this.imageListener = imageListener;
    this.layout = layout;
    this.desiredSize = desiredSize;
    this.rotation = rotation;
  }

  public LegacyCameraConnectionFragment() {

  }

  // invoked when the activity may be temporarily destroyed, save the instance state here
  @Override
  public void onSaveInstanceState(Bundle outState) {
    outState.putInt("layout", layout);
    //outState.putSize("desiredSize", desiredSize);

    // call superclass to save any view hierarchy
    super.onSaveInstanceState(outState);
  }


  @Override
  public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    super.onViewStateRestored(savedInstanceState);
    if(savedInstanceState !=null){

      this.layout = savedInstanceState.getInt("layout");
      //this.desiredSize = savedInstanceState.getSize("desiredSize");
    }
  }

  /**
   * Conversion from screen rotation to JPEG orientation.
   */
  private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

  static {
    ORIENTATIONS.append(Surface.ROTATION_0, 90);
    ORIENTATIONS.append(Surface.ROTATION_90, 0);
    ORIENTATIONS.append(Surface.ROTATION_180, 270);
    ORIENTATIONS.append(Surface.ROTATION_270, 180);
  }

  /**
   * {@link android.view.TextureView.SurfaceTextureListener} handles several lifecycle events on a
   * {@link TextureView}.
   */
  private final TextureView.SurfaceTextureListener surfaceTextureListener =
      new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(
                final SurfaceTexture texture, final int width, final int height) {

          int index = getCameraId();
          camera = Camera.open(index);

          try {
            Camera.Parameters parameters = camera.getParameters();
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes != null
                    && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
              parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            List<Camera.Size> cameraSizes = parameters.getSupportedPreviewSizes();
            Size[] sizes = new Size[cameraSizes.size()];
            int i = 0;
            for (Camera.Size size : cameraSizes) {
              sizes[i++] = new Size(size.width, size.height);
            }

            Size previewSize = null;
            previewSize =  CameraConnectionFragment.chooseOptimalSize(
                    sizes, desiredSize.getWidth(), desiredSize.getHeight());

            if(Math.abs(rotation) == 180 || Math.abs(rotation) == 270 || Math.abs(rotation) == 0 || Math.abs(rotation) == 360 ){
              previewSize = CameraConnectionFragment.chooseOptimalSize(sizes, desiredSize.getHeight(),desiredSize.getWidth());
            }

            //parameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
            parameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
            camera.setDisplayOrientation(Math.abs(rotation));
            camera.setParameters(parameters);
            camera.setPreviewTexture(texture);
          } catch (IOException exception) {
            camera.release();
          }

          camera.setPreviewCallbackWithBuffer(imageListener);
          Camera.Size s = camera.getParameters().getPreviewSize();
          camera.addCallbackBuffer(new byte[ImageUtils.getYUVByteSize(s.height, s.width)]);

          if(Math.abs(rotation) == 180 || Math.abs(rotation) == 270 || Math.abs(rotation) == 0|| Math.abs(rotation) == 360){

            //camera.addCallbackBuffer(new byte[ImageUtils.getYUVByteSize(720, 480)]);
            //textureView.setAspectRatio( 1920, 1080);
            textureView.setAspectRatio(  1920, 1080);

          }else{

            //textureView.setAspectRatio(s.height, s.width);
            textureView.setAspectRatio(s.height, s.width+200);

          }

          camera.startPreview();
        }

        @Override
        public void onSurfaceTextureSizeChanged(
            final SurfaceTexture texture, final int width, final int height) {}

        @Override
        public boolean onSurfaceTextureDestroyed(final SurfaceTexture texture) {
          return true;
        }

        @Override
        public void onSurfaceTextureUpdated(final SurfaceTexture texture) {}
      };

  /**
   * An {@link AutoFitTextureView} for camera preview.
   */
  private AutoFitTextureView textureView;

  /**
   * An additional thread for running tasks that shouldn't block the UI.
   */
  private HandlerThread backgroundThread;

  @Override
  public View onCreateView(
      final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {

    // recovering the instance state
    if (savedInstanceState != null) {
      this.layout = savedInstanceState.getInt("layout");
      this.desiredSize = savedInstanceState.getSize("desiredSize");
      this.imageListener = savedInstanceState.getParcelable("imageListener");
    }
    return inflater.inflate(layout, container, false);
  }

  @Override
  public void onViewCreated(final View view, final Bundle savedInstanceState) {
    textureView = (AutoFitTextureView) view.findViewById(R.id.texture);
  }

  @Override
  public void onActivityCreated(final Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
  }

  @Override
  public void onResume() {
    super.onResume();
    startBackgroundThread();
    // When the screen is turned off and turned back on, the SurfaceTexture is already
    // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
    // a camera and start preview from here (otherwise, we wait until the surface is ready in
    // the SurfaceTextureListener).

    if (textureView.isAvailable()) {
      camera.startPreview();
    } else {
      textureView.setSurfaceTextureListener(surfaceTextureListener);
    }
  }

  @Override
  public void onPause() {
    stopCamera();
    stopBackgroundThread();
    super.onPause();
  }

  /**
   * Starts a background thread and its {@link Handler}.
   */
  private void startBackgroundThread() {
    backgroundThread = new HandlerThread("CameraBackground");
    backgroundThread.start();
  }

  /**
   * Stops the background thread and its {@link Handler}.
   */
  private void stopBackgroundThread() {
    backgroundThread.quitSafely();
    try {
      backgroundThread.join();
      backgroundThread = null;
    } catch (final InterruptedException e) {
      Timber.e(e, "Exception!");
    }
  }

  protected void stopCamera() {
    if (camera != null) {
      camera.stopPreview();
      camera.setPreviewCallback(null);
      camera.release();
      camera = null;
    }
  }

  private int getCameraId() {
    CameraInfo ci = new CameraInfo();
    for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
      Camera.getCameraInfo(i, ci);
      if (ci.facing == CameraInfo.CAMERA_FACING_BACK)
        return i;
    }
    return -1; // No camera found
  }
}
