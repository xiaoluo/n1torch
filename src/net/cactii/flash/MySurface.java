package net.cactii.flash;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MySurface extends SurfaceView implements SurfaceHolder.Callback {

  SurfaceHolder mHolder;
  public Camera mCamera;
  public MySurface(Context context) {
    super(context);
    mHolder = getHolder();
    mHolder.addCallback(this);
    mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    Camera.Parameters parameters = mCamera.getParameters();
    //parameters.setPreviewSize(width, height);
    mCamera.setParameters(parameters);
    mCamera.startPreview();
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    try {
      mCamera.setPreviewDisplay(holder);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    //mCamera.stopPreview();
  }
}
