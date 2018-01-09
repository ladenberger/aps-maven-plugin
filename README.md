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
					<stencils>
						<stencil>
							<name>notes</name>
						</stencil>
						<stencil>
							<name>signature</name>
							<scripts>
								<script>
									<name>signature_pad.js</name>
								</script>
							</scripts>
						</stencil>
					</stencils>
				</configuration>
			</plugin>
		...
```

The plugin expects 

## Auto Update in Eclipse

Add this snippet to your pom.xml to enable updating the dynamic stencil templates files automatically whenever you add/change/delete a template (design or controller). The plugin is designed to be incremental build aware. 

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

Based on the configuration, the plugin will look for the following files in your Maven WAR project:

```
src
-main
--webapp
---workflow
----dynamic-stencils
-----notes-stencil
------notes-directive.html
------notes-ctrl.js
-----signature-stencil
------signature-directive.html
------signature-ctrl.js
------scripts
-------signature_pad.js
```
