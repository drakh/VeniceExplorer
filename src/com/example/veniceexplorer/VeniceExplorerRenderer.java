package com.example.veniceexplorer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.opengl.GLES20;
import android.os.Environment;
import rajawali.renderer.RajawaliRenderer;
import rajawali.util.RajLog;
import android.content.Context;
import rajawali.BaseObject3D;
import rajawali.lights.PointLight;
import rajawali.parser.ObjParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import rajawali.math.Number3D;
import rajawali.materials.*;
import rajawali.materials.TextureManager.TextureType;
import java.util.ArrayList;
import android.util.Log;

public class VeniceExplorerRenderer extends RajawaliRenderer {
	private PointLight mLight;
	private ObjParser mParser;
	private TextureManager mTextureManager;
	private ArrayList<ProjectLevel> ps;
	private int ActualModel;
	private boolean izLoaded;
	private float rot = 0f;

	public VeniceExplorerRenderer(Context context) {
		super(context);
		RajLog.enableDebug(true);
		setFrameRate(60);
		izLoaded = false;
	}

	protected void initScene() {
		Log.d("main", "scene init");
		mTextureManager = new TextureManager();
		mLight = new PointLight();
		mLight.setColor(1.0f, 1.0f, 1.0f);
		mLight.setPower(5f);
		mLight.setAttenuation(500, 1, .09f, .032f);
		mLight.setPosition(0f, 1.6f, 0f);
		mCamera.setPosition(0f, 1.4f, 0f);
		mCamera.setFarPlane(50f);
		mCamera.setNearPlane(0.1f);
		for (int i = 0; i < ps.size(); i++) {
			LoadObjects(ps.get(i));
		}
		// LoadObjects(ps);
		/*
		 * mParser = new ObjParser(this, "VeniceViewer/mesto_obj");
		 * mParser.parse(); mObj = mParser.getParsedObject();
		 * mObj.addLight(mLight); addChild(mObj);
		 */
	}

	public void setObjs(ArrayList<ProjectLevel> p) {
		this.ps = p;
		Log.d("main", "set projects");
	}

	public void LoadObjects(ProjectLevel p) {
		for (int i = 0; i < p.getModels().size(); i++) {
			mParser = new ObjParser(this, p.getModels().get(i).getModel());
			mParser.parse();
			BaseObject3D obj = mParser.getParsedObject();
			//obj.addLight(mLight);
			if (p.getModels().get(i).isDoubleSided()) {
				obj.setDoubleSided(true);
			}
			obj.setDoubleSided(true);
			
			obj.setDepthMaskEnabled(true);
			obj.setVisible(false);
			obj.setMaterial(new SimpleMaterial());
			//obj.setColor(new Number3D(1f, 0.5f, 0.5f));
			obj.setDepthTestEnabled(true);
			Bitmap mBM = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory()+ "/"+ p.getModels().get(i).getTexture());
			obj.addTexture(mTextureManager.addTexture(mBM));
			addChild(obj);
			p.getModels().get(i).obj = obj;
			/*
			 * 
			 * objs.get(i).setBlendingEnabled(true);
			 * mObj.setBlendFunc(GL10.GL_SRC_ALPHA,
			 * GL10.GL_ONE_MINUS_SRC_ALPHA);
			 */
			/*
			 * mObj.setMaterial(new BumpmapPhongMaterial());
			 * mBM=BitmapFactory.decodeFile("/sdcard/VeniceViewer/mesto_png");
			 * mObj
			 * .addTexture(mTextureManager.addTexture(mBM,TextureType.DIFFUSE));
			 * mObj
			 * .addTexture(mTextureManager.addTexture(mBM,TextureType.BUMP));
			 */
			// addChild(objs.get(i));
			// addChild(mObj);
		}
		Log.d("objloader", "objects in scene:" + getNumChildren());
	}

	public void showProject(int k) {
		hideModels();
		ProjectLevel p = ps.get(k);
		for (int i = 0; i < p.getModels().size(); i++) {
			p.getModels().get(i).obj.setVisible(true);
		}

	}

	public void hideModels() {
		for (int k = 0; k < ps.size(); k++) {
			ProjectLevel p = ps.get(k);
			for (int i = 0; i < p.getModels().size(); i++) {
				p.getModels().get(i).obj.setVisible(false);
			}
		}
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		super.onSurfaceCreated(gl, config);
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {
		super.onDrawFrame(glUnused);
		/*
		 * rot += 0.1f; if (rot > 360) rot = 0; setCamLA(rot, 100);
		 */
	}

	public void setCamLA(float phi, float theta) {
		float cx = mCamera.getX();
		float cy = mCamera.getY();
		float cz = mCamera.getZ();
		float p = (float) Math.toRadians(phi);
		float t = (float) Math.toRadians(theta);
		float sinPhi = (float) (Math.round(Math.sin(p) * 1000)) / 1000;
		float cosPhi = (float) (Math.round(Math.cos(p) * 1000)) / 1000;
		float sinTheta = (float) (Math.round(Math.sin(t) * 1000)) / 1000;
		float cosTheta = (float) (Math.round(Math.cos(t) * 1000)) / 1000;
		float ay = cosTheta;
		float ax = sinPhi * sinTheta;
		float az = cosPhi * sinTheta;
		mCamera.setLookAt(cx - ax, cy + ay, cz - az);
	}
}
