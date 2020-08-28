package com.usi.emadpres.parser.extra_tobedeleted;

import com.usi.emadpres.MavenUtils.ds.MavenLibInfo;
import com.usi.emadpres.MavenUtils.ds.PomInfoDB;
import com.usi.emadpres.parser.parser.ProjectParser;
import com.usi.emadpres.parser.parser.ds.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;



public class main_MethodInvocations {
    private static final Logger logger = LoggerFactory.getLogger(main_MethodInvocations.class.getName());


    public static void main(String[] args) {

        logger.info("start");
//        testingOneProject();
//        if(true)
//            return;

        if(args.length!=2 && args.length!=5)
        {
            System.err.println("MethodInvocationInspector -Xmx????m <path-to-repoPomInfo.db> <path-to-repos-root-dir> [<groupId> <artifactId> <version> just projects depends on this lib]");
            // <path-to-repoPomInfo.db>     ~/githubCrawler/out/repoPomInfo.db
             //<path-to-repos-root-dir>
            return;
        }

        final String PATH_TO_REPOS_INFO_DB = args[0];
        final String PATH_TO_REPOS = args[1];


        logger.info("Preparing...");
        Map<String, List<MavenLibInfo>> repoDependencies = PomInfoDB.ReadSQLite_Repo2Library(PATH_TO_REPOS_INFO_DB);
        //if(args.length==5)
        //    repoDependencies = RepoDependencyUtil.ReadRepoListAndDependencies(PATH_TO_REPOS_INFO_DB, args[2], args[3], args[4]);
        logger.info("Done");


        logger.info("Starting Extraction Invocations ...");
        ArrayList<MethodInvocationInfo> allrepos_methodInvocations = new ArrayList<>();
        Map<String, Set<PackageDeclaration>> allrepos_packageDef = new HashMap<>();
        int nError = 0;
        int index=0;
        long totalUnique_fileMethodInv_pairs = 0, totalMethodInv=0, totalPackgesCount=0;
        Iterator<Map.Entry<String, List<MavenLibInfo>>> it = repoDependencies.entrySet().iterator();
        while(it.hasNext())
        {
            index++;
            Map.Entry<String, List<MavenLibInfo>> next = it.next();
            String repoFullname = next.getKey();

            if(repoFullname.equals("gemxd/gemfirexd-oss")) //TODO
                continue;

            List<MavenLibInfo> mavenLibDependencies = next.getValue();


            String repoOwnerName = repoFullname.substring(0, repoFullname.indexOf('/'));
            String repoProjectName = repoFullname.substring(repoFullname.indexOf('/')+1);
            String pathToProject = PATH_TO_REPOS+"/"+repoOwnerName+"/"+repoProjectName;

            logger.info("#{}/{}: {}", index, repoDependencies.size(), repoFullname);



            ProjectParsingResult result = ProjectParser.ParseProject(repoFullname, null, pathToProject, mavenLibDependencies,  new ParserConfig(true, false, true ));

            if(result!=null) {
                //////////////////////// Method Invocation
                int sum=0;
                for(MethodInvocationInfo s: result.methodInvocations)
                    sum += s.lineNumbers.size();
                totalMethodInv += sum;
                totalUnique_fileMethodInv_pairs += result.methodInvocations.size();
                logger.info("{} Method Invocations Found ({} unique <file,method>) [OVERALL: {},{}]", sum, result.methodInvocations.size(), totalMethodInv, totalUnique_fileMethodInv_pairs);
                allrepos_methodInvocations.addAll(result.methodInvocations);
                //////////////////////// Packages Definitions
                totalPackgesCount += result.packageDeclarations.size();
                logger.info("{} Packages Found [OVERALL: {}]", result.packageDeclarations.size(), totalPackgesCount);
                allrepos_packageDef.put(repoFullname, new HashSet<>(result.packageDeclarations));
            }
            else
            {
                nError++;
                logger.info("FAILED: {}", repoFullname);
            }
        }
        logger.info("\n-----------------------------------------------------");
        logger.info("Extraction DONE: Error: {} out of {} total", nError, repoDependencies.size());
        logger.info("Overall: {} Method Invocations Found - {} unique <file,method> Found", totalMethodInv, totalUnique_fileMethodInv_pairs);
        logger.info("-----------------------------------------------------\n");

        logger.info("Writing Result on DB 1/2: Packages...");
        //PackagesDeclarationDB.WriteToSQLite(allrepos_packageDef, RESULT_PATH_PACKAGE_DEF);
        logger.info("Writing Result on DB 2/2: Method Invocations...");
        //MethodInvocationInfoDB.WriteToSQLite(allrepos_methodInvocations, false, true, RESULT_PATH_METHOD_INVOCATION, RESULT_TABLE_NAME_INVOCATION);
        logger.info("Done");
    }

}
