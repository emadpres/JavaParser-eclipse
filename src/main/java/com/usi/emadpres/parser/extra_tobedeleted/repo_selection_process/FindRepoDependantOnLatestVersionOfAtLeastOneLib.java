package com.usi.emadpres.parser.extra_tobedeleted.repo_selection_process;

import com.opencsv.CSVWriter;
import com.usi.emadpres.MavenUtils.ds.MavenLibInfo;
import com.usi.emadpres.MavenUtils.ds.PomInfoDB;
import com.usi.emadpres.parser.parser.db.PackagesDeclarationDB;
import com.usi.emadpres.parser.parser.ds.PackageDeclaration;
import com.usi.emadpres.parser.extra_tobedeleted.MavenLibUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Goal: Selecting repos which are dependant on latest version of at least one Maven library
 *
 * Given:
 *  - <path-to-finalReleases.csv> Maven library identifiers (groupId:artifiactId:version) of our interests
 *  - <path-to-repoPomInfo.db>  List of pom dependencies for all repositories
 *  - <path-to-maven-packages.db> Packages defined in a set of Maven libraries's source code
 * Process:
 *  - Read all inputs
 *  - For each repository R and their dependencies:
 *      - For each Maven lib in R's dependencies D:
 *          - if D is among Maven libraries of our interest and it's the latest version --> R is good
 *          - otherwise: R is bad
 *  - Return list of good R.
 */
public class FindRepoDependantOnLatestVersionOfAtLeastOneLib {
    private static final Logger logger = LoggerFactory.getLogger(FindRepoDependantOnLatestVersionOfAtLeastOneLib.class.getName());
    private static final String RESULT_PATH = "out/reposHavingADependencyWhichIsLatestVersion.txt";
    private static final String RESULT_PATH_REST = "out/reposHavingADependencyWhichIsLatestVersion_rest.txt";

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("FindMavenInRepos <path-to-finalReleases.csv> <path-to-repoPomInfo.db> <path-to-maven-packages.db>");
            return;
        }

        ArrayList<MavenLibInfo> mavenLibInfos = MavenLibUtils.loadMavenInfo(args[0]);
        //Map<String, ArrayList<MavenLibInfo>> allReposDependencies = RepoDependency.readRepoListAndDependencies(args[1]);
        Map<String, List<MavenLibInfo>> allReposDependencies = PomInfoDB.ReadSQLite_Repo2Library(args[1]);
        Map<String/*Lib Identifier*/, Set<PackageDeclaration>> libIdentifierToPackages = PackagesDeclarationDB.readAllRepoPackages(args[2]);
        Map<String, List<String>> libsVersion = MavenLibUtils.giveVersionsSorted(mavenLibInfos, true, libIdentifierToPackages);


        List<String> reposHavingAtLeastOneDependencyOnOurMavenLib_OnLatestVersion = new ArrayList<>();
        List<String> reposHavingAtLeastOneDependencyOnOurMavenLib = new ArrayList<>();

        for(String repoFullname: allReposDependencies.keySet())
        {
            List<MavenLibInfo> repoDependencies = allReposDependencies.get(repoFullname);
            boolean dependsOnLatestVersion = false;

            for(MavenLibInfo d: repoDependencies) {
                String libName = d.groupId + ":" + d.artifactId ;

                if(libsVersion.containsKey(libName)==false)
                    continue; // Probably some not-related 3rd party lib

                if (libsVersion.get(libName).get(0).equals(d.version)) {
                    dependsOnLatestVersion = true;
                    break;
                }
            }

            if(dependsOnLatestVersion)
                reposHavingAtLeastOneDependencyOnOurMavenLib_OnLatestVersion.add(repoFullname);
            else
                reposHavingAtLeastOneDependencyOnOurMavenLib.add(repoFullname);
        }

        writeReposResult(reposHavingAtLeastOneDependencyOnOurMavenLib_OnLatestVersion, RESULT_PATH);
        //writeReposResult(reposHavingAtLeastOneDependencyOnOurMavenLib, RESULT_PATH_REST);


    }

    private static void writeReposResult(List<String> reposHavingADependencyWhichIsLatestVersion, String resultPath) {
        CSVWriter writer = null;
        try {
            writer = new CSVWriter(new FileWriter(resultPath));
            String line[] = new String[1];
            for(String repoFullnam: reposHavingADependencyWhichIsLatestVersion)
            {
                line[0]=repoFullnam;
                writer.writeNext(line, false);
            }
            writer.close();

        } catch (IOException e) {
            logger.error("******Error (Critical) File Not Found: ***** {}", resultPath);
            e.printStackTrace();
        }
    }
}
