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
		SurfaceHolder.Callback
{
	SurfaceHolder		mHolder;
	private Camera		mVCamera;
	private Camera.Size	ps;

	CameraPreview(Context context, Camera.Size cs)
	{
		super(context);
		ps=cs;
		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void surfaceCreated(SurfaceHolder holder)
	{
		if (mVCamera == null)
		{
			mVCamera = Camera.open(0);
			try
			{
				Camera.Parameters p = mVCamera.getParameters();
				p.setPreviewSize(ps.width, ps.height);
				mVCamera.setPreviewDisplay(holder);
				mVCamera.setParameters(p);
				mVCamera.startPreview();
			}
			catch (IOException e)
			{
			}
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder)
	{
		stopCam();
	}

	public void stopCam()
	{

		mVCamera.stopPreview();
		mVCamera.release();
		mVCamera = null;

	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h)
	{
	}
}