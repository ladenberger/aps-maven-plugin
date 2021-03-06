package com.ladenberger.aps.maven.plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

@Mojo(name = "stencil-templates", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class GenerateStencilTemplatesMojo extends AbstractGenerateStencilMojo {

	@Parameter(defaultValue = "activitiApp", required = true)
	private String moduleName;

	@Parameter(defaultValue = "${basedir}/src/main/webapp/scripts/dynamic-stencils/templates.js", readonly = true)
	private File templateTargetFile;

	@Parameter(defaultValue = "${basedir}/src/main/webapp/scripts/dynamic-stencils/dynamicStencil.js", readonly = true)
	private File dynamicDirectiveFile;

	@Parameter(defaultValue = "${basedir}/src/main/webapp/scripts/app-cfg.js", readonly = true)
	private File appConfigurationFile;

	/** @see org.apache.maven.plugin.Mojo#execute() */
	@Override
	public void execute() throws MojoExecutionException {

		long start = System.currentTimeMillis();

		try {

			File targetFolder = new File(project.getBuild().getDirectory());
			if (!targetFolder.exists()) {
				targetFolder.mkdir();
			}

			boolean stencilsChanged = stencilsChanged();

			if (isBuildNeededDynamicStencilDirectiveFile()) {
				createDynamicStencilDirectiveFile();
			}

			if (isBuildNeededAppConfigurationFile() || stencilsChanged) {
				createAppConfigurationFile();
			}

			if (isBuildNeededTemplatesFile() || stencilsChanged) {
				createTemplatesFile();
			}

		} finally {
			getLog().debug("Goal took " + (System.currentTimeMillis() - start) + "ms");
		}

	}

	private boolean isBuildNeededDynamicStencilDirectiveFile() {
		return !dynamicDirectiveFile.exists() || !buildContext.isIncremental();
	}

	private boolean isBuildNeededAppConfigurationFile() throws MojoExecutionException {
		return !appConfigurationFile.exists() || !buildContext.isIncremental();
	}

	private boolean isBuildNeededTemplatesFile() throws MojoExecutionException {

		if (!buildContext.isIncremental()) {
			// always needed if we're not doing an incremental build
			return true;
		}

		// ensure the target exists
		if (!templateTargetFile.exists()) {
			return true;
		}

		// determine the last modified template
		long lastModified = 0;
		File lastModifiedFile = null;
		for (File templateFile : findTemplateFiles()) {
			if (templateFile.lastModified() > lastModified) {
				lastModifiedFile = templateFile;
				lastModified = templateFile.lastModified();
			}
		}

		// check if the target is as recent as the last modified template
		if (lastModifiedFile != null && !buildContext.isUptodate(templateTargetFile, lastModifiedFile)) {
			return true;
		}

		return false;

	}

	private boolean stencilsChanged() throws MojoExecutionException {

		boolean changed = false;

		String apsTargetFolderPath = project.getBuild().getDirectory() + File.separator + TARGET_FOLDER_NAME;
		Gson gson = new GsonBuilder().disableHtmlEscaping().create();

		File stencilsStateFile = new File(apsTargetFolderPath + File.separator + "dynamic-stencils.json");
		if (stencilsStateFile.exists()) {

			try (FileReader fileReader = new FileReader(stencilsStateFile)) {

				Type listType = new TypeToken<ArrayList<CustomField>>() {
				}.getType();

				List<CustomField> oldStencilState = gson.fromJson(fileReader, listType);

				changed = !oldStencilState.toString().equals(fields.toString());

			} catch (IOException e) {
				throw new MojoExecutionException(
						"Could not find stencils state file at " + stencilsStateFile.getPath());
			}

		}

		if (changed || !stencilsStateFile.exists()) {

			// Check if target/aps-app exists
			if (!stencilsStateFile.getParentFile().exists()) {
				stencilsStateFile.getParentFile().mkdir();
			}

			try {
				stencilsStateFile.createNewFile();
			} catch (IOException e) {
				throw new MojoExecutionException(
						"An error occurred while creating stencils state file at " + stencilsStateFile.getPath(), e);
			}

			try (Writer writer = new FileWriter(stencilsStateFile)) {
				gson.toJson(fields, writer);
			} catch (IOException e) {
				throw new MojoExecutionException(
						"An error occurred while writing to stencils state file at " + stencilsStateFile.getPath(), e);
			}

		}

		return changed;

	}

	private void createDynamicStencilDirectiveFile() throws MojoExecutionException {

		InputStream dynamicStencilJsInputStream = getClass().getClassLoader().getResourceAsStream("dynamicStencil.js");
		try {
			FileUtils.copyInputStreamToFile(dynamicStencilJsInputStream, dynamicDirectiveFile);
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to write dynamic stencil directive file", e);
		}

	}

	private void createAppConfigurationFile() throws MojoExecutionException {

		List<String> appConfigurationFileLines;
		try {
			appConfigurationFileLines = IOUtils
					.readLines(getClass().getClassLoader().getResourceAsStream("app-cfg.js"));
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to read app-cfg.js file", e);
		}

		for (CustomField stencil : fields) {

			for (ScriptFile scriptFile : stencil.getScripts()) {

				appConfigurationFileLines.add("ACTIVITI.CONFIG.resources['workflow'].push({");
				appConfigurationFileLines.add("\t'tag' : 'script',");
				appConfigurationFileLines.add("\t'type' : 'text/javascript',");
				appConfigurationFileLines
						.add("\t'src' : ACTIVITI.CONFIG.webContextRoot + '/workflow/dynamic-stencils/'");
				appConfigurationFileLines.add(
						"\t\t+ '" + stencil.getCustomType() + "-field/scripts/" + scriptFile.getName() + "?v=1.0'");
				appConfigurationFileLines.add("});");

			}

		}

		appConfigurationFileLines.set(35, "var customFields = ["
				+ fields.stream().map(cs -> "'" + cs.getCustomType() + "'").collect(Collectors.joining(",")) + "]");

		try {
			FileUtils.writeLines(appConfigurationFile, appConfigurationFileLines);
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to write app-cfg.js file", e);
		}

		buildContext.refresh(appConfigurationFile);

	}

	private void createTemplatesFile() throws MojoExecutionException {

		List<String> lines = new ArrayList<>();

		lines.add("angular.module('" + moduleName + "').run(['$templateCache', function($templateCache) {");

		for (CustomField customStencil : fields) {

			File stencilTemplateFile = new File(
					dynamicStencilsFolder.getPath() + File.separator + customStencil.getCustomType() + "-field"
							+ File.separator + customStencil.getCustomType() + "-runtime.html");

			if (!stencilTemplateFile.exists()) {
				throw new MojoExecutionException("No stencil template file found at " + stencilTemplateFile.getPath());
			}

			List<String> fileLines = null;
			try {
				fileLines = FileUtils.readLines(stencilTemplateFile);
			} catch (IOException ex) {
				throw new MojoExecutionException(
						"Unable to read template file: " + stencilTemplateFile.getAbsolutePath(), ex);
			}
			if (fileLines.isEmpty()) {
				lines.add("\t$templateCache.put('" + customStencil.getCustomType() + "', \"\");");
			} else {
				lines.add("\t$templateCache.put('" + customStencil.getCustomType() + "',");
				for (String line : fileLines) {
					lines.add("\t\"" + line.replace("\\", "\\\\").replace("\"", "\\\"") + "\\n\" +");
				}
				lines.set(lines.size() - 1, StringUtils.chomp(lines.get(lines.size() - 1), "\\n\" +") + "\");");
			}

		}

		lines.add("}]);");

		// finally emit the output file
		try {
			getLog().debug("Writing output file: " + templateTargetFile.getAbsolutePath());
			FileUtils.writeLines(templateTargetFile, lines);
		} catch (final IOException ex) {
			throw new MojoExecutionException("Unable to write output file: " + templateTargetFile.getAbsolutePath(),
					ex);
		}

		buildContext.refresh(templateTargetFile);

	}

	private List<File> findTemplateFiles() throws MojoExecutionException {

		List<File> templateFiles = new ArrayList<>();

		for (CustomField customStencil : fields) {
			File stencilTemplateFile = new File(dynamicStencilsFolder + File.separator + customStencil.getCustomType()
					+ "-field" + File.separator + customStencil.getCustomType() + "-runtime.html");
			if (!stencilTemplateFile.exists()) {
				throw new MojoExecutionException("No stencil template file found at " + stencilTemplateFile.getPath());
			}
			templateFiles.add(stencilTemplateFile);
		}

		return templateFiles;

	}

}
