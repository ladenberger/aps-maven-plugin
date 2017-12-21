package com.ladenberger.aps.maven.plugin;

import com.google.gson.Gson;

public class ScriptFile {

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}

}
