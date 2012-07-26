package com.example.veniceexplorer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

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
	private BaseObject3D mObj;
	private TextureManager mTextureManager;
	private Bitmap mBM;
	private ArrayList<BaseObject3D> objs;
	
	public VeniceExplorerRenderer(Context context) {
		super(context);
		objs=new ArrayList();
		RajLog.enableDebug(true);
		setFrameRate(60);
	}

	protected void initScene() {
		mTextureManager=new TextureManager();
		mLight = new PointLight();
		mLight.setColor(1.0f, 1.0f, 1.0f);
		mLight.setPower(5f);
		mLight.setAttenuation(500, 1, .09f, .032f);
		mLight.setPosition(0f, 0f, 0f);
		mCamera.setPosition(0, 1.4f, 0);
		mCamera.setRotation(0, 0, 0);
		mCamera.setFarPlane(1000);
		setSceneCachingEnabled(false);
		mParser = new ObjParser(this, "VeniceViewer/mesto_obj");
		mParser.parse();
		mObj=mParser.getParsedObject();
		mObj.addLight(mLight);
		addChild(mObj);
	}
	public void LoadObjects(ProjectLevel p)
	{
		clearObjs();		
		mParser = new ObjParser(this, "VeniceViewer/mesto_obj");
		mParser.parse();
		mObj=mParser.getParsedObject();
		mObj.addLight(mLight);
		mObj.setVisible(true);
		addChild(mObj);
		Log.d("objloader","objects in scene:"+getNumChildren());
		for(int i=0;i<p.getModels().size();i++)
		{
			/*
			mParser = new ObjParser(this, p.getModels().get(i).getModel());
			mParser.parse();
			mObj=mParser.getParsedObject();
			*/
			/*
			objs.add(i, mParser.getParsedObject());
			objs.get(i).addLight(mLight);
			if(p.getModels().get(i).isDoubleSided())
			{
				objs.get(i).setDoubleSided(true);
			}
			objs.get(i).setDepthMaskEnabled(true);
			objs.get(i).setVisible(true);
			objs.get(i).setMaterial(new DiffuseMaterial());
			*/
			/*
			objs.get(i).setDepthTestEnabled(true);
			objs.get(i).setBlendingEnabled(true);
			mObj.setBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			*/
			/*
			mObj.setMaterial(new BumpmapPhongMaterial());
			mBM=BitmapFactory.decodeFile("/sdcard/VeniceViewer/mesto_png");
			mObj.addTexture(mTextureManager.addTexture(mBM,TextureType.DIFFUSE));
			mObj.addTexture(mTextureManager.addTexture(mBM,TextureType.BUMP));
			*/
			//addChild(objs.get(i));
			//addChild(mObj);
		}
	}
	public void clearObjs()
	{
		//clearChildren();
		//objs.clear();
	}
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		super.onSurfaceCreated(gl, config);
	}
	public void setCameraAngle(float x, float y, float z)
	{
		//mCamera.setRotation(x, y, z);
		//mCamera.setRotY(y);
		//mCamera.setRotX(x);
	}
}
