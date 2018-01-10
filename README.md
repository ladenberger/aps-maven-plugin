# Alfresco Process Services Maven Plugin

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
							<name>notes</name>
						</field>
						<field>
							<name>signature</name>
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

For each custom stencil field a new subfolder called *[field name]-field* should be created in the project folder *src/main/webapp/workflow/dynamic-stencils*. In the *[field name]-field* folder at least the following two template files should be created:
	
* [field name]-runtime.html: Defines the form runtime template
* [field name]-ctrl.js: Defines the custom component controller

Optionally, you can include additional JavaScript snippets or libraries. For this, just place the JavaScript files in a subfolder *scripts* and configure them as shown in the example above (see *signature_pad.js*).

## Example Plugin Configuration (APS)

For developing custom stencil fields using this plugin, a minimal configuration is needed in APS. For each configured custom stencil field, you need to create a new custom stencil field using the APS stencils editor. Check the official Alfresco APS documentation https://docs.alfresco.com/process-services1.7/topics/custom_form_fields.html for more information about custom stencils.

In the form field configuration view in the APS stencils editor, select a name for your custom stencil field and set the following code snippet as the *form runtime template*:

```
<dynamic-stencil stencil="[field name]"></dynamic-stencil>
```

where, *[field name]* is the name of the configured custom stencil field (e.g. *notes* and *signature*).

That's all! Just include the custom stencil fields in a form and a user task respectively in your APS development environment and start some processes and tasks.
	
### Manual Update of APS Stencil Webresources

Before your custom stencil field template files are applied in the APS development environment, you need to genereate the APS stencil webresources based on your custom stencil template files (*[field name]-runtime.html* and *[field name]-ctrl.js*).

For this, execute the maven goal:

```
mvn aps:stencil-templates
```

### Auto Update of APS Stencil Webresources (Eclipse)

Add this snippet to your pom.xml to enable generating/updating the APS stencil webresources automatically whenever you add/change/delete a custom stencil field template file (e.g. *[field name]-runtime.html* and *[field name]-ctrl.js*). The plugin is designed to be incremental build aware. 

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

Although this plugin will support you developing custom APS stencils, the produced APS stencil webresources are insufficient for production.
