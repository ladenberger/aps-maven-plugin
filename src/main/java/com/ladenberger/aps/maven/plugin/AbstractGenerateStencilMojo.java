package com.ladenberger.aps.maven.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.Scanner;

public abstract class AbstractGenerateStencilMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true)
	protected MavenProject project;

	@Parameter(defaultValue = "${basedir}/src/main/templates/", required = true)
	protected File sourceDir;

	@Parameter
	protected List<CustomStencil> stencils;

	// Local fields below this point
	protected String[] includes;
	protected String[] excludes;

	protected List<File> findFiles() {
		final DirectoryScanner scanner = new DirectoryScanner();
		scanner.setBasedir(sourceDir);
		return findFiles(scanner);
	}

	protected List<File> findFiles(final Scanner scanner) {
		final List<File> results = new ArrayList<File>();
		if (includes != null && includes.length > 0) {
			scanner.setIncludes(includes);
		}
		if (excludes != null && excludes.length > 0) {
			scanner.setExcludes(excludes);
		}
		scanner.addDefaultExcludes();
		scanner.scan();
		for (final String name : scanner.getIncludedFiles()) {
			results.add(new File(scanner.getBasedir(), name));
		}
		return results;
	}

}
