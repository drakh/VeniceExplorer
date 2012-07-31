package com.example.veniceexplorer;

import java.io.IOException;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.util.Log;

public class CameraPreview extends SurfaceView implements
		SurfaceHolder.Callback {
	SurfaceHolder mHolder;
	public Camera mVCamera;

	CameraPreview(Context context) {
		super(context);
		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		Log.d("main", "camera context init");
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.d("main", "camera surface created");
		if (mVCamera == null) {
			mVCamera = Camera.open(0);
			try {
				Camera.Parameters parameters = mVCamera.getParameters();
				mVCamera.setDisplayOrientation(0);
				mVCamera.setPreviewDisplay(holder);
				mVCamera.startPreview();
			    mVCamera.setParameters(parameters);
			    mVCamera.startPreview();
				Log.d("main", "camera started");
			} catch (IOException e) {
				Log.d("main", "camera preview failed");
			}
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		stopCam();
	}

	public void stopCam()

	{

		mVCamera.stopPreview();
		mVCamera.release();
		mVCamera = null;

	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
	}
}