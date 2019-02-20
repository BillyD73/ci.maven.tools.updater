# ci.maven.tools.updater
Tool to automate update process of ci.maven.tools artifacts

Steps to update ci.maven.tools

1. Obtain the latest date from https://search.maven.org/search?q=com.ibm.websphere.appserver.api
2. Update 'CiMavenToolsUpdater.java' - newVersion, date
3. Run 'CiMavenToolsUpdater.java' - this will update pom.xml files under 'ci.maven.tools' folder
4. Run Step #9 in the wiki page below. i.e. mvn clean deploy  
   Wiki Page : https://apps.na.collabserv.com/wikis/home?lang=en-us#!/wiki/Wadb7253d6d74_4520_94f1_4860bfae905d/page/Tools%20artifacts%20updates
5. Check staging from https://oss.sonatype.org/#stagingRepositories
6. Remove unnecessary folders from staging - follow Step #14 on the Wiki page above
7. Close the staging repository
8. Test WDT with the staging repository
9. Update 'ci.maven.tools' and create a PR. e.g. https://github.com/WASdev/ci.maven.tools/pull/33
10. Once the PR has reviewed and approved and merged hit the Release button on the repository in the Nexus Repository Manager.
11. Check the release version from https://search.maven.org/search?q=g:net.wasdev.maven.tools.targets

