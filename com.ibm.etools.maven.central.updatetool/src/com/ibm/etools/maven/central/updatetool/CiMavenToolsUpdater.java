package com.ibm.etools.maven.central.updatetool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Date;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Iterator;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * 
 * This is a tool that's used to help update the https://github.com/WASdev/ci.maven.tools project.
 * 
 * For more information on the update process see the Maven tools wiki documentation:
 * https://apps.na.collabserv.com/wikis/home?lang=en-us#!/wiki/Wadb7253d6d74_4520_94f1_4860bfae905d/page/Tools%20artifacts%20updates
 *
 */
public class CiMavenToolsUpdater {
	
	public static final String LIBERTY_APIS_FOLDER = "liberty-apis";
	public static final String LIBERTY_SPIS_FOLDER = "liberty-spis";

	// User input values
	public static String ciMavenToolsFolder = "/Users/billydawson/Documents/WDT/ci.maven.tools";
	public static String newVersion = "22.0.0.11";
	public static String date = "20-Nov-2022";
	
	public static void main(String[] args) throws Exception {
		
		CiMavenToolsUpdater mtu = new CiMavenToolsUpdater();
		
		String apiPOMPath = ciMavenToolsFolder + "/targets/" + LIBERTY_APIS_FOLDER + "/pom.xml";
		String spiPOMPath = ciMavenToolsFolder + "/targets/" + LIBERTY_SPIS_FOLDER + "/pom.xml";
		
		// TODO removeAllDepdencies(model); move model up here too apiModel spiModel with each file. pass model instead of pomPath. and call updatePomFile * 4
		
		// Update APIs pom
		mtu.updatePomFile(apiPOMPath, "com.ibm.websphere.appserver.api", newVersion, date);
		//TODO updatePomFile io.openliberty etc
		//mtu.updatePomFile(apiPOMPath, "io.openliberty.api", newVersion, date);
		
		// Update SPIs pom
		mtu.updatePomFile(spiPOMPath, "com.ibm.websphere.appserver.spi", newVersion, date);
		//TODO updatePomFile io.openliberty etc
		//mtu.updatePomFile(spiPOMPath, "io.openliberty.spi", newVersion, date);
		
		System.out.println("\nThe version in the following files need to be updated manually:");
		System.out.println("targets/pom.xml");
		System.out.println("targets/liberty-target/pom.xml");
	}
	
	public void updatePomFile(String pomPath, String group, String newVersion, String date) throws Exception {
		MavenXpp3Reader reader = new MavenXpp3Reader();
		File file = new File(pomPath);
		Model model = reader.read(new FileReader(file));
		HttpURLConnection httpConnection = null;
		try{
			// Remove the old dependencies
			removeAllDependencies(model);

			// Update version to latest version (eg. 16.0.0.3 -> 16.0.0.4)
			model.setVersion(newVersion);
			
			// Update parent reference to latest version (eg. 16.0.0.3 -> 16.0.0.4)
			model.getParent().setVersion(newVersion);
			
			/* 
			 * Parse the data from Maven central and add the dependencies to the pom.xml.
			 * 
			 * Note: The tool uses the publish date to find the artifacts to include in the dependencies. This should handle
			 * the majority of the cases but if there are any artifacts that were published on a different date they'll need
			 * to be added to the file manually.
			 */ 
			addLatestDependenciesByGroup(model, group, date);
			
			//TODO if group = api addLatestDependenciesByGroup(model, "io.openliberty", date); else if spi addLatestDependenciesByGroup
			if (group.compareToIgnoreCase("com.ibm.websphere.appserver.api") == 0)
			{
				addLatestDependenciesByGroup(model, "io.openliberty.api", date);
			}
			else if (group.compareToIgnoreCase("com.ibm.websphere.appserver.spi") == 0)
			{
				addLatestDependenciesByGroup(model, "io.openliberty.spi", date);
			}
			
			MavenXpp3Writer writer = new MavenXpp3Writer();
			
			System.out.println("Updating file: " + file.getAbsolutePath());
			writer.write(new FileWriter(file), model);
			
		} finally {
			if(httpConnection != null){
				try{
					httpConnection.disconnect();
				} catch(Exception e){
					// ignore
				}
			}
		}
	}

	private void removeAllDependencies(Model model) {
		System.out.println("Removing old dependencies from model");
		Iterator<Dependency> itr = model.getDependencies().iterator();
		while(itr.hasNext()){
			itr.next();
			itr.remove();
		}
	}

	private void addLatestDependenciesByGroup(Model model, String group, String date) throws Exception{
		
		HttpURLConnection httpConnection = null;
		try{
			// Read previous release POM file
			String urlStr = "https://search.maven.org/solrsearch/select?q=g:%22" + group + "%22&rows=200&wt=json";
			URL url = new URL(urlStr);
			httpConnection = (HttpURLConnection) url.openConnection();
			JsonReader reader = Json.createReader(httpConnection.getInputStream());
			JsonArray arr = reader.readObject().getJsonObject("response").getJsonArray("docs");
			
			if (arr.size() >200)
			{
				throw new Exception("Array size exceeds 200 and max rows visible is 200. Pagination will need to be implemented to support greater than 200 items.");
			}
			
			// Add dependencies in reverse order so that they will be in alphabetical order in the pom file
			for(int i=arr.size()-1; i >= 0 ; i--){
				JsonObject o = arr.getJsonObject(i);
				
				JsonNumber timestamp = o.getJsonNumber("timestamp");
				Date d = new Date(timestamp.longValue());
				Format formatter = new SimpleDateFormat("dd-MMM-yyyy");
				String formattedDate = formatter.format(d);
				
				if(d != null && date.equals(formattedDate)){
					Dependency dep = new Dependency();

					// Set artifact id
					String artifactId = o.getString("a");
					System.out.print(artifactId);
					dep.setArtifactId(artifactId);
					
					// Set group id
					dep.setGroupId(group);
					
					// Set version
					String version = o.getString("latestVersion");
					dep.setVersion(version);
					System.out.println(" " + version);
					
					// Set type (currently only support jar)
					dep.setType("jar");
					
					// Add dependency to model
					model.addDependency(dep);
				}
			}
		} finally {
			if(httpConnection != null){
				try{
					httpConnection.disconnect();
				} catch(Exception e){
					// ignore
				}
			}
		}
	}

}
