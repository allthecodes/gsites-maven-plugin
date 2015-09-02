Super simple plugin for uploading individual files to Google Sites

Usage:

Add the below to your pom.xml

```xml
  <plugins>
      <plugin>
      <groupId>com.github.allthecodes</groupId>
      <artifactId>gsites-maven-plugin</artifactId>
      <version>0.1</version>
      <configuration>
          <markdownFile>src/main/resources/my-fancy-markdown-file.md</markdownFile>
          <sitesPublishLocation>/path/to/page</sitesPublishLocation>
          <sitesDomainName>site</sitesDomainName>
          <sitesSiteName>home</sitesSiteName>
      </configuration>
      </plugin>
  </plugins>
```

And then mvn gsites:publish

Gotchas
 * This is very much a Proof of Concept...
 * You'll need a browser to authenticate with Google (just once).
 * This plugin won't create the path - you have to do that yourself
 * It currently only supports publishing one file
 * If there's a page already there - it will update it
