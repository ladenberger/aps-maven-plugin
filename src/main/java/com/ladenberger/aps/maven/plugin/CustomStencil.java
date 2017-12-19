package com.ladenberger.aps.maven.plugin;

import java.util.ArrayList;
import java.util.List;

public class CustomStencil {

	private String name;

	private List<ScriptFile> scripts = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<ScriptFile> getScripts() {
		return scripts;
	}

	public void setScripts(List<ScriptFile> scripts) {
		this.scripts = scripts;
	}

}
