package com.ladenberger.aps.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "stencil-templates", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class GenerateStencilTemplatesMojo extends AbstractGenerateStencilMojo {

	/**
	 * The name of the overall module to use for the templates
	 */
	@Parameter(defaultValue = "activitiApp", required = true)
	private String moduleName;

	/**
	 * Prefix to put before the cache key
	 */
	@Parameter
	private String prefix;

	/**
	 * Location for the generated templates js file
	 */
	@Parameter(defaultValue = "${basedir}/src/main/webapp/scripts/dynamic-stencils/templates.js", readonly = true)
	private File templateTargetFile;

	@Parameter(defaultValue = "${basedir}/src/main/webapp/scripts/dynamic-stencils/dynamicStencil.js", readonly = true)
	private File dynamicDirectiveFile;

	@Parameter(defaultValue = "${basedir}/src/main/webapp/scripts/app-cfg.js", readonly = true)
	private File appConfigurationFile;

	@Component(role = org.sonatype.plexus.build.incremental.BuildContext.class)
	private BuildContext buildContext;

	/** @see org.apache.maven.plugin.Mojo#execute() */
	@Override
	public void execute() throws MojoExecutionException {
		long start = System.currentTimeMillis();
		try {

			prefix = prefix == null ? "" : prefix;

			if (!isBuildNeeded()) {
				getLog().debug("Nothing to do ...");
				return;
			}

			if (!templateTargetFile.getParentFile().exists()) {
				templateTargetFile.getParentFile().mkdirs();
			}

			try {
				doIt();
			} catch (final Exception e) {
				throw new MojoExecutionException("", e);
			}

		} finally {
			getLog().debug("Goal took " + (System.currentTimeMillis() - start) + "ms");
		}
	}

	/**
	 * We can skip if no files were deleted, modified or added since the last
	 * build AND the target file is still there
	 * 
	 * @return true if a build is needed, otherwise false
	 * @throws MojoExecutionException
	 */
	private boolean isBuildNeeded() throws MojoExecutionException {

		if (!buildContext.isIncremental()) {
			// always needed if we're not doing an incremental build
			getLog().debug("Full build");
			return true;
		}

		// ensure the target exists
		if (!templateTargetFile.exists() || !dynamicDirectiveFile.exists() || !appConfigurationFile.exists()) {
			getLog().debug(
					"Stencils template target file, dynamic directive file or app configuration file is missing file missing");
			return true;
		}

		// check for any deleted files
		/*
		 * List<File> deleted =
		 * findFiles(buildContext.newDeleteScanner(sourceDir)); for (File
		 * deletedFile : deleted) {
		 * getLog().info("Html2js:: detected deleted template: " +
		 * shorten(deletedFile)); }
		 * 
		 * // next check for any new/changed files List<File> changed =
		 * findFiles(buildContext.newScanner(sourceDir)); for (File changedFile
		 * : changed) {
		 * getLog().info("Html2js:: detected new/changed template: " +
		 * shorten(changedFile)); }
		 * 
		 * if (changed.size() > 0 || deleted.size() > 0) { return true; }
		 */

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
			getLog().info("Template target file was changed or is older than " + lastModifiedFile.getPath());
			return true;
		}

		return false;

	}

	private void doIt() throws Exception {

		List<String> lines = new ArrayList<>();

		lines.add("angular.module('" + moduleName + "').run(['$templateCache', function($templateCache) {");

		for (CustomStencil customStencil : stencils) {

			File stencilTemplateFile = new File(
					dynamicStencilsFolder.getPath() + File.separator + customStencil.getName() + "-stencil"
							+ File.separator + customStencil.getName() + "-directive.html");

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
				lines.add("\t$templateCache.put('" + customStencil.getName() + "', \"\");");
			} else {
				lines.add("\t$templateCache.put('" + customStencil.getName() + "',");
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

		// Write dynamic stencil directive
		if (!dynamicDirectiveFile.exists()) {

			InputStream dynamicStencilJsInputStream = getClass().getClassLoader()
					.getResourceAsStream("dynamicStencil.js");
			FileUtils.copyInputStreamToFile(dynamicStencilJsInputStream, dynamicDirectiveFile);

		}

		// Write app-cfg.js file
		if (!appConfigurationFile.exists()) {

			List<String> appConfigurationFileLines = IOUtils
					.readLines(getClass().getClassLoader().getResourceAsStream("app-cfg.js"));

			StringJoiner stringJoiner = new StringJoiner(", ", "'", "'");
			for (CustomStencil stencil : stencils) {
				stringJoiner.add(stencil.getName());
			}

			appConfigurationFileLines.set(35, "var customStencils = [" + stringJoiner.toString() + "]");

			FileUtils.writeLines(appConfigurationFile, appConfigurationFileLines);

		}

	}

	private List<File> findTemplateFiles() throws MojoExecutionException {

		List<File> templateFiles = new ArrayList<>();

		for (CustomStencil customStencil : stencils) {
			File stencilTemplateFile = new File(dynamicStencilsFolder + File.separator + customStencil.getName()
					+ "-stencil" + File.separator + customStencil.getName() + "-directive.html");
			if (!stencilTemplateFile.exists()) {
				throw new MojoExecutionException("No stencil template file found at " + stencilTemplateFile.getPath());
			}
			templateFiles.add(stencilTemplateFile);
		}

		return templateFiles;

	}

}
