package com.example.veniceexplorer;

import java.util.ArrayList;

public class InteractiveAction
{
	private String				ActionName;
	private ArrayList<String>	textures;
	private ArrayList<String>	onleave;
	private ArrayList<String>	onenter;
	private ArrayList<String>	oncross;

	InteractiveAction(String n)
	{
		ActionName = n;
		textures = new ArrayList<String>();
	}

	public void onEnter()
	{

	}

	public void onLeave()
	{

	}

	public void onCross()
	{

	}

	public void addTexture(String texture)
	{
		textures.add(texture);
	}
}
