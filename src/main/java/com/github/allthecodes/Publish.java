package com.github.allthecodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.pegdown.PegDownProcessor;

@Mojo( name = "publish")
public class Publish extends AbstractMojo {

	@Parameter
	private String markdownFile;

	@Parameter
	private String sitesPublishLocation;

	@Parameter
	private String sitesDomainName = "site";

	@Parameter
	private String sitesSiteName = "";

	@Parameter
	private String pageTitle = "untitled";

	@Parameter
	private String pageName = "untitled";

	public void execute() throws MojoExecutionException
	{
	    getLog().info( "===== Publishing to Google Sites =====" );
	    getLog().info("Using markdown file: " + markdownFile);
	    getLog().info("Publishing to here: " + sitesPublishLocation);

	    String rawContent = "";

	    try
			{
				rawContent = new String(Files.readAllBytes(Paths.get(markdownFile)));
			}
			catch (IOException e)
			{
				throw new RuntimeException("Unable to open markdown File", e);
			}

	    if (rawContent.isEmpty())
	    	throw new RuntimeException("Markdown file was empty");

	    GoogleSitesClient sitesClient = new GoogleSitesClient();
	    sitesClient.setDomain(sitesDomainName);
	    sitesClient.setSiteName(sitesSiteName);
	    sitesClient.authenticate();

	    sitesClient.publishOrUpdate(sitesPublishLocation, pageName, pageTitle, convertMarkdownToHTML(rawContent));

	}

	private String convertMarkdownToHTML(String rawContent) {
		return new PegDownProcessor().markdownToHtml(rawContent);
	}

}
