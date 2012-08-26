package com.example.veniceexplorer;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import rajawali.animation.TimerManager;

import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;

import android.view.Gravity;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.view.View;
import android.view.View.OnClickListener;
import java.text.DecimalFormat;
import java.math.RoundingMode;
import java.util.ArrayList;

public class VeniceExplorerActivity extends Activity implements
		SensorEventListener
{
	private float[]					gyro					= new float[3];
	public static final float		EPSILON					= 0.000000001f;
	private static final float		NS2S					= 1.0f / 1000000000.0f;
	private float					timestamp				= 0f;
	float							gyroVal					= 0;
	float							accVal					= 9.8f;
	boolean							moving					= false;
	private float					rot						= 0;
	/* step detector */
	DecimalFormat					d						= new DecimalFormat(
																	"#.##");
	private boolean					detecting				= false;
	private static int				mLimit					= 15;
	private static float			mLastValues[]			= new float[3 * 2];
	private static float			mScale[]				= new float[2];
	private static float			mYOffset				= 0;
	private static float			mLastDirections[]		= new float[3 * 2];
	private static float			mLastExtremes[][]		= {
			new float[3 * 2], new float[3 * 2]				};
	private static float			mLastDiff[]				= new float[3 * 2];
	private static int				mLastMatch				= -1;
	private int						steps					= 0;
	private float					step_len				= 0.67f;
	private int						da						= 0;
	static
	{
		int h = 480;
		mYOffset = h * 0.5f;
		mScale = new float[2];
		mScale[0] = -(h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
		mScale[1] = -(h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
	}

	protected FrameLayout			mLayout;
	protected GLSurfaceView			mSurfaceView;
	protected boolean				mMultisamplingEnabled	= true;
	protected boolean				mUsesCoverageAa;
	private VeniceExplorerRenderer	mRenderer;
	protected boolean				checkOpenGLVersion		= true;

	/* main app */
	private SensorManager			mSensorManager			= null;
	private CameraPreview			mCameraSurface;
	private String					filename				= "main.xml";
	private String					dirname					= "VeniceViewer";
	private TextView				rotZ;
	private ArrayList<ProjectLevel>	vProjects;
	private LinearLayout			ll;
	private float[]					orientation;
	private float[]					positions;										// here
																					// be
																					// dragons
	private boolean					showDebug				= false;
	private float					current_phi;
	private float					current_theta;
	private float					cam_x;
	private float					cam_y;
	private float					cam_z;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		/* <layout setup> */
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LOW_PROFILE);
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		chkConfig();
		super.onCreate(savedInstanceState);
		/* <renderer init> */

		mLayout = new FrameLayout(this);
		mLayout.setForegroundGravity(Gravity.CENTER);

		int h = metrics.heightPixels;
		int w = metrics.widthPixels;
		Camera c = Camera.open(0);
		Camera.Parameters p = c.getParameters();
		Camera.Size cs = getBestPreviewSize(w, h, p);
		c.release();
		c = null;
		// float fl = p.getFocalLength();
		float ha = p.getHorizontalViewAngle();
		float wa = p.getVerticalViewAngle();

		ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(cs.width,
				cs.height);
		float fov = (float) Math.round(Math.sqrt(ha * ha + wa * wa));

		/* <camera init> */
		mCameraSurface = new CameraPreview(this, cs);
		mCameraSurface.setLayoutParams(lp);
		mCameraSurface.setX((w - cs.width) / 2);
		mCameraSurface.setY((h - cs.height) / 2);
		mLayout.addView(mCameraSurface);
		/* </camera init> */

		/* </renderer init> */
		mRenderer = new VeniceExplorerRenderer(this, fov);
		mSurfaceView = new GLSurfaceView(this);
		mSurfaceView.setEGLContextClientVersion(2);
		mSurfaceView.setLayoutParams(lp);
		mSurfaceView.setX((w - cs.width) / 2);
		mSurfaceView.setY((h - cs.height) / 2);
		mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
		mSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
		// mSurfaceView.setZOrderOnTop(true);
		mSurfaceView.setZOrderMediaOverlay(true);
		mRenderer.setSurfaceView(mSurfaceView);
		mRenderer.setObjs(vProjects);
		mSurfaceView.setRenderer(mRenderer);
		mLayout.addView(mSurfaceView);
		mRenderer.setBackgroundColor(0);
		/* </renderer init> */

		/* <sensors init> */
		mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
		initListeners();
		/* </sensors init> */

		/* <some GUI> */
		ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setGravity(Gravity.TOP);
		mLayout.addView(ll);

		/* <debug text> */
		rotZ = new TextView(this);
		rotZ.setText("");
		rotZ.setTextSize(12);
		rotZ.setGravity(Gravity.CENTER);
		rotZ.setHeight(100);
		rotZ.setTextColor(Color.WHITE);

		ll.addView(rotZ);
		d.setRoundingMode(RoundingMode.HALF_UP);
		d.setMaximumFractionDigits(3);
		d.setMinimumFractionDigits(3);
		/* <text menu> */
		CreateTextMenu();
		positions = new float[3];
		orientation = new float[3];

		float rp = w - 90 - 20;
		float bp = h - 33 - 20;
		FrameLayout gui = new FrameLayout(this);
		ViewGroup.LayoutParams gui_p = new ViewGroup.LayoutParams(90, 33);

		ImageView info_v = new ImageView(this);
		info_v.setLayoutParams(gui_p);
		info_v.setImageResource(R.drawable.info_b);
		info_v.setX(rp);
		info_v.setY(bp);
		gui.addView(info_v);

		ImageView back_v = new ImageView(this);
		back_v.setImageResource(R.drawable.back_b);
		back_v.setLayoutParams(gui_p);
		back_v.setX(20);
		back_v.setY(20);
		gui.addView(back_v);
		ImageView comm_v = new ImageView(this);
		comm_v.setLayoutParams(gui_p);
		comm_v.setImageResource(R.drawable.comment_b);
		comm_v.setX(rp);
		comm_v.setY(20);
		gui.addView(comm_v);
		mLayout.addView(gui);
		setContentView(mLayout);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		mSurfaceView.onResume();
		initListeners();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		TimerManager.getInstance().clear();
		mSensorManager.unregisterListener(this);
		mCameraSurface.stopCam();
		mSurfaceView.onPause();
		super.finish();
		System.gc();
		System.exit(0);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		mRenderer.onSurfaceDestroyed();
		unbindDrawables(mLayout);
		System.gc();
		System.exit(0);
	}

	private void unbindDrawables(View view)
	{
		if (view.getBackground() != null)
		{
			view.getBackground().setCallback(null);
		}
		if (view instanceof ViewGroup && !(view instanceof AdapterView))
		{
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
			{
				unbindDrawables(((ViewGroup) view).getChildAt(i));
			}
			((ViewGroup) view).removeAllViews();
		}
	}

	public void chkConfig()
	{
		Log.d("main", "checkit");
		File folder = new File(Environment.getExternalStorageDirectory() + "/"
				+ dirname);
		if (!folder.exists())
		{
			folder.mkdir();
		}
		folder = new File(Environment.getExternalStorageDirectory() + "/"
				+ dirname + "/" + filename);
		try
		{
			vProjects = new ArrayList();
			parseXML(folder, dirname);
		}
		catch (XmlPullParserException e)
		{
			Log.d("main", "xml error");
		}
		catch (IOException e)
		{
			Log.d("main", "io error");

		}

	}

	public void parseXML(File file, String dirn) throws XmlPullParserException,
			IOException
	{
		Log.d("main", "xml parser?");
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(true);
		XmlPullParser xpp = factory.newPullParser();
		FileInputStream fis = new FileInputStream(file);
		xpp.setInput(new InputStreamReader(fis));
		int eventType = xpp.getEventType();
		int jj = 0;
		while (eventType != XmlPullParser.END_DOCUMENT)
		{
			if (eventType == XmlPullParser.START_DOCUMENT)
			{
				Log.d("main", "tralala");
			}
			else if (eventType == XmlPullParser.START_TAG)
			{
				String nodeName = xpp.getName();
				Log.d("main", nodeName);
				if (nodeName.contentEquals("venice"))
				{
					for (int k = 0; k < xpp.getAttributeCount(); k++)
					{
						String an = xpp.getAttributeName(k);
						String av = xpp.getAttributeValue(k);
						if (an.contentEquals("da"))
						{
							da = Integer.parseInt(av);
						}
					}
				}
				else if (nodeName.contentEquals("project"))
				{
					for (int k = 0; k < xpp.getAttributeCount(); k++)
					{
						String an = xpp.getAttributeName(k);
						String av = xpp.getAttributeValue(k);
						if (an.contentEquals("name"))
						{
							vProjects.add(jj, new ProjectLevel(av));
						}

					}
				}
				else if (nodeName.contentEquals("object"))
				{
					ProjectObject po = new ProjectObject();
					for (int k = 0; k < xpp.getAttributeCount(); k++)
					{
						String an = xpp.getAttributeName(k);
						String av = xpp.getAttributeValue(k);
						if (an.contentEquals("model"))
						{
							po.setModel(dirn + "/" + av);
						}
						else if (an.contentEquals("texture"))
						{
							Log.d("ww", "set texture");
							po.setTexture(dirn + "/" + av);
						}
						else if (an.contentEquals("doublesided"))
						{
							po.setDS(av);
						}
						else if (an.contentEquals("usevideo"))
						{
							po.setVideo(av);
						}
						else if (an.contentEquals("interactive"))
						{
							po.setInteractive(av);
						}
						else if (an.contentEquals("visible"))
						{

						}
					}
					vProjects.get(jj).addModel(po);
				}
				else if (nodeName.contentEquals("action"))
				{

				}
				else if (nodeName.contentEquals("texture"))
				{
					// vProjects.get(jj).addTexture(xpp.getText());
				}
			}
			else if (eventType == XmlPullParser.END_TAG)
			{
				String nodeName = xpp.getName();
				Log.d("main", "end node: " + nodeName);
				if (nodeName.contentEquals("project"))
				{
					jj++;
				}
			}
			eventType = xpp.next();
		}
		Log.d("main", "Num projects: " + vProjects.size());
	}

	public void CreateTextMenu()
	{
		/* <build list of projects> */
		for (int i = 0; i < vProjects.size(); i++)
		{
			TextView chsP = new TextView(this);
			chsP.setTextSize(18);
			chsP.setGravity(Gravity.LEFT);
			chsP.setHeight(32);
			chsP.setShadowLayer(2f, 2f, 2f, Color.BLACK);
			chsP.setPadding(10, 2, 0, 2);
			chsP.setText(vProjects.get(i).getName());
			chsP.setClickable(true);
			MyClickListener myh = new MyClickListener(i, this);
			chsP.setOnClickListener(myh);
			ll.addView(chsP);
		}
	}

	private class MyClickListener implements OnClickListener
	{
		private int				w;
		VeniceExplorerActivity	a;

		public MyClickListener(int w, VeniceExplorerActivity a)
		{
			this.w = w;
			this.a = a;
		}

		public void onClick(View v)
		{
			Log.d("Clicked", "project no:" + getW());
			a.SelectProject(getW());
		}

		public int getW()
		{
			return w;
		}
	}

	public void SelectProject(int w)
	{
		mRenderer.showProject(w);
	}

	public void onSensorChanged(SensorEvent event)
	{
		switch (event.sensor.getType())
		{

			case Sensor.TYPE_GYROSCOPE:
				gyroFunction(event);
				break;
			case Sensor.TYPE_ORIENTATION:
				orientation[0] = event.values[1] + 180;
				orientation[1] = event.values[0];
				orientation[2] = event.values[2];
				setCameraPos();
				if (!detecting)
				{
					storeCurrentRotPos();
				}
				break;
			case Sensor.TYPE_ACCELEROMETER:
				accFunction(event);
				if (detecting) detectStep(event);
				break;
		}
	}

	private void storeCurrentRotPos()
	{

		current_phi = orientation[1];
		current_theta = orientation[0];

		cam_x = mRenderer.getCamera().getX();
		cam_y = mRenderer.getCamera().getY();
		cam_z = mRenderer.getCamera().getZ();

		detecting = true;
	}

	private void detectStep(SensorEvent event)
	{
		if (event == null) return;

		else
		{
			float vSum = 0;
			for (int i = 0; i < 3; i++)
			{
				final float v = mYOffset + event.values[i] * mScale[0];
				vSum += v;
			}
			int k = 0;
			float v = vSum / 3;

			float direction = (v > mLastValues[k] ? 1
					: (v < mLastValues[k] ? -1 : 0));
			if (direction == -mLastDirections[k])
			{
				// Direction changed
				int extType = (direction > 0 ? 0 : 1); // minumum or
														// maximum?
				mLastExtremes[extType][k] = mLastValues[k];
				float diff = Math.abs(mLastExtremes[extType][k]
						- mLastExtremes[1 - extType][k]);

				if (diff > mLimit)
				{

					boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k] * 2 / 3);
					boolean isPreviousLargeEnough = mLastDiff[k] > (diff / 3);
					boolean isNotContra = (mLastMatch != 1 - extType);

					if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough
							&& isNotContra)
					{
						onStep();
						mLastMatch = extType;
					}
					else
					{
						mLastMatch = -1;
					}
				}
				mLastDiff[k] = diff;
			}
			mLastDirections[k] = direction;
			mLastValues[k] = v;
		}
	}

	public void onStep()
	{
		float[] ap = CylindricalToCartesian(current_phi, step_len, cam_y);
		ap[0] = cam_x - ap[0];
		ap[2] = cam_z - ap[2];
		mRenderer.setCamPos(ap[0], ap[1], ap[2]);
		storeCurrentRotPos();
		steps++;
		setLogValues();
	}

	public void setLogValues()
	{
		String dtext = "X:" + d.format(orientation[0]) + " | Y: "
				+ d.format(orientation[1]) + " | Z:" + d.format(orientation[2])
				+ "\n";
		dtext += "Step: " + steps + "\n";
		dtext += "X: " + cam_x + " | Y: " + cam_y + " | Z: " + cam_z + "\n";
		rotZ.setText(dtext);
	}

	public void setCameraPos()
	{
		setLogValues();
		// current camera position
		// convert sphjerical to cartesian - sphere radius =1
		float phi=(float) (rot*1.2f + (orientation[1]+da)*0.3f)/(1.2f+0.3f);//weighted
		float theta = orientation[0];
		float[] cartesian = SphericalToCartesian(phi, theta, 1f);
		mRenderer.setCamLA(cartesian[0], cartesian[1], cartesian[2]);
	}

	public float[] SphericalToCartesian(float phi, float theta, float r)
	{
		float[] coords = new float[3];
		float p = (float) Math.toRadians(phi);
		float t = (float) Math.toRadians(theta);
		float sinPhi = (float) (Math.round(Math.sin(p) * 1000)) / 1000;
		float cosPhi = (float) (Math.round(Math.cos(p) * 1000)) / 1000;
		float sinTheta = (float) (Math.round(Math.sin(t) * 1000)) / 1000;
		float cosTheta = (float) (Math.round(Math.cos(t) * 1000)) / 1000;
		float ay = r * cosTheta;
		float ax = r * sinPhi * sinTheta;
		float az = r * cosPhi * sinTheta;
		coords[0] = ax;
		coords[1] = ay;
		coords[2] = az;
		return coords;
	}

	public float[] CylindricalToCartesian(float phi, float r, float h)
	{
		float[] coords = new float[3];
		float p = (float) Math.toRadians(phi);
		float sinPhi = (float) (Math.round(Math.sin(p) * 1000)) / 1000;
		float cosPhi = (float) (Math.round(Math.cos(p) * 1000)) / 1000;
		float ax = r * sinPhi;
		float az = r * cosPhi;
		coords[0] = ax;
		coords[1] = h;
		coords[2] = az;
		return coords;

	}

	public void onAccuracyChanged(Sensor arg0, int arg1)
	{

	}

	public void initListeners()
	{
		mSensorManager.registerListener(this, mSensorManager
				.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
				SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
				SensorManager.SENSOR_DELAY_UI);
	}

	private Camera.Size getBestPreviewSize(int width, int height,
			Camera.Parameters p)
	{
		Camera.Size result = null;
		for (Camera.Size size : p.getSupportedPreviewSizes())
		{
			if (size.width <= width && size.height <= height)
			{
				if (result == null)
				{
					result = size;
				}
				else
				{
					int resultArea = result.width * result.height;
					int newArea = size.width * size.height;

					if (newArea > resultArea)
					{
						result = size;
					}
				}
			}
		}
		return result;
	}

	public void gyroFunction(SensorEvent event)
	{
		if (timestamp * NS2S > 2)
		{
			final float dT = (event.timestamp - timestamp) * NS2S;
			final float rot_v = event.values[1];
			gyroVal += (rot_v - gyroVal) / 10;
			float omegaMagnitude = (float) Math.sqrt(event.values[0]
					* event.values[0] + gyroVal * gyroVal + event.values[2]
					* event.values[2]);
			if (omegaMagnitude > EPSILON && moving == true)
			{
				rot -= Math.toDegrees(gyroVal * dT);
				if (rot > 360)
				{
					rot = rot % 360;
				}
			}
			// measurement done, save current time for next interval
		}
		timestamp = event.timestamp;
	}

	public void accFunction(SensorEvent event)
	{
		float omegaMagnitude = (float) Math.sqrt(event.values[0]
				* event.values[0] + event.values[1] * event.values[1]
				+ event.values[2] * event.values[2]);
		float prevAcc = accVal;
		accVal += (omegaMagnitude - accVal) / 2.5;
		if (Math.abs(prevAcc - accVal) >= 0.02)
		{
			moving = true;
		}
		else
		{
			moving = false;
		}
	}
	/*
	 * public void onReceive(Context context , Intent intent) { String action =
	 * intent.getAction();
	 * 
	 * if(action.equals(Intent.ACTION_POWER_CONNECTED)) { // Do something when
	 * power connected } else
	 * if(action.equals(Intent.ACTION_POWER_DISCONNECTED)) { // Do something
	 * when power disconnected } }
	 */
}
