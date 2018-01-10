package com.ladenberger.aps.maven.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

public abstract class AbstractGenerateStencilMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true)
	protected MavenProject project;

	@Parameter
	protected List<CustomField> fields = new ArrayList<CustomField>();

	@Parameter(defaultValue = "${basedir}/src/main/webapp/workflow/dynamic-stencils", readonly = true)
	protected File dynamicStencilsFolder;

	protected static String TARGET_FOLDER_NAME = "aps-app";

	@Component(role = org.sonatype.plexus.build.incremental.BuildContext.class)
	protected BuildContext buildContext;

}
