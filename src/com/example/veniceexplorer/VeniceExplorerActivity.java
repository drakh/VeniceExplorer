package com.example.veniceexplorer;

import rajawali.RajawaliActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import android.os.Environment;
import android.util.Log;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
//import android.widget.ListView;
import android.view.View;
import android.view.View.OnClickListener;
import java.text.DecimalFormat;
import java.math.RoundingMode;
import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import android.widget.AdapterView;
//import android.widget.ListAdapter;

public class VeniceExplorerActivity extends RajawaliActivity implements
		SensorEventListener {
	/* step detector */
	DecimalFormat d = new DecimalFormat("#.##");
	private boolean detecting = false;
	private static int mLimit = 15;
	private static float mLastValues[] = new float[3 * 2];
	private static float mScale[] = new float[2];
	private static float mYOffset = 0;
	private static float mLastDirections[] = new float[3 * 2];
	private static float mLastExtremes[][] = { new float[3 * 2],
			new float[3 * 2] };
	private static float mLastDiff[] = new float[3 * 2];
	private static int mLastMatch = -1;
	private int steps = 0;
	private float step_len = 0.67f;
	static {
		int h = 480;
		mYOffset = h * 0.5f;
		mScale = new float[2];
		mScale[0] = -(h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
		mScale[1] = -(h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
	}
	/* main app */
	private SensorManager mSensorManager = null;
	private VeniceExplorerRenderer mRenderer;
	private CameraPreview mCameraSurface;
	private String filename = "main.xml";
	private String dirname = "VeniceViewer";
	private TextView rotZ;
	private ArrayList<ProjectLevel> vProjects;
	private LinearLayout ll;
	private float[] orientation;
	private float[] positions;// here be dragons

	private float current_phi;
	private float current_theta;
	private float cam_x;
	private float cam_y;
	private float cam_z;

	public void onCreate(Bundle savedInstanceState) {
		/* <layout setup> */
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LOW_PROFILE);
		getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
		/* doesnt work */
		// getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
		/* </layout setup> */
		super.onCreate(savedInstanceState);
		chkConfig();
		/* <renderer init> */
		// super.createMultisampleConfig();
		super.setGLBackgroundTransparent(true);
		mSurfaceView.setZOrderOnTop(false);
		mRenderer = new VeniceExplorerRenderer(this);
		mRenderer.setSurfaceView(mSurfaceView);
		mRenderer.setObjs(vProjects);
		super.setRenderer(mRenderer);
		mRenderer.setBackgroundColor(0);
		/* </renderer init> */

		/* <camera init> */
		mCameraSurface = new CameraPreview(this);
		mLayout.addView(mCameraSurface);
		/* </camera init> */
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

		rotZ.bringToFront();

		d.setRoundingMode(RoundingMode.HALF_UP);
		d.setMaximumFractionDigits(3);
		d.setMinimumFractionDigits(3);
		/* <text menu> */
		CreateTextMenu();
		positions = new float[3];
		orientation = new float[3];
	}

	@Override
	public void onStop() {
		super.onStop();
		mCameraSurface.stopCam();
		// unregister sensor listeners to prevent the activity from draining the
		// device's battery.
		mSensorManager.unregisterListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		initListeners();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
		mCameraSurface.stopCam();
		super.finish();
	}

	public void chkConfig() {
		Log.d("main", "checkit");
		File folder = new File(Environment.getExternalStorageDirectory() + "/"
				+ dirname);
		if (!folder.exists()) {
			folder.mkdir();
		}
		folder = new File(Environment.getExternalStorageDirectory() + "/"
				+ dirname + "/" + filename);
		try {
			vProjects = new ArrayList();
			parseXML(folder, dirname);
		} catch (XmlPullParserException e) {
			Log.d("main", "xml error");
		} catch (IOException e) {
			Log.d("main", "io error");

		}

	}

	public void parseXML(File file, String dirn) throws XmlPullParserException,
			IOException {
		Log.d("main", "xml parser?");
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(true);
		XmlPullParser xpp = factory.newPullParser();
		FileInputStream fis = new FileInputStream(file);
		xpp.setInput(new InputStreamReader(fis));
		int eventType = xpp.getEventType();
		int jj = 0;
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_DOCUMENT) {
				Log.d("main", "tralala");
			} else if (eventType == XmlPullParser.START_TAG) {
				String nodeName = xpp.getName();
				Log.d("main", nodeName);
				if (nodeName.contentEquals("project")) {
					for (int k = 0; k < xpp.getAttributeCount(); k++) {
						String an = xpp.getAttributeName(k);
						String av = xpp.getAttributeValue(k);
						if (an.contentEquals("name")) {
							vProjects.add(jj, new ProjectLevel(av));
						}
					}
				}
				if (nodeName.contentEquals("object")) {
					ProjectObject po = new ProjectObject();
					for (int k = 0; k < xpp.getAttributeCount(); k++) {
						String an = xpp.getAttributeName(k);
						String av = xpp.getAttributeValue(k);
						if (an.contentEquals("model")) {
							Log.d("ww", "set model");
							po.setModel(dirn + "/" + av);
						} else if (an.contentEquals("texture")) {
							Log.d("ww", "set texture");
							po.setTexture(dirn + "/" + av);
						} else if (an.contentEquals("doublesided")) {
							Log.d("ww", "set doublesided");
							po.setDS(av);
						} else if (an.contentEquals("usevideo")) {
							Log.d("ww", "set video");
							po.setVideo(av);
						}
					}
					vProjects.get(jj).addModel(po);
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				String nodeName = xpp.getName();
				Log.d("main", "end node: " + nodeName);
				if (nodeName.contentEquals("project")) {
					jj++;
				}
			}
			eventType = xpp.next();
		}
		Log.d("main", "Num projects: " + vProjects.size());
	}

	public void CreateTextMenu() {
		/* <build list of projects> */
		for (int i = 0; i < vProjects.size(); i++) {
			TextView chsP = new TextView(this);
			chsP.setText(vProjects.get(i).getName());
			chsP.setClickable(true);
			MyClickListener myh = new MyClickListener(i, this);
			chsP.setOnClickListener(myh);
			ll.addView(chsP);
		}
	}

	private class MyClickListener implements OnClickListener {
		private int w;
		VeniceExplorerActivity a;

		public MyClickListener(int w, VeniceExplorerActivity a) {
			this.w = w;
			this.a = a;
		}

		public void onClick(View v) {
			Log.d("Clicked", "project no:" + getW());
			a.SelectProject(getW());
		}

		public int getW() {
			return w;
		}
	}

	public void SelectProject(int w) {
		mRenderer.showProject(w);
	}

	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ORIENTATION:
			orientation[0] = event.values[1] + 180;
			orientation[1] = event.values[0];
			orientation[2] = event.values[2];
			setCameraPos();
			if (!detecting) {
				storeCurrentRotPos();
			}
			break;
		case Sensor.TYPE_ACCELEROMETER:
			if (detecting)
				detectStep(event);
			break;
		}
	}

	private void storeCurrentRotPos() {

		current_phi = orientation[1];
		current_theta = orientation[0];

		cam_x = mRenderer.getCamera().getX();
		cam_y = mRenderer.getCamera().getY();
		cam_z = mRenderer.getCamera().getZ();

		detecting = true;
	}

	private void detectStep(SensorEvent event) {
		if (event == null)
			return;

		else {
			float vSum = 0;
			for (int i = 0; i < 3; i++) {
				final float v = mYOffset + event.values[i] * mScale[0];
				vSum += v;
			}
			int k = 0;
			float v = vSum / 3;

			float direction = (v > mLastValues[k] ? 1
					: (v < mLastValues[k] ? -1 : 0));
			if (direction == -mLastDirections[k]) {
				// Direction changed
				int extType = (direction > 0 ? 0 : 1); // minumum or
														// maximum?
				mLastExtremes[extType][k] = mLastValues[k];
				float diff = Math.abs(mLastExtremes[extType][k]
						- mLastExtremes[1 - extType][k]);

				if (diff > mLimit) {

					boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k] * 2 / 3);
					boolean isPreviousLargeEnough = mLastDiff[k] > (diff / 3);
					boolean isNotContra = (mLastMatch != 1 - extType);

					if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough
							&& isNotContra) {
						onStep();
						mLastMatch = extType;
					} else {
						mLastMatch = -1;
					}
				}
				mLastDiff[k] = diff;
			}
			mLastDirections[k] = direction;
			mLastValues[k] = v;
		}
	}

	public void onStep() {
		float[] ap = CylindricalToCartesian(current_phi, step_len, cam_y);
		ap[0]=cam_x-ap[0];
		ap[2]=cam_z-ap[2];
		mRenderer.getCamera().setPosition(ap[0], ap[1], ap[2]);
		storeCurrentRotPos();
		steps++;
		setLogValues();
	}

	public void setLogValues() {
		String dtext = "X:" + d.format(orientation[0]) + " | Y: "
				+ d.format(orientation[1]) + " | Z:" + d.format(orientation[2])
				+ "\n";
		dtext += "Step: " + steps + "\n";
		dtext += "X: " + cam_x + " | Y: " + cam_y + " | Z: " + cam_z + "\n";
		rotZ.setText(dtext);
	}

	public void setCameraPos() {
		setLogValues();
		// current camera position
		// convert sphjerical to cartesian - sphere radius =1
		float phi = orientation[1];
		float theta = orientation[0];
		float[] cartesian = SphericalToCartesian(phi, theta, 1f);
		mRenderer.setCamLA(cartesian[0], cartesian[1], cartesian[2]);
	}

	public float[] SphericalToCartesian(float phi, float theta, float r) {
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

	public float[] CylindricalToCartesian(float phi, float r, float h) {
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

	public void onAccuracyChanged(Sensor arg0, int arg1) {

	}

	public void initListeners() {
		mSensorManager.registerListener(this, mSensorManager
				.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
				SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);
	}
}
