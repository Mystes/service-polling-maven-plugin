# service-polling-maven-plugin

Service Polling Maven plugin will wait until the given URL responds and then it lets the maven build process to continue.
This maven plugin is useful in integration testing where you have server processes that you need to start before tests can be run, but you don't know how long the starting process takes.

### Usage

Add the following to your pom.xml in `<pluginRepositories>` section:

```xml
    <pluginRepository>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
        <id>bintray-mystes-maven</id>
        <name>bintray-plugins</name>
        <url>http://dl.bintray.com/mystes/maven</url>
    </pluginRepository>
```

Add the following to your pom.xml in `<plugins>` section:

```xml
    <plugin>
        <groupId>fi.mystes.maven</groupId>
        <artifactId>service-polling-maven-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
        <configuration>
            <!-- Mandatory paramater -->
            <url>URL_TO_THE_SERVICE</url>

             <!-- Optional. Default value 120 seconds. -->
            <timeout>15</timeout>

            <!-- Optional. Default is 1000 milliseconds -->
            <pollingIntervalMillis>3000</pollingIntervalMillis>

            <!-- Optional. Default value is 200. -->
            <acceptedStatusCodes>
                <param>200</param>
                <param>204</param>
                <param>301</param>
                <param>302</param>
            </acceptedStatusCodes>

            <!-- Optional. Default is GET -->
            <requestType>POST</requestType>
            <skip>${maven.test.skip}</skip>
        </configuration>
        <executions>
            <execution>
                <id>start-polling</id>
                <phase>pre-integration-test</phase>
                <goals>
                    <goal>poll</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
```

If timeout occurs, the build stops.

#### Minimal example


```xml
    <plugin>
        <groupId>fi.mystes.maven</groupId>
        <artifactId>service-polling-maven-plugin</artifactId>
        <version>1.0</version>
        <configuration>
            <url>https://localhost:9443/carbon</url>
            <skip>${maven.test.skip}</skip>
        </configuration>
        <executions>
            <execution>
                <id>start-polling</id>
                <phase>pre-integration-test</phase>
                <goals>
                    <goal>poll</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
```

### Running tests
Currently only two integration tests exist. 
Run them with `mvn clean install -Prun-its`.

### Contributors
* [Jussi Mikkonen](https://github.com/jussi-mikkonen)

### [License](LICENSE)
Copyright © 2016 [Mystes Oy](https://www.mystes.fi). Licensed under the Apache 2.0 License.
