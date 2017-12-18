package com.ladenberger.aps.maven.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Mojo(name = "stencil-json", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class GenerateStencilJsonMojo extends AbstractGenerateStencilMojo {

	@Parameter(defaultValue = "${basedir}/app", required = true)
	private String appPath;

	@Parameter(required = true)
	private String stencilsName;

	List<String> filesListInDir = new ArrayList<String>();

	private static String TARGET_FOLDER_NAME = "aps-app";

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		File appFolder = new File(appPath);

		if (!appFolder.exists()) {
			throw new MojoExecutionException("Application folder '" + appPath + "' does not exist");
		}

		// Create target folder structure, i.e. copy APS to target folder
		String targetAppFolderPath = project.getBuild().getDirectory() + File.separator + TARGET_FOLDER_NAME;
		File targetAppResourcesFolder = new File(targetAppFolderPath + File.separator + "resources");
		try {
			FileUtils.copyDirectory(appFolder, targetAppResourcesFolder);
		} catch (IOException e) {
			throw new MojoExecutionException("An error occurred while creating target build structure", e);
		}

		File jsonStencilFile = findStencilJsonFile(targetAppResourcesFolder);

		// Collect stencil templates and controllers
		Map<String, String> stencilTemplateMap = new HashMap<>();
		Map<String, String> stencilControllerMap = new HashMap<>();

		for (CustomStencil customStencil : stencils) {

			File stencilTemplateFile = new File(dynamicStencilsFolder + File.separator + customStencil.getName()
					+ "-stencil" + File.separator + customStencil.getName() + "-directive.html");

			if (!stencilTemplateFile.exists()) {
				throw new MojoExecutionException("No stencil template file found at " + stencilTemplateFile.getPath());
			}

			File stencilControllerFile = new File(dynamicStencilsFolder + File.separator + customStencil.getName()
					+ "-stencil" + File.separator + customStencil.getName() + "-ctrl.js");

			if (!stencilControllerFile.exists()) {
				throw new MojoExecutionException(
						"No stencil controller file found at " + stencilControllerFile.getPath());
			}

			List<String> templateFileLines = null;
			try {
				templateFileLines = FileUtils.readLines(stencilTemplateFile);
			} catch (IOException ex) {
				throw new MojoExecutionException(
						"Unable to read template file: " + stencilTemplateFile.getAbsolutePath(), ex);
			}

			StringBuilder templateStrBuilder = new StringBuilder();
			for (String line : templateFileLines) {
				templateStrBuilder.append(line + "\n");
			}

			stencilTemplateMap.put(customStencil.getName(), templateStrBuilder.toString());

			List<String> controllerFileLines = null;
			try {
				controllerFileLines = FileUtils.readLines(stencilControllerFile);
			} catch (IOException ex) {
				throw new MojoExecutionException(
						"Unable to read stencil controller file: " + stencilControllerFile.getPath(), ex);
			}

			StringBuilder controllerStrBuilder = new StringBuilder();
			for (String line : controllerFileLines) {
				controllerStrBuilder.append(line + "\n");
			}

			getLog().debug(controllerStrBuilder.toString());

			stencilControllerMap.put(customStencil.getName(), controllerStrBuilder.toString());

		}
		// --------------------------

		// Modify original stencils Json file with template and controller data
		getLog().debug("Found stencils json file: " + jsonStencilFile.getPath());
		getLog().debug("Start parsing stencils json file ...");

		FileReader fileReader;

		try {
			getLog().debug("Configured stencils: " + stencils);
			fileReader = new FileReader(jsonStencilFile);
		} catch (FileNotFoundException e) {
			throw new MojoExecutionException("Could not find file at " + jsonStencilFile.getPath());
		}

		JsonParser parser = new JsonParser();
		JsonElement jsonElement = parser.parse(fileReader);
		JsonObject jsonObject = jsonElement.getAsJsonObject();

		JsonArray fieldTypes = jsonObject.getAsJsonArray("fieldTypes");

		if (fieldTypes == null) {
			throw new MojoExecutionException("Wrong Json format, expected 'fieldTypes' property");
		}

		Iterator<JsonElement> fieldTypesIterator = fieldTypes.iterator();
		while (fieldTypesIterator.hasNext()) {

			JsonObject fieldType = (JsonObject) fieldTypesIterator.next();
			String fieldTypeValue = fieldType.get("type").getAsString();
			if ("custom".equalsIgnoreCase(fieldTypeValue)) {

				String stencilType = fieldType.get("customType").getAsString();

				getLog().debug("Custom field type found ...");
				getLog().debug("Type of custom stencil: " + stencilType);

				if (stencilTemplateMap.containsKey(stencilType)) {
					getLog().debug(stencilType + " is configured as dynamic stencil ...");
					fieldType.addProperty("template", stencilTemplateMap.get(stencilType));
					getLog().debug(stencilControllerMap.get(stencilType));
					fieldType.addProperty("componentControllerCode", stencilControllerMap.get(stencilType));
				}

			}

		}

		// Write modified stencils Json file
		try (Writer writer = new FileWriter(targetAppResourcesFolder.getPath() + File.separator + "stencils"
				+ File.separator + jsonStencilFile.getName())) {
			Gson gson = new GsonBuilder().disableHtmlEscaping().create();
			gson.toJson(jsonElement, writer);
		} catch (IOException e) {
			throw new MojoExecutionException("An error occurred while writing to Json file", e);
		}

		// Zip the modified App definition data
		zipDirectory(targetAppResourcesFolder, targetAppFolderPath + File.separator + stencilsName + ".zip");

	}

	private File findStencilJsonFile(File targetAppFolder) throws MojoExecutionException {

		File stencilsFolder = new File(targetAppFolder + File.separator + "stencils");

		getLog().debug("Stencils folder path: " + stencilsFolder.getPath());

		File[] jsonStencilFiles = stencilsFolder.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(stencilsName);
			}

		});

		if (jsonStencilFiles.length == 0) {
			throw new MojoExecutionException("No Stencils Json file found at " + appPath);
		} else if (jsonStencilFiles.length > 1) {
			throw new MojoExecutionException("More than one stencils Json file found at " + appPath);
		}

		return jsonStencilFiles[0];

	}

	private void zipDirectory(File dir, String zipDirName) {
		try {
			populateFilesList(dir);
			FileOutputStream fos = new FileOutputStream(zipDirName);
			ZipOutputStream zos = new ZipOutputStream(fos);
			for (String filePath : filesListInDir) {
				ZipEntry ze = new ZipEntry(filePath.substring(dir.getAbsolutePath().length() + 1, filePath.length()));
				zos.putNextEntry(ze);
				FileInputStream fis = new FileInputStream(filePath);
				byte[] buffer = new byte[1024];
				int len;
				while ((len = fis.read(buffer)) > 0) {
					zos.write(buffer, 0, len);
				}
				zos.closeEntry();
				fis.close();
			}
			zos.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void populateFilesList(File dir) throws IOException {
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isFile())
				filesListInDir.add(file.getAbsolutePath());
			else
				populateFilesList(file);
		}
	}

}
