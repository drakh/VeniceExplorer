package com.example.veniceexplorer;

import rajawali.BaseObject3D;
import android.util.Log;

public class ProjectObject {
	private String modelName = "";
	private String modelTexture = "";
	private int doublesided = 0;
	private int video = 0;
	public BaseObject3D obj;

	ProjectObject() {

	}

	public void setDS(String ds) {
		doublesided = Integer.parseInt(ds);
	}

	public void setVideo(String v) {
		video = Integer.parseInt(v);
		Log.d("vidijou","v: "+video);
	}

	public void setTexture(String path) {
		modelTexture = path;
	}

	public void setModel(String path) {
		modelName = path;
	}

	public String getModel() {
		return modelName;
	}

	public String getTexture() {
		return modelTexture;
	}

	public boolean isDoubleSided() {
		boolean r = false;
		if (doublesided == 1)
			r = true;
		return r;
	}

	public boolean isVideo() {
		boolean r = false;
		if (video == 1)
			r = true;
		return r;
	}

}
