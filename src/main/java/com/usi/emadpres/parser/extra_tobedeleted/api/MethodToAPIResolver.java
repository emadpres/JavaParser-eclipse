package com.usi.emadpres.parser.extra_tobedeleted.api;

import com.usi.emadpres.MavenUtils.ds.MavenLibInfo;
import com.usi.emadpres.parser.parser.db.MethodInvocationInfoDB;
import com.usi.emadpres.parser.parser.db.UserTypeDeclarationDB;
import com.usi.emadpres.parser.parser.ds.MethodInvocationInfo;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

// TODO: Not checked for ICSE21
public class MethodToAPIResolver {
    private static final Logger logger = LoggerFactory.getLogger(MethodToAPIResolver.class.getName());
    private static final String RESULT_PATH = "out/APIInvocations.db";
    private static final String RESULT_ONLY_RESOLVED_PATH = "out/APIInvocations_onlyResolved.db";
    public static final String RESULT_TABLE_NAME_INVOCATION = "MethodInvocation";


    public static void main(String[] args) {

        if (args.length != 4) {
            System.err.println("MethodToAPIResolver -Xmx50+Gig <path-to-repoPomInfo.db> <path-to-APIDeclarations.db> <path-to-MethodInvocations.db>  <path-to-RepoPackagesClasses.db>");
            return;
        }

        String PATH_TO__REPO_POM_INFO = args[0];
        String PATH_TO__API_DECLARATIONS = args[1];
        String PATH_TO__API_INVOCATIONS = args[2];
        String PATH_TO__PACKAGES_AND_CLASSES = args[3];

        run(PATH_TO__REPO_POM_INFO, PATH_TO__API_DECLARATIONS, PATH_TO__API_INVOCATIONS, PATH_TO__PACKAGES_AND_CLASSES, null, RESULT_PATH, RESULT_ONLY_RESOLVED_PATH);

    }


    public static void run(String PATH_TO__REPO_POM_INFO,
                           String PATH_TO__API_DECLARATIONS,
                           String PATH_TO__API_INVOCATIONS,
                           String PATH_TO__PACKAGES_AND_CLASSES,
                            /*Used when we analyzing a lib-method-invocations and we don't have a pom/dependency-list as normally we have for repositories*/
                           Map<String, MavenLibInfo> additionalDependency,
                           String result_dbPath,
                           String resultOnlyResolved_dbPath)
    {
        Map<String, List<String>> repo2Pom = new HashMap<>();
        Map<Pair<String, String>, Set<MavenLibInfo>> repoPom2Dep = new HashMap<>();
        Map<MethodInvocationInfo, ArrayList<Pair<Integer, MavenLibInfo>>> apiDecToMaven = new HashMap<>();
        Set<String> qualifiedClasses = new HashSet<>();
        Map<String/*repo*/, Set<String> /*local packages*/> repoToTypeNames = new HashMap<>();
        //////////////////////////////////////////////////////////////////////////////////
        repoToTypeNames = UserTypeDeclarationDB.readAllRepoTypes(PATH_TO__PACKAGES_AND_CLASSES);
        readSQL_repo2Pom2Dep(PATH_TO__REPO_POM_INFO, repo2Pom, repoPom2Dep);
        Iterator<Map.Entry<String, List<String>>> it = repo2Pom.entrySet().iterator();
        while (it.hasNext())
        {
            List<String> poms = it.next().getValue();
            if (poms.size() == 1)
                continue;
            poms.sort(Comparator.comparing(String::length).reversed()); //longest to shortest
        }
        readSQL_apiDec2Lib(PATH_TO__API_DECLARATIONS, apiDecToMaven, qualifiedClasses);
        ////////////////////////////////////////////////////////////////////////////
        Connection conn = null;
        int nAPIMatchedSuccessfully = 0;
        int nNoMatchingPom = 0, nNoMatchingAPIDeclaration = 0, nNoMatchingAPIDeclaration_java = 0, nNoMatchingAPIDeclaration_smelly = 0;
        int nAPICorrespondingToMultipleLib = 0, nAPIWithNoLib = 0;
        Set<String> skippedAPIClasses = new HashSet<>();
        Set<Pair<String, String>> skippedAPIClassesMethodPairs = new HashSet<>();
        int lastReportedProgress = -100, currentProgress;
        int TOTAL = 37406877;//allApiInvocations.size();
        ArrayList<MethodInvocationInfo> apiInvocations_all = new ArrayList<>();
        ArrayList<MethodInvocationInfo> apiInvocations_targetedLibs = new ArrayList<>();
        ArrayList<MethodInvocationInfo> apiInvocations_nonTargetedLibs = new ArrayList<>();
        int index = 0;
        try {

            String sqlitePath = "jdbc:sqlite:" + PATH_TO__API_INVOCATIONS;
            conn = DriverManager.getConnection(sqlitePath);
            Statement stmt = conn.createStatement();
            //PreparedStatement stmt2 = conn.prepareStatement("SELECT line, methodContHashcode, blockContHashcode FROM "+ MethodInvocationInspectorNew.RESULT_TABLE_NAME_INVOCATION_OCCURRENCE+" WHERE methodInvId = ?");

            String query = String.format("SELECT I.methodInvId, I.repoFullname, I.filePath, I.count, \n" +
                    "I.qualifiedClassName, I.name, I.returnType, I.nArgs, I.argsTypes\n" +
                    "FROM %s I", RESULT_TABLE_NAME_INVOCATION);

            //"ORDER BY I.repoFullname, I.filePath, I.qualifiedClassName, I.name";
            ResultSet rs = stmt.executeQuery(query);


            while (rs.next()) {

                //logger.info(">"+index);
                currentProgress = (int) ((index++ * 100.0) / TOTAL);
                if (currentProgress - lastReportedProgress > 1) {
                    lastReportedProgress = currentProgress;
                    logger.info("---> %{} (estimated on #total={})", currentProgress, TOTAL);
                }

                int methodInvId = rs.getInt("methodInvId");
                String repoFullname = rs.getString("repoFullname");
                String filePath = rs.getString("filePath");
                //int methodContainerHashcode = rs.getInt("methodContHashcode");
                int count = rs.getInt("count");
                ////////////////////////////
                String qualifiedClassName = rs.getString("qualifiedClassName");
                String name = rs.getString("name");
                String returnType = rs.getString("returnType");
                int nArgs = rs.getInt("nArgs");
                String argsTypes = rs.getString("argsTypes");
                ////////////////////////////
                MethodInvocationInfo curInvocationInfo = new MethodInvocationInfo(qualifiedClassName, returnType, name, nArgs, argsTypes);
                curInvocationInfo.databaseId = methodInvId;
                curInvocationInfo.projectName = repoFullname;
                curInvocationInfo.fileRelativePath = filePath;

//                stmt2.setInt(1, methodInvId);
//                ResultSet rs_lines = stmt2.executeQuery();
//                while(rs_lines.next())
//                {
//                    int line = rs_lines.getInt("line");
//                    int methodContHashcode = rs_lines.getInt("methodContHashcode");
//                    int blockContHashcode = rs_lines.getInt("blockContHashcode");
//                    ///////
//                    curInvocationInfo.lineNumbers.add(line);
//                    curInvocationInfo.surroundingMethod_hashcodes.add(methodContHashcode);
//                    curInvocationInfo.surroundingMethod_statementNumbers.add(blockContHashcode);
//                }

                // To populate "count" column, we need to fakely fill "curInvocationInfo.lineNumbers" array.
                // See "count" column at "MethodInvocationInspector::writeToSqlite_methodInvocations()"
                for (int addingFakeLineCounter = 0; addingFakeLineCounter < count; addingFakeLineCounter++) {
                    curInvocationInfo.lineNumbers.add(-1);
                    //curInvocationInfo.surroundingMethod_hashcodes.add(-1);
                    //curInvocationInfo.surroundingMethod_statementNumbers.add(-1);
                }
                ///////////////////////////////////////
                int result = doo(curInvocationInfo, repo2Pom, apiDecToMaven, repoPom2Dep, qualifiedClasses, additionalDependency, skippedAPIClasses, skippedAPIClassesMethodPairs);
                /////////
                boolean isLocalCall = false;//, isLocalCall_wrong_forDebugging = false;
                Set<String> localTypesForThisRepo = repoToTypeNames.get(curInvocationInfo.projectName);
                //int lastDot = curInvocationInfo.qualifiedClassName.lastIndexOf('.');
                //String packageNameForThisInvocation = ""; //the qualified class name had no package-name prefix
                //if (lastDot != -1)
                //    packageNameForThisInvocation = curInvocationInfo.qualifiedClassName.substring(0, lastDot);
                /* Below if-code is not enough. Imagine we have an inner static class B inside A at Owner/Proj/x/y/z/A.java
                    Then qualifiedClassName for B methods would be x.y.z.A.B, but x.y.z.A doesn't exits as packageName!
                if (localPackagesForThisRepo != null && localPackagesForThisRepo.contains(packageNameForThisInvocation))
                    isLocalCall_wrong_forDebugging = true;*/

                for(String aLocalTypeInCurRepo: localTypesForThisRepo)
                    if(curInvocationInfo.qualifiedClassName.equals(aLocalTypeInCurRepo)) {
                        isLocalCall = true;
                        break;
                    }

                boolean isJavaCall = false;
                ArrayList<Pair<Integer, MavenLibInfo>> candidateLibForThisApi = apiDecToMaven.get(curInvocationInfo);
                if(candidateLibForThisApi!=null)
                    for(Pair<Integer, MavenLibInfo> lib: candidateLibForThisApi)
                        if(lib.getValue().groupId.equals("java") && lib.getValue().artifactId.equals("java"))
                        {
                            isJavaCall=true;
                            if(result<0)
                                result = lib.getKey();
                            break;
                        }



                ////////////////////////////////////////
                if (result >= 0) {
                    nAPIMatchedSuccessfully++;
                    curInvocationInfo.methodDeclarationId = result;
                    curInvocationInfo.isLocalInvocation = isLocalCall;
                    curInvocationInfo.isJavaInvocation = isJavaCall;
                    apiInvocations_targetedLibs.add(curInvocationInfo);
                    apiInvocations_all.add(curInvocationInfo);
                } else {
                    curInvocationInfo.methodDeclarationId = -1;
                    curInvocationInfo.isLocalInvocation = isLocalCall;
                    curInvocationInfo.isJavaInvocation = isJavaCall;
                    apiInvocations_nonTargetedLibs.add(curInvocationInfo);
                    apiInvocations_all.add(curInvocationInfo);
                    if (result == -1)
                        nNoMatchingAPIDeclaration++;
                    else if (result == -11)
                        nNoMatchingAPIDeclaration_java++;
                    else if (result == -12)
                        nNoMatchingAPIDeclaration_smelly++;
                    else if (result == -2)
                        nNoMatchingPom++;
                    else if (result == -3)
                        nAPICorrespondingToMultipleLib++;
                    else if (result == -5)
                        nAPIWithNoLib++;
                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

//
//        for(int i=0; i<TOTAL; i++) {
//            currentProgress = (int)((i*100.0)/TOTAL);
//            if(currentProgress-lastReportedProgress>1)
//            {
//                lastReportedProgress=currentProgress;
//                logger.info("Matching API %{}", currentProgress);
//            }
//
//            MethodInvocationInfo curInvocationInfo = allApiInvocations.get(i);
//
//
//
//        }

        TOTAL = index;
        logger.error("#Invocations (Local ot non targeted Libs): {}/{} + ...[see next line] ", nNoMatchingAPIDeclaration, TOTAL);
        logger.error("{} Java , {} smelly ({} classes belongs to targeted libs)", nNoMatchingAPIDeclaration_java, nNoMatchingAPIDeclaration_smelly, skippedAPIClasses.size());
        logger.error("#API Invocations belong to a targeted Lib, but that lib not included in the subproject pom.xml: {}/{}", nAPIWithNoLib, TOTAL);
        logger.error("#API Invocations Multi-Libs: {}/{}", nAPICorrespondingToMultipleLib, TOTAL);
        logger.error("#API successful: #{}/{}", nAPIMatchedSuccessfully, TOTAL);


        if(result_dbPath!=null)
            MethodInvocationInfoDB.WriteToSqlite(apiInvocations_all, true, false, true, result_dbPath);
        if(resultOnlyResolved_dbPath!=null)
            MethodInvocationInfoDB.WriteToSqlite(apiInvocations_targetedLibs, true, false, true, resultOnlyResolved_dbPath);
        //writeToSqlite(apiInvocations_nonTargetedLibs, RESULT_PATH_SEPERATED_TABLE, "OtherInvocations");

    }


    private static int doo(MethodInvocationInfo curInvocationInfo,
                           Map<String, List<String>> repo2Pom,
                           Map<MethodInvocationInfo, ArrayList<Pair<Integer, MavenLibInfo>>> apiDecToMaven,
                           Map<Pair<String, String>, Set<MavenLibInfo>> repoPom2Dep,
                           Set<String> qualifiedClasses,
                           Map<String, MavenLibInfo> additionalDependency,
                           Set<String> skippedAPIClasses,
                           Set<Pair<String, String>> skippedAPIClassesMethodPairs) {
        /////////////// Find possible dependencies for such API signature (performance tip: this step first since lots of invocations are actually not thrid-party invocations)
        ArrayList<Pair<Integer, MavenLibInfo>> candidateLibForThisApi = apiDecToMaven.get(curInvocationInfo);
        if (candidateLibForThisApi == null) {

//            if(curInvocationInfo.qualifiedClassName.isEmpty()==false && qualifiedClasses.contains(curInvocationInfo.qualifiedClassName))
//            {
//                // Potentially valid API... Try ro recover
//                if(curInvocationInfo.argsTypes.indexOf(".Class<")==-1 && curInvocationInfo.argsTypes.indexOf(".Class")!=-1)
//                    curInvocationInfo.argsTypes = curInvocationInfo.argsTypes.replaceAll("\\.Class(?!<)", ".Class<T>");
//
//                if(curInvocationInfo.returnType.indexOf(".Class<")==-1 && curInvocationInfo.returnType.indexOf(".Class")!=-1)
//                    curInvocationInfo.returnType = curInvocationInfo.returnType.replaceAll("\\.Class(?!<)", ".Class<T>");
//
//                candidateLibForThisApi = apiDecToMaven.get(curInvocationInfo);
//            }

            if (candidateLibForThisApi == null) {
                if (qualifiedClasses.contains(curInvocationInfo.qualifiedClassName)) {
                    skippedAPIClasses.add(curInvocationInfo.qualifiedClassName);
                    skippedAPIClassesMethodPairs.add(new ImmutablePair(curInvocationInfo.qualifiedClassName, curInvocationInfo.name));
                    return -11;
                }

                if (curInvocationInfo.qualifiedClassName.startsWith("java."))
                    return -12;
                return -1;
//                nNoMatchingAPIDeclaration++;
//                continue;
            }

        }



        /////////////// Find matching Pom
        String matchingPom = null;
        List<String> candidatePoms_sorted = repo2Pom.get(curInvocationInfo.projectName);

        Set<MavenLibInfo> candidateLibForThisSubproject = null;
        if(additionalDependency!=null) {
            candidateLibForThisSubproject = new HashSet<>();
            candidateLibForThisSubproject.add(additionalDependency.get(curInvocationInfo.projectName));
        }
        else if(candidatePoms_sorted!=null)
        {
            for (String pom : candidatePoms_sorted) {
                if (curInvocationInfo.fileRelativePath.startsWith(curInvocationInfo.projectName + "/" + pom)) {
                    matchingPom = pom;
                    break;
                }
            }
            if (matchingPom == null) {
//            nNoMatchingPom++;
//           logger.error("-2: No Matching Pom: {}",curInvocationInfo);
//            continue;
                return -2;
            }
            /////////////// Find possible dependencies for This Subproject
            candidateLibForThisSubproject = repoPom2Dep.get(new ImmutablePair(curInvocationInfo.projectName, matchingPom));
            if (candidateLibForThisSubproject == null) {
//            logger.error("-4: candidateLibForThisSubproject=null ???? ===> Repo:{} -- {}", curInvocationInfo.repoFullname, curInvocationInfo);
                return -4;
            }
        }




        /*TODO We have two cases:
          1. "candidateLibForThisSubproject" is filled with pom.xml dependencies
          2. When we're given a random java project (~ libraries source code, ...) with no pom.
         */

        /////////////// Find Library of API
        MavenLibInfo matchingLibForAPI = null;
        int mathingAPIDecIdForAPI = -1;
        boolean ambiguse = false;
        //for (MavenLibInfo m1 : candidateLibForThisApi_unique) {
        for(Pair<Integer, MavenLibInfo> p: candidateLibForThisApi){
            if (ambiguse) break;
            MavenLibInfo m1 = (MavenLibInfo) (p.getValue());
            for (MavenLibInfo m2 : candidateLibForThisSubproject) {
                if (m1.equals(m2)) {
                    // Why below lines for ambiguse is commented?
                    // A) public static <T extends Map<?, ?>> T notEmpty(final T map, final String message, final Object... values)
                    // B) public static <T extends Collection<?>> T notEmpty(final T collection, final String message, final Object... values)
                    // Although (A) and (B) have different methodDecId, they are the same from "Signature" point of view
                    // and we face ambiguse. I decided for such cases take one. because in fact if I pick (A), really (A)
                    // is totally correct match, except for a minor T class.
//                    if (mathingAPIDecIdForAPI != -1) {
//                        ambiguse = true;
//                        break;
//                    }
                    matchingLibForAPI = m1;
                    mathingAPIDecIdForAPI = (Integer) (p.getKey());
                    break;
                }
            }
        }

        if (ambiguse) {
//            logger.warn("Two matching Lib for API: Repo:{} - {}", curInvocationInfo.repoFullname, curInvocationInfo);
//            continue;
            return -3;
        } else if (mathingAPIDecIdForAPI == -1) {
            //nAPIWithNoLib++;
            return -5;
            //continue;
        }

        //nAPIMatchedSuccessfully++;
        return mathingAPIDecIdForAPI;
        // RESULT ===> mathingAPIDecIdForAPI, matchingLibForAPI
    }


    private static Set<String> intersectTwoSet(Set<String> s1, Set<String> s2) {
        // Pay attention not to change inputs
        Set<String> common = new HashSet<>(s1);
        common.retainAll(s2);
        return common;
    }

    private static void readSQL_repo2Pom2Dep(String _path, Map<String,
            List<String>> repo2Pom, Map<Pair<String, String>, Set<MavenLibInfo>> repoPom2Dep) {
        logger.info("Reading repo2Pom and repoPom2Dep ...");
        Connection conn = null;
        try {
            String sqlitePath = "jdbc:sqlite:" + _path;
            conn = DriverManager.getConnection(sqlitePath);
            Statement stmt = conn.createStatement();

            String query = "SELECT P.repo_fullname, P.pomPath, D.groupId, D.artifactId, D.version\n" +
                    "FROM RepoPomFiles P JOIN RepoPomDependencies D on P.id = D.pomFile_id";
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                String repoFullname = rs.getString("repo_fullname");
                String pomPath = rs.getString("pomPath");
                pomPath = pomPath.substring(0, pomPath.length() - "pom.xml".length());
                String groupId = rs.getString("groupId");
                String artifactId = rs.getString("artifactId");
                String version = rs.getString("version");

                Pair repoPomPair = new ImmutablePair(repoFullname, pomPath);
                MavenLibInfo dep = new MavenLibInfo(groupId, artifactId, version);

                if (repoPom2Dep.containsKey(repoPomPair))
                    repoPom2Dep.get(repoPomPair).add(dep);
                else {
                    repoPom2Dep.put(repoPomPair, new HashSet<>(Arrays.asList(dep)));

                    if (repo2Pom.containsKey(repoFullname))
                        repo2Pom.get(repoFullname).add(pomPath);
                    else
                        repo2Pom.put(repoFullname, new ArrayList<>(Arrays.asList(pomPath)));
                }

            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        logger.info("DONE: repo2Pom {} -- repoPom2Dep {}", repo2Pom.size(), repoPom2Dep.size());
    }


    private static void readSQL_apiDec2Lib(String _path,
               // We use MethodInvocation instead of MethodDeclarationInfo make it easier later when comparing with
               // actual MethodInvocation
               Map<MethodInvocationInfo, ArrayList<Pair<Integer, MavenLibInfo>>> apiDec2Maven,
               Set<String> qualifiedClasses) {

        Set<MethodInvocationInfo> apiInMoreThanOneLib = new HashSet<>();
        logger.info("Reading apiDec2Lib...");

        Connection conn = null;
        try {
            String sqlitePath = "jdbc:sqlite:" + _path;
            conn = DriverManager.getConnection(sqlitePath);
            Statement stmt = conn.createStatement();

            String query = "SELECT D.methodDecId, D.groupId, D.artifactId, D.version, \n" +
                    "D.qualifiedClassName, D.name, D.argsTypes, D.nArgs, D.returnType\n" +
                    "FROM APIDeclaration D\n" +
                    //"WHERE D.accessLevel=0\n" +
                    "ORDER BY D.nArgs";
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                int methodDecId = rs.getInt("methodDecId");
                String groupId = rs.getString("groupId");
                String artifactId = rs.getString("artifactId");
                String version = rs.getString("version");
                /////////
                String qualifiedClassName = rs.getString("qualifiedClassName");
                String name = rs.getString("name");
                String returnType = rs.getString("returnType");
                int nArgs = rs.getInt("nArgs");
                String argsTypes = rs.getString("argsTypes");

                MethodInvocationInfo apiInfo = new MethodInvocationInfo(qualifiedClassName, returnType, name, nArgs, argsTypes);
                MavenLibInfo libInfo = new MavenLibInfo(groupId, artifactId, version);

                if (apiDec2Maven.containsKey(apiInfo)) {
                    // We might map X to <123, junit-4.12> and <456, junit-4.12>..
                    // Although two entries, but the same unique lib
                    apiDec2Maven.get(apiInfo).add(new ImmutablePair(methodDecId, libInfo));
                    apiInMoreThanOneLib.add(apiInfo);
                } else {
                    ArrayList<Pair<Integer, MavenLibInfo>> arr = new ArrayList<>();
                    arr.add(new ImmutablePair(methodDecId, libInfo));
                    apiDec2Maven.put(apiInfo, arr);
                    qualifiedClasses.add(qualifiedClassName);
                }
            }


        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        logger.info("DONE   (# API In More Than One Lib: {})", apiInMoreThanOneLib.size());
        logger.info("apiDec2Maven {} -- qualifiedClasses {}", apiDec2Maven.size(), qualifiedClasses.size());
    }


//    private static ResultSet readSQL_ApiInv(String _path) {
//        //ArrayList<MethodInvocationInfo> allApiInvocations = new ArrayList<>();
//        ResultSet rs = null;
//        Connection conn = null;
//        logger.info("Reading All method Invocations...");
//        try {
//            String sqlitePath = "jdbc:sqlite:" + _path;
//            conn = DriverManager.getConnection(sqlitePath);
//            Statement stmt = conn.createStatement();
//
//            String query = "SELECT I.repoFullname, I.filePath, I.count, \n" +
//                            "I.qualifiedClassName, I.name, I.returnType, I.nArgs, I.argsTypes\n" +
//                            "FROM MethodInvocation I" +
//                            "\n LIMIT 10000" ;
//                            //"ORDER BY I.repoFullname, I.filePath, I.qualifiedClassName, I.name";
//            rs = stmt.executeQuery(query);
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
////        finally {
////            try {
////                if (conn != null) conn.close();
////            } catch (SQLException ex) {
////                ex.printStackTrace();
////            }
////        }
//        return rs;
//    }

//    private void readSQL_template(String _path) {
//        Connection conn = null;
//        try {
//            String sqlitePath = "jdbc:sqlite:" + _path;
//            conn = DriverManager.getConnection(sqlitePath);
//            Statement stmt = conn.createStatement();
//
//            String query = "SELECT * FROM ...";
//            ResultSet rs = stmt.executeQuery(query);
//
//            while (rs.next()) {
//                int id = rs.getInt("id");
//            }
//
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if (conn != null) conn.close();
//            } catch (SQLException ex) {
//                ex.printStackTrace();
//            }
//        }
//    }
}
