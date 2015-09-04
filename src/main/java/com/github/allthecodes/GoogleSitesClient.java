package com.github.allthecodes;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.gdata.client.sites.ContentQuery;
import com.google.gdata.client.sites.SitesService;
import com.google.gdata.data.Link;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.XhtmlTextConstruct;
import com.google.gdata.data.sites.BaseContentEntry;
import com.google.gdata.data.sites.ContentFeed;
import com.google.gdata.data.sites.PageName;
import com.google.gdata.data.sites.SitesLink;
import com.google.gdata.data.sites.WebPageEntry;
import com.google.gdata.util.ServiceException;

public class GoogleSitesClient {

	/** Application name. */
	private static final String APPLICATION_NAME = "Google Sites Maven Publish Plugin";

	/** Directory to store user credentials for this application. */
	private static final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"),
			".credentials/publish-plugin");

	/** Global instance of the {@link FileDataStoreFactory}. */
	private static FileDataStoreFactory DATA_STORE_FACTORY;

	/** Global instance of the JSON factory. */
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	/** Global instance of the HTTP transport. */
	private static HttpTransport HTTP_TRANSPORT;

	/** Global instance of the scopes required by this quickstart. */
	private static final List<String> SCOPES = Arrays.asList("https://sites.google.com/feeds/");

	static {
		try {
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}



	private SitesService sitesService = null;

	private String domain;

	private String siteName;

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getSiteName() {
		return siteName;
	}

	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}

	public String buildContentFeedUrl(String domain, String siteName) {
		return "https://sites.google.com/feeds/content/" + domain + "/" + siteName + "/";
	}

	public Credential getCredentials() {

		// Load client secrets.
		InputStream in = GoogleSitesClient.class.getResourceAsStream("client_secret.json");
		GoogleClientSecrets clientSecrets;
		try {
			clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));


			GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
					clientSecrets, SCOPES).setDataStoreFactory(DATA_STORE_FACTORY).setAccessType("offline").build();
			Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver())
					.authorize("user");

			return credential;
		} catch (NullPointerException | IOException e) {
			throw new RuntimeException("Unable to open Client Secret File - Cannot proceed", e);
		}

	}

	public SitesService authenticate() {
		SitesService sites = new SitesService("Google Sites Maven Publish Plugin");
		sites.setOAuth2Credentials(getCredentials());
		this.sitesService = sites;
		return sites;
	}

	public void publishOrUpdate(String sitesPublishLocation, String sitePageName, String siteTitle, String content) {

		if (updatePageIfExists(sitesPublishLocation, sitePageName, siteTitle, content)) return;
		
		ContentFeed feed = getContentFeedForPublishLocation(sitesPublishLocation);

		
		if (feed.getEntries().size() == 0) {
			throw new RuntimeException("Parent page " + sitesPublishLocation + " was not found.  Please check config.");
		}
		
		BaseContentEntry parentEntry = feed.getEntries().get(0);

		WebPageEntry entry = new WebPageEntry();

		entry.setTitle(new PlainTextConstruct(siteTitle));
		entry.setPageName(new PageName(sitePageName));
		setContentBlob(entry, content);

		entry.addLink(SitesLink.Rel.PARENT, Link.Type.ATOM, parentEntry.getSelfLink().getHref());

		try {
			sitesService.insert(new URL(buildContentFeedUrl(domain,siteName)), entry);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} catch (ServiceException e) {
			throw new RuntimeException(e);
		}

	}

	private boolean updatePageIfExists(String sitesPublishLocation, String sitePageName, String siteTitle,
			String content) {
		
		ContentFeed feed = getContentFeedForPublishLocation(sitesPublishLocation + "/" + sitePageName);

		if (feed.getEntries().size() != 1)
			return false;
		
		BaseContentEntry bce = feed.getEntries().get(0);
		
		try {
			bce.setTitle(new PlainTextConstruct(siteTitle));
			setContentBlob(bce, content);
			bce.update();
			return true;
		} catch (Exception e) {
				throw new RuntimeException("Unable to update Google Sites page ", e);
		}
		
	}

	private void setContentBlob(BaseContentEntry<?> entry, String pageContent) {
		com.google.gdata.util.XmlBlob xml = new com.google.gdata.util.XmlBlob();
		xml.setBlob(pageContent);
		entry.setContent(new XhtmlTextConstruct(xml));
	}

	private ContentFeed getContentFeedForPublishLocation(String sitesPublishLocation) {
		try {
			ContentQuery query = new ContentQuery(new URL(buildContentFeedUrl(domain, siteName)));
			query.setPath(sitesPublishLocation);
			return sitesService.getFeed(query, ContentFeed.class);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

}
