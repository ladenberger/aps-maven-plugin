package com.ladenberger.aps.maven.plugin;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public abstract class AbstractGenerateStencilMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true)
	protected MavenProject project;

	@Parameter
	protected List<CustomStencil> stencils;

	@Parameter(defaultValue = "${basedir}/src/main/webapp/workflow/dynamic-stencils", readonly = true)
	protected File dynamicStencilsFolder;

	protected static String TARGET_FOLDER_NAME = "aps-app";

}
