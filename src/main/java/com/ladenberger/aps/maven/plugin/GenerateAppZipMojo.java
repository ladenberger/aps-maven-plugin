package com.ladenberger.aps.maven.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
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

@Mojo(name = "app-zip", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class GenerateAppZipMojo extends AbstractGenerateStencilMojo {

	@Parameter(defaultValue = "${basedir}/app", required = true)
	private String appPath;

	@Parameter(required = true)
	private String stencilsName;

	private List<String> filesListInDir = new ArrayList<String>();

	private Gson gson = new GsonBuilder().disableHtmlEscaping().create();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		String targetAppFolderPath = project.getBuild().getDirectory() + File.separator + TARGET_FOLDER_NAME;

		// Copy original App definition resources to target folder (prepare for
		// modification)
		File targetAppResourcesFolder = copyAppDefinitionResources(targetAppFolderPath);

		// Get JSON stencil file from App definition resources
		File jsonStencilFile = findStencilJsonFile(targetAppResourcesFolder);

		JsonArray jsonStencilArray;
		JsonElement jsonElement;

		JsonParser parser = new JsonParser();

		// Modify original stencils Json file with template and controller data
		try (FileReader fileReader = new FileReader(jsonStencilFile)) {

			jsonElement = parser.parse(fileReader);
			JsonObject jsonObject = jsonElement.getAsJsonObject();

			jsonStencilArray = jsonObject.getAsJsonArray("fieldTypes");

			if (jsonStencilArray == null) {
				throw new MojoExecutionException("Wrong Json format, expected 'fieldTypes' property");
			}

		} catch (IOException e) {
			throw new MojoExecutionException("Could not find file at " + jsonStencilFile.getPath());
		}

		InputStream customStencilJsonInputStream = getClass().getClassLoader()
				.getResourceAsStream("customStencil.json");
		JsonObject newCustomStencilJsonObject = (JsonObject) parser
				.parse(new InputStreamReader(customStencilJsonInputStream));

		for (CustomField stencil : fields) {

			JsonObject dynamicStencilObject = findDynamicStencilObject(jsonStencilArray, stencil.getCustomType());
			if (dynamicStencilObject == null) {
				dynamicStencilObject = newCustomStencilJsonObject;
				dynamicStencilObject.addProperty("customType", stencil.getCustomType());
				jsonStencilArray.add(dynamicStencilObject);
			}

			// Set new template value
			dynamicStencilObject.addProperty("template", getDirectiveData("runtime.html", stencil.getCustomType()));
			// Set new controller value
			dynamicStencilObject.addProperty("componentControllerCode",
					getDirectiveData("ctrl.js", stencil.getCustomType()));

			if (stencil.getTitle() != null) {
				dynamicStencilObject.addProperty("title", stencil.getTitle());
			} else if (!dynamicStencilObject.has("title")) {
				dynamicStencilObject.addProperty("title", stencil.getCustomType());
			}

			handleScriptFiles(stencil, dynamicStencilObject, targetAppResourcesFolder);

		}

		// Write modified stencils Json file
		try (Writer writer = new FileWriter(jsonStencilFile)) {
			gson.toJson(jsonElement, writer);
		} catch (IOException e) {
			throw new MojoExecutionException("An error occurred while writing to stencils Json file", e);
		}

		// Zip the modified App definition data
		String appDefinitionZipFilePath = targetAppFolderPath + File.separator + stencilsName + ".zip";
		zipDirectory(targetAppResourcesFolder, appDefinitionZipFilePath);

		buildContext.refresh(new File(appDefinitionZipFilePath));

	}

	private void handleScriptFiles(CustomField stencil, JsonObject dynamicStencilObject, File targetAppResourcesFolder)
			throws MojoExecutionException {

		if (stencil.getScripts().size() > 0) {

			File stencilsFolder = new File(
					targetAppResourcesFolder.getPath() + File.separator + "stencils" + File.separator + "scripts");

			if (!stencilsFolder.exists()) {
				stencilsFolder.mkdir();
			}

			JsonArray jsonScriptFilesArray = dynamicStencilObject.getAsJsonArray("scriptFiles");
			if (jsonScriptFilesArray == null) {
				jsonScriptFilesArray = new JsonArray();
				dynamicStencilObject.add("scriptFiles", jsonScriptFilesArray);
			}

			Random random = new Random();

			for (ScriptFile scriptFile : stencil.getScripts()) {

				File directiveDataFile = new File(dynamicStencilsFolder + File.separator + stencil.getCustomType()
						+ "-field" + File.separator + "scripts" + File.separator + scriptFile.getName());

				if (!directiveDataFile.exists()) {
					throw new MojoExecutionException(
							"Stencil script file '" + scriptFile.getName() + "' does not exist");
				}

				int randomInt = random.nextInt(9999);

				File destFile = new File(
						stencilsFolder.getPath() + File.separator + randomInt + "-" + scriptFile.getName());
				try {
					FileUtils.copyFile(directiveDataFile, destFile);
				} catch (IOException e) {
					throw new MojoExecutionException("Error creating file " + destFile.getPath(), e);
				}

				JsonObject scriptFileJsonObject = new JsonObject();
				scriptFileJsonObject.addProperty("id", randomInt);

				jsonScriptFilesArray.add(scriptFileJsonObject);

			}

		}

	}

	private JsonObject findDynamicStencilObject(JsonArray jsonStencilArray, String stencilName) {

		Iterator<JsonElement> fieldTypesIterator = jsonStencilArray.iterator();

		while (fieldTypesIterator.hasNext()) {

			JsonObject customStencilJsonObject = (JsonObject) fieldTypesIterator.next();

			String fieldTypeValue = customStencilJsonObject.get("type").getAsString();
			if ("custom".equalsIgnoreCase(fieldTypeValue)) {
				String stencilType = customStencilJsonObject.get("customType").getAsString();
				if (stencilType.equals(stencilName)) {
					return customStencilJsonObject;
				}
			}

		}

		return null;

	}

	private String getDirectiveData(String type, String fieldName) throws MojoExecutionException {

		File directiveDataFile = new File(dynamicStencilsFolder + File.separator + fieldName + "-field" + File.separator
				+ fieldName + "-" + type);

		if (!directiveDataFile.exists()) {
			throw new MojoExecutionException("No directive data file found at " + directiveDataFile.getPath());
		}

		List<String> templateFileLines = null;
		try {
			templateFileLines = FileUtils.readLines(directiveDataFile);
		} catch (IOException ex) {
			throw new MojoExecutionException(
					"Unable to read directive data file: " + directiveDataFile.getAbsolutePath(), ex);
		}

		StringBuilder templateStrBuilder = new StringBuilder();
		for (String line : templateFileLines) {
			templateStrBuilder.append(line + "\n");
		}

		return templateStrBuilder.toString();

	}

	private File copyAppDefinitionResources(String targetAppFolderPath) throws MojoExecutionException {

		File appFolder = new File(appPath);
		if (!appFolder.exists()) {
			throw new MojoExecutionException("Application folder '" + appPath + "' does not exist");
		}

		// Create target folder structure, i.e. copy APS to target folder
		File targetAppResourcesFolder = new File(targetAppFolderPath + File.separator + "resources");

		if (targetAppResourcesFolder.exists()) {
			targetAppResourcesFolder.delete();
		}

		try {
			FileUtils.copyDirectory(appFolder, targetAppResourcesFolder);
		} catch (IOException e) {
			throw new MojoExecutionException("An error occurred while creating target build structure", e);
		}

		return targetAppResourcesFolder;

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
