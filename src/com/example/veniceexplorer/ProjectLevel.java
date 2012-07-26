package com.example.veniceexplorer;
import java.util.ArrayList;

public class ProjectLevel {
	private String projectName;
	private ArrayList<ProjectObject> objs;
	ProjectLevel(String pn) {
		projectName=pn;
		objs=new ArrayList();
	}
	public String getName()
	{
		return projectName;
	}
	public void addModel(ProjectObject o)
	{
		objs.add(o);
	}
	public ArrayList<ProjectObject> getModels()
	{
		return objs;
	}
}
