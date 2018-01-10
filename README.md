# APS (Alfresco Process Services) Maven Plugin

Plugin with useful tools and helper for the development of custom stencil fields for APS (Alfresco Process Services).

## Example Plugin Configuration (Maven)

```
	<build>
		<plugins>
			<plugin>
				<groupId>com.ladenberger</groupId>
				<artifactId>aps-maven-plugin</artifactId>
				<version>0.0.1</version>
				<executions>
					<execution>
						<id>prepare-stencils</id>
						<goals>
							<goal>stencil-templates</goal>
						</goals>
					</execution>
					<execution>
						<id>compile-stencils</id>
						<goals>
							<goal>app-zip</goal>
						</goals>
					</execution>
				</executions>
				<configuration>
					<goalPrefix>aps</goalPrefix>
					<stencilsName>MyCustomStencils</stencilsName>
					<fields>
						<field>
							<customType>notes</customType>
							<title>Notes Form Field</title>
						</field>
						<field>
							<customType>signature</customType>
							<title>Signature Form Field</title>
							<scripts>
								<script>
									<name>signature_pad.js</name>
								</script>
							</scripts>
						</field>
					</fields>
				</configuration>
			</plugin>
		...
```

The plugin will look for the following custom stencil field template files in your Maven WAR project based on the configuration shown above:

```
src
-main
--webapp
---workflow
----dynamic-stencils
-----notes-field
------notes-runtime.html
------notes-ctrl.js
-----signature-field
------signature-runtime.html
------signature-ctrl.js
------scripts
-------signature_pad.js
```

For each custom stencil field a new subfolder called *[customType]-field* should be created in the project folder *src/main/webapp/workflow/dynamic-stencils*. In the *[customType]-field* folder at least the following two template files should be created:
	
* [customType]-runtime.html: Defines the form runtime template
* [customType]-ctrl.js: Defines the custom component controller

Optionally, you can include additional JavaScript snippets or libraries. For this, just place the JavaScript files in a subfolder *scripts* and configure them as shown in the example above (see *signature_pad.js*).

## Example Plugin Configuration (APS)

For developing custom stencil fields using this plugin, a minimal configuration is needed in APS. For each configured custom stencil field, you need to create a new custom stencil field using the APS stencils editor. Check the official Alfresco APS documentation https://docs.alfresco.com/process-services1.7/topics/custom_form_fields.html for more information about custom stencils.

In the form field configuration view in the APS stencils editor, select a name for your custom stencil field and set the following code snippet as the *form runtime template*:

```
<dynamic-stencil stencil="[customType]"></dynamic-stencil>
```

where, *[customType]* is the type of the configured custom stencil field (e.g. *notes* and *signature*).

That's all! Just include the custom stencil fields in a form and a user task respectively in your APS development environment and start some processes and tasks.
	
### Manual Update of APS Stencil Webresources

Before your custom stencil field template files are applied in the APS development environment, you need to genereate the APS stencil webresources based on your custom stencil template files (*[customType]-runtime.html* and *[customType]-ctrl.js*).

For this, execute the maven goal:

```
mvn aps:stencil-templates
```

### Auto Update of APS Stencil Webresources (Eclipse)

Add this snippet to your pom.xml to enable generating/updating the APS stencil webresources automatically whenever you add/change/delete a custom stencil field template file (e.g. *[customType]-runtime.html* and *[customType]-ctrl.js*). The plugin is designed to be incremental build aware. 

```
	<build>
		<pluginManagement>
			<plugins>
				<plugin>
					<groupId>org.eclipse.m2e</groupId>
					<artifactId>lifecycle-mapping</artifactId>
					<version>1.0.0</version>
					<configuration>
						<lifecycleMappingMetadata>
							<pluginExecutions>
								<pluginExecution>
									<pluginExecutionFilter>
										<groupId>com.ladenberger</groupId>
										<artifactId>aps-maven-plugin</artifactId>
										<versionRange>[0.0.1,)</versionRange>
										<goals>
											<goal>stencil-templates</goal>
										</goals>
									</pluginExecutionFilter>
									<action>
										<execute>
											<runOnIncremental>true</runOnIncremental>
										</execute>
									</action>
								</pluginExecution>
							</pluginExecutions>
						</lifecycleMappingMetadata>
					</configuration>
				</plugin>
			...	
```

## From Development to Production

Although this plugin will support you developing custom APS stencils, the produced APS stencil webresources are insufficient for the production environment. This is becuse the runtime template and controller script is "outsourced" (located in external files). So, the out-of-the-box versioning of custom stencils in APS is not working anymore. To overcome this challenge, this plugin provides the goal *app-zip* that takes your current app definition and generates the stencil definition with inline code. Just as you would develop your custom stencil fields using the APS stencils editor.

Before you can use this plugin feature, you will need (1) to export your app definition, (2) unzip the exported app definition zip file, and (3) put the unzipped files into a new folder called *app* in your Maven WAR project.

As a next step you need to configure the name of your custom stencil set which you have defined in the APS stencils editor. Set the name in the *stencilsName* property as demonstrated in the example plugin configuration above.

Now, run the following goal in your project:

```
mvn aps:app-zip
```

The goal should generate a new app definition zip file located at *target/aps-app/[stencilsName].zip*. This app definition zip file can be used in your production environment.

## Example Project

An example project is available in the repository https://github.com/ladenberger/aps-stencils-devcon.
