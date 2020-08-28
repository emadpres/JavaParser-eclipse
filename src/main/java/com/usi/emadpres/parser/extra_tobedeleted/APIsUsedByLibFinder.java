package com.usi.emadpres.parser.extra_tobedeleted;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class APIsUsedByLibFinder {
    private static final Logger logger = LoggerFactory.getLogger(APIsUsedByLibFinder.class.getName());

    public static void main(String[] args) {
        logger.info("Start.");

        String GROUP_ID = "org.apache.commons";
        String ARTIFACT_ID = "commons-lang3";
        String VERSION = "3.7";


        Map<Integer, Integer> res = findPublicAPINeverCalledInterClass(GROUP_ID, ARTIFACT_ID, VERSION);

    }

    public static Map<Integer/*MethodDecId-UsedInternallyByLib*/, Integer/*Sum #Invocations*/>
                    findPublicAPINeverCalledInterClass(String group_id, String artifact_id, String version)
    {

        /*
        Map<String, MavenLibInfo> ff = new HashMap<>();
        String DB_PATH___METHOD_INVOCATION, DB_PATH___PACKAGES_CLASSES;
        if(version.isEmpty())
            version=null;

        if (version == null) {
            MavenLibInfo tmp = new MavenLibInfo(group_id, artifact_id, "");
            String libPath = tmp.GetPathToLibDir();
            if(Files.exists(Paths.get(libPath))==false)
                return null;

            File[] versionDirs = new File(libPath).listFiles();
            Map<String, ArrayList<MavenLibInfo>> dependeciesPerVersion = new HashMap<>();
            Map<String, String> pathPerVersion = new HashMap<>();
            for (File p : versionDirs) {
                String srcPath = p.getAbsolutePath() + "/src";
                if (Files.exists(Paths.get(srcPath))) {
                    String v = p.getAbsolutePath().substring(p.getAbsolutePath().lastIndexOf('/')+1);
                    final String SimulatedRepoFullName = group_id + ":" + artifact_id + ":" + v;
                    dependeciesPerVersion.put(SimulatedRepoFullName, new ArrayList<>());
                    pathPerVersion.put(SimulatedRepoFullName, srcPath);
                    ff.put(SimulatedRepoFullName, new MavenLibInfo(group_id, artifact_id, v));
                }

            }

            DB_PATH___METHOD_INVOCATION =   "./out/temp/"+group_id+"-"+artifact_id+"-all-methodInvocations.db";
            DB_PATH___PACKAGES_CLASSES = "./out/temp/"+group_id+"-"+artifact_id+"-all-packagesClasses.db";
            if(!Files.exists(Paths.get(DB_PATH___METHOD_INVOCATION)) || !Files.exists(Paths.get(DB_PATH___METHOD_INVOCATION))) {
                Map<String, ProjectParsingResult> result = MethodInvocationInspectorNew.inspectProjects(dependeciesPerVersion, pathPerVersion);
                MethodInvocationInspectorNew.writeToSqlite_packagesAndClassesDef(result, DB_PATH___PACKAGES_CLASSES);
                MethodInvocationInspectorNew.writeToSqlite_methodInvocations(MethodInvocationInspectorNew.xxxx(result), false, true, DB_PATH___METHOD_INVOCATION, MethodInvocationInspectorNew.RESULT_TABLE_NAME_INVOCATION);
            }
        } else {
            DB_PATH___METHOD_INVOCATION =   "./out/temp/"+group_id+"-"+artifact_id+"-"+version+"-methodInvocations.db";
            DB_PATH___PACKAGES_CLASSES = "./out/temp/"+group_id+"-"+artifact_id+"-"+version+"-packagesClasses.db";
            final String SimulatedRepoFullName = group_id + ":" + artifact_id + ":" + version;
            if(!Files.exists(Paths.get(DB_PATH___METHOD_INVOCATION)) || !Files.exists(Paths.get(DB_PATH___METHOD_INVOCATION))) {
                MavenLibInfo targetLibToBeAnalyzed = new MavenLibInfo(group_id, artifact_id, version);

                ProjectParsingResult result =
                        MethodInvocationInspectorNew.IdentifyMethodInvocationsAndPackageDef(SimulatedRepoFullName, new ArrayList<>(), targetLibToBeAnalyzed.GetPathToLibDir() + "/src/");
                HashMap<String, MethodInvocationInspectorNew.ProjectInformationContainer> r = new HashMap<>();
                r.put(SimulatedRepoFullName, result);
                MethodInvocationInspectorNew.writeToSqlite_packagesAndClassesDef(r, DB_PATH___PACKAGES_CLASSES);
                MethodInvocationInspectorNew.writeToSqlite_methodInvocations(MethodInvocationInspectorNew.xxxx(r), false, true, DB_PATH___METHOD_INVOCATION, MethodInvocationInspectorNew.RESULT_TABLE_NAME_INVOCATION);
            }
            ff.put(SimulatedRepoFullName, new MavenLibInfo(group_id, artifact_id, version));
        }







        // 2. For given Lib, Bind Invocations, assuming only dependency for the lib-project is the lib-jar itself

        String DB_PATH___API_INVOCATIONS_ONLY_RESOVED;
        if(version==null)
            DB_PATH___API_INVOCATIONS_ONLY_RESOVED = "./out/temp/"+group_id+"-"+artifact_id+"-all-apiInvocations-onlyResolved.db";
        else
            DB_PATH___API_INVOCATIONS_ONLY_RESOVED = "./out/temp/"+group_id+"-"+artifact_id+"-"+version+"-apiInvocations-onlyResolved.db";

        if(!Files.exists(Paths.get(DB_PATH___API_INVOCATIONS_ONLY_RESOVED))) {
            MethodToAPIResolver.run(
                    "/Users/emadpres/IdeaProjects/Crawler/out/repoPomInfo.db",
                    "/Users/emadpres/playground/javaapi/javaapi-apiDeclarations/APIDeclarations.db",
                    DB_PATH___METHOD_INVOCATION,
                    DB_PATH___PACKAGES_CLASSES,
                    ff,
                    null,
                    DB_PATH___API_INVOCATIONS_ONLY_RESOVED);
        }


        // 3. Return list of methodDecIds
        String LIB_JAVA_SRC_REAL_PATH_PREFIX = group_id.replaceAll("[.]","/")+"/"+artifact_id.replaceAll("[.]","/");
        Map<Integer, Integer> apiNeverCalledInterClass = new HashMap<>(); //MethodDecId-UsedInternallyByLib -> Sum #Invocations
        Set<Integer> apiCalledInterClass = new HashSet<>();
        Connection con = null;
        try {
            con = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH___API_INVOCATIONS_ONLY_RESOVED);

            Statement stmt = con.createStatement();
            stmt.execute("ATTACH DATABASE  \"/Users/emadpres/playground/javaapi/javaapi-apiDeclarations/APIDeclarations.db\" AS  APIDeclarations;");
            //ResultSet rs = stmt.executeQuery("SELECT DISTINCT methodDecId FROM MethodInvocations_Resolved WHERE methodDecId!=-1");
            ResultSet rs = stmt.executeQuery("SELECT API_DEC.methodDecId, API_INVOC.filePath as invocFilePath, API_DEC.filePath as decFilePath, API_INVOC.count, API_DEC.isPublic " +
                    "FROM MethodInvocations_Resolved API_INVOC " +
                    "JOIN APIDeclarations.APIDeclaration API_DEC ON API_INVOC.methodDecId=API_DEC.methodDecId ");
            while(rs.next())
            {
                // I know, I could put this as "WHERE" clause in SQL, but to highlight I extracted it here
                if(rs.getInt("isPublic")==0)
                    continue;

                int methodDecId = rs.getInt("methodDecId");
                String invocFilePath = rs.getString("invocFilePath");
                invocFilePath = invocFilePath.substring(invocFilePath.indexOf(LIB_JAVA_SRC_REAL_PATH_PREFIX));
                String decFilePath = rs.getString("decFilePath");
                decFilePath = decFilePath.substring(decFilePath.indexOf(LIB_JAVA_SRC_REAL_PATH_PREFIX));

                if(invocFilePath.equals(decFilePath)==false ) //Inter Class
                {
                    // We're only interested in those API neve called by other classes, although they're public.
                    apiCalledInterClass.add(methodDecId);
                    apiNeverCalledInterClass.remove(methodDecId);
                    continue;
                }
                else if(apiCalledInterClass.contains(methodDecId)==false) {
                    int count = rs.getInt("count");
                    apiNeverCalledInterClass.put(methodDecId, count + apiNeverCalledInterClass.getOrDefault(methodDecId, 0));
                }
            }
            while(rs.next()) { }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return apiNeverCalledInterClass;
        */
        return null;
    }


}
