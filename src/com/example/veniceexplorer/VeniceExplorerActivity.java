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
	DecimalFormat d = new DecimalFormat("#.##");
	private SensorManager mSensorManager = null;
	private VeniceExplorerRenderer mRenderer;
	private CameraPreview mCameraSurface;
	private String filename = "main.xml";
	private String dirname = "VeniceViewer";
	private TextView rotZ;
	private ArrayList<ProjectLevel> vProjects;
	private LinearLayout ll;

	public void onCreate(Bundle savedInstanceState) {
		/* <layout setup> */
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		/*doesnt work*/
		//getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG); 
		/* </layout setup> */
		super.onCreate(savedInstanceState);
		chkConfig();
		/* <renderer init> */
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
		/*<text menu>*/
		CreateTextMenu();
	}

	@Override
	public void onStop() {
		super.onStop();
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
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		/*causes crash*/
		// this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD);
		// this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
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
							po.setModel(dirn + "/" + av);
						}
						if (an.contentEquals("texture")) {
							po.setTexture(dirn + "/" + av);
						}
						if (an.contentEquals("doublesided")) {
							//po.setDS(dirn + "/" + av);
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
	public void CreateTextMenu()
	{
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

		Log.d("Select project", "project no:" + w + ":"
				+ vProjects.get(w).getName());
		//mRenderer.LoadObjects(vProjects.get(w));
		mRenderer.showProject(w);
	}

	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ORIENTATION:
			// mRenderer.setCameraAngle((-1 * event.values[1]) - 90,0, 0);
			rotZ.setText("X:" + d.format((event.values[1]) + 90) + " | Y: "
					+ d.format(event.values[0]) + " | Z:"
					+ d.format(event.values[1]));
			mRenderer
					.setCameraAngle((event.values[1]) + 90, event.values[0], 0);
			break;
		}
	}

	public void onAccuracyChanged(Sensor arg0, int arg1) {

	}

	public void initListeners() {
		mSensorManager.registerListener(this, mSensorManager
				.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
				SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_UI);
	}
}
