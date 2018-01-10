# Alfresco Process Services Maven Plugin

Plugin with useful tools and helper for the development of custom stencils for APS (Alfresco Process Services).

## Sample Plugin Configuration

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

The plugin will look for the following files in your Maven WAR project based on the configuration shown above:

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

For each custom stencil field a new subfolder called *<stencil name>-field* should be created in the project folder *src/main/webapp/workflow/dynamic-stencils*. In the *<stencil-name>-field* folder at least the following two files should be created:
	
* <stencil name>-runtime.html: Defines the form runtime template
* <stencil name>-ctrl.js: Defines the custom component controller
	

## Auto Update in Eclipse

Add this snippet to your pom.xml to enable updating the dynamic stencil template file automatically whenever you add/change/delete a template (design or controller). The plugin is designed to be incremental build aware. 

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
