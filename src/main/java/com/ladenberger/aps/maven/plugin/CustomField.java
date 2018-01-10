package com.ladenberger.aps.maven.plugin;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

public class CustomField {

	private String customType;
	
	private String title;
	
	private List<ScriptFile> scripts = new ArrayList<>();

	public String getCustomType() {
		return customType;
	}

	public void setCustomType(String customType) {
		this.customType = customType;
	}

	public List<ScriptFile> getScripts() {
		return scripts;
	}

	public void setScripts(List<ScriptFile> scripts) {
		this.scripts = scripts;
	}

	@Override
	public String toString() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

}
