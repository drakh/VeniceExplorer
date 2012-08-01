package com.example.veniceexplorer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
//import android.opengl.GLES20;
import android.os.Environment;
import rajawali.renderer.RajawaliRenderer;
import rajawali.util.RajLog;
import android.content.Context;
import rajawali.BaseObject3D;
import rajawali.lights.PointLight;
import rajawali.parser.ObjParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
//import rajawali.math.Number3D;
import rajawali.materials.*;
//import rajawali.materials.TextureManager.TextureType;
import java.util.ArrayList;
import android.util.Log;
import android.view.Surface;
import java.io.IOException;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.AudioManager;
import android.graphics.SurfaceTexture;
//import rajawali.primitives.*;

public class VeniceExplorerRenderer extends RajawaliRenderer implements
		OnPreparedListener, OnBufferingUpdateListener, OnCompletionListener,
		OnErrorListener {

	private MediaPlayer mMediaPlayer;
	private SurfaceTexture mTexture;

	private PointLight mLight;
	private ObjParser mParser;
	private TextureManager mTextureManager;
	private ArrayList<ProjectLevel> ps;
	//private int ActualModel;
	private boolean izLoaded;
	//private float rot = 0f;
	private TextureInfo vt;
	VideoMaterial vmaterial;

	public VeniceExplorerRenderer(Context context) {
		super(context);
		RajLog.enableDebug(true);
		setFrameRate(60);
		izLoaded = false;

	}

	protected void initScene() {
		Log.d("main", "scene init");
		mTextureManager = new TextureManager();
		setupVideoTexture();
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

	}

	public void setupVideoTexture() {

		vmaterial = new VideoMaterial();
		vt = mTextureManager.addVideoTexture();
		int textureid = vt.getTextureId();

		mTexture = new SurfaceTexture(textureid);
		mMediaPlayer = new MediaPlayer();
		mMediaPlayer.setOnPreparedListener(this);
		mMediaPlayer.setOnBufferingUpdateListener(this);
		mMediaPlayer.setOnCompletionListener(this);
		mMediaPlayer.setOnErrorListener(this);
		mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mMediaPlayer.setSurface(new Surface(mTexture));
		mMediaPlayer.setLooping(true);
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
			// obj.addLight(mLight);
			obj.setDepthMaskEnabled(true);
			obj.setVisible(false);
			obj.setDepthTestEnabled(true);
			obj.setBlendingEnabled(true);
			obj.setBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			if (p.getModels().get(i).isDoubleSided()) {
				obj.setDoubleSided(true);
			}
			if (p.getModels().get(i).isVideo()) {
				Log.d("isvideo","yeees");
				obj.setMaterial(vmaterial);
				obj.addTexture(vt);
				//Log.d(tag, msg)
			} else {
				obj.setMaterial(new SimpleMaterial());
				Bitmap mBM = BitmapFactory.decodeFile(Environment
						.getExternalStorageDirectory()
						+ "/"
						+ p.getModels().get(i).getTexture());
				obj.addTexture(mTextureManager.addTexture(mBM));
			}
			addChild(obj);
			p.getModels().get(i).obj = obj;
		}
		Log.d("objloader", "objects in scene:" + getNumChildren());
	}

	public void showProject(int k) {
		mMediaPlayer.stop();
		hideModels();
		ProjectLevel p = ps.get(k);
		for (int i = 0; i < p.getModels().size(); i++) {
			p.getModels().get(i).obj.setVisible(true);
			if (p.getModels().get(i).isVideo()) {
				try {

					mMediaPlayer.setDataSource(Environment
							.getExternalStorageDirectory()
							+ "/"
							+ p.getModels().get(i).getTexture());
					mMediaPlayer.prepareAsync();
					Log.d("video","loading");
				} catch (IOException e) {
					Log.d("video","not loaded");
				}
			}
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
		mTexture.updateTexImage();
		super.onDrawFrame(glUnused);
	}

	public void setCamLA(float ax, float ay, float az) {
		float cx = mCamera.getX();
		float cy = mCamera.getY();
		float cz = mCamera.getZ();
		mCamera.setLookAt(cx - ax, cy + ay, cz - az);
	}

	public void onBufferingUpdate(MediaPlayer arg0, int arg1) {
	}

	public void onPrepared(MediaPlayer mediaplayer) {
		mMediaPlayer.start();
	}

	public void onCompletion(MediaPlayer arg0) {
	}

	public boolean onError(MediaPlayer mp, int what, int extra) {
		return false;
	}

	public void onSurfaceDestroyed() {
		mMediaPlayer.release();
		super.onSurfaceDestroyed();
	}
}
