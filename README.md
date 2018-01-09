# Alfresco Process Services Maven Plugin

## Sample Plugin Configuration

```
...
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
