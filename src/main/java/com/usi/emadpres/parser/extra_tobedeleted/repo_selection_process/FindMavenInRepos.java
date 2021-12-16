package com.usi.emadpres.parser.extra_tobedeleted.repo_selection_process;

import com.usi.emadpres.MavenUtils.ds.MavenLibInfo;
import com.usi.emadpres.parser.parser.db.PackagesDeclarationDB;
import com.usi.emadpres.parser.parser.ds.PackageDeclarationInfo;
import com.usi.emadpres.parser.extra_tobedeleted.MavenLibUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

/**
 * Given:
 *  - <path-to-finalReleases.csv>: Maven library identifiers (groupId:artifiactId:version) of our interests
 *  - <path-to-maven-packages.db>: Packages defined in a set of Maven libraries's source code
 *  - <path-to-repo-packages.db>: Packages defined in a set of repositories:
 *
 *  Perform:
 *  - Read all three files
 *  - For each Maven library L (in first parameter):
 *      - skip it if it's not the latest version of its groupId:artifact in this machine
 *      - otherwise: define `packages_of_curLibWhichIsLatestVersion`
 *      - Loop over repositories and find those whose packages contain all of `packages_of_curLibWhichIsLatestVersion`
 *          which means we found this Maven library on GitHub
 *  Result:
 *  - Print list of repos which are a Maven library in fact.
 *
 */
public class FindMavenInRepos {
    private static final Logger logger = LoggerFactory.getLogger(FindMavenInRepos.class.getName());

    public static void main(String[] args) {
        if(args.length!=3)
        {
            System.err.println("FindMavenInRepos <path-to-finalReleases.csv> <path-to-maven-packages.db> <path-to-repo-packages.db>");
            return;
        }

        ArrayList<MavenLibInfo> mavenLibInfos = MavenLibUtils.loadMavenInfo(args[0]);
        Map<String/*Lib Identifier*/, Set<PackageDeclarationInfo>> libIdentifierToPackages = PackagesDeclarationDB.ReadPackagesFromSqlite_GroupByProject(Path.of(args[1]));
        Map<String/*RepoFullname*/, Set<PackageDeclarationInfo>> repoToPackages = PackagesDeclarationDB.ReadPackagesFromSqlite_GroupByProject(Path.of(args[2]));

        Map<String, List<String>> libsVersion = MavenLibUtils.giveVersionsSorted(mavenLibInfos, true, libIdentifierToPackages);



        int nMatche = 0 , nUnexpectedMissingLibPackages = 0;

        for(MavenLibInfo curLibInfo: mavenLibInfos)
        {
            String libName = curLibInfo.groupId+":"+curLibInfo.artifactId;

            if(libsVersion.containsKey(libName)==false || libsVersion.get(libName).size()==0)
                continue; /* we have no version of it on this machine*/
            if(!libsVersion.get(libName).get(0).equals(curLibInfo.version))
                continue; /* This curLibInfo doesn't refer to latest version we have on this machine*/

            Set<PackageDeclarationInfo> packages_of_curLibWhichIsLatestVersion = libIdentifierToPackages.get(curLibInfo.GetIdentifier());

            if(packages_of_curLibWhichIsLatestVersion==null) {
                nUnexpectedMissingLibPackages++;
                logger.warn("#{}: Although we have \"{}\" on this machine, we don't have its packages data. You forgot to run \"main.java.com.usi.javaapi.emad.parser.MethodInvocationInspector\"?", nUnexpectedMissingLibPackages, curLibInfo.GetIdentifier());
                // Maybe you calculated Repo/Maven Packages in a different machine. "giveVersionsSorted()" works based on the current machine and don't care about "Maven/Repo packages info" content.
                continue;
            }

            ////////////////////////////////////////////////////////////////////////////////////

            Iterator<Map.Entry<String, Set<PackageDeclarationInfo>>> repoPackIt = repoToPackages.entrySet().iterator();
            while(repoPackIt.hasNext())
            {
                Map.Entry<String, Set<PackageDeclarationInfo>> next = repoPackIt.next();
                String curRepoFullname = next.getKey();
                Set<PackageDeclarationInfo> curRepoPackages = next.getValue();
                if(curRepoPackages==null)
                    continue;

                if(curRepoPackages.containsAll(packages_of_curLibWhichIsLatestVersion)) {
                    nMatche++;
                    logger.info("MATCH #{}: {} ==> {}", nMatche, curRepoFullname, curLibInfo);
                }
//                else
//                {
//                    Set<String> temp = new HashSet<>(packages_of_curLibWhichIsLatestVersion);
//                    temp.removeAll(curRepoPackages);
//                    if(temp.size()==1)
//                        logger.info("PARTIAL_MATCH: {} ==> {} -- {}", curRepoFullname, curLibInfo, temp.iterator().next());
//                }
            }

        }

        logger.info("TOTAL: {}", mavenLibInfos.size());
        logger.info("#Libs with missing Packages Info: {}", nUnexpectedMissingLibPackages);
    }
}
