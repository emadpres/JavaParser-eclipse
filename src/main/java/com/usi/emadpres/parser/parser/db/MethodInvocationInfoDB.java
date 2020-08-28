package com.usi.emadpres.parser.parser.db;

import com.usi.emadpres.MavenUtils.ds.MavenLibInfo;
import com.usi.emadpres.parser.parser.ds.MethodInvocationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Schemas:
 *  TABLE 1: MethodInvocation
 *  (method_invoc_id, ...//TODO)
 *  TABLE 2: MethodInvocationLines
 *  (id, method_invoc_id, ...//TODO)
 */
public class MethodInvocationInfoDB {

    private static final Logger logger = LoggerFactory.getLogger(MethodInvocationInfoDB.class.getName());

    public static final String TABLE_INVOCATION_MAIN = "MethodInvocation";
    public static final String TABLE_INVOCATION_LINES ="MethodInvocationLines"; //only if withMethodSrcInformation=true

    public static void main(String[] args) {
        // Test
//        Map<String, List<MethodInvocationInfo>> res = ReadFromSqlite_GroupByProjects(Paths.get("/Users/emadpres/Downloads/Results/OLD_result.sqlite"), true);
//        List<MethodInvocationInfo> all = new ArrayList<>();
//        for(var x: res.values())
//            all.addAll(x);
//        WriteToSqlite(all, true, false, true, "/Users/emadpres/Downloads/Results/OLD_result__R.sqlite");
    }

    /**
     * @param withMethodSrcInformation
     *      Whether we should write {@link MethodInvocationInfo#isLocalInvocation}, {@link MethodInvocationInfo#isJavaInvocation},
     *      {@link MethodInvocationInfo#originalLibrary} information as extra columns.
     * @param withLineTable
     *      Whether we create an extra table {@link #TABLE_INVOCATION_LINES} which stores line numbers of a same method
     *      invocations within a file. (Otherwise in default table {@link #TABLE_INVOCATION_MAIN} we just write method M
     *      in file F has been called N times)
     * @param useDatabaseIdFieldAsIds   Whether we should use {@link MethodInvocationInfo#databaseId} or newly created IDs.
     *                                  Reusing existing databaseId comes in handy when we are re-storing a loaded data
     *                                  and we want to ensure ID consistency between to-be-written db and older ones.
     */
    public static void WriteToSqlite(List<MethodInvocationInfo> invocationsList, boolean withMethodSrcInformation, boolean withLineTable, boolean useDatabaseIdFieldAsIds,  String path)
    {
        if(invocationsList==null) return;

        Connection conn = null;
        String sqlitePath = "jdbc:sqlite:" + path;
        try {
            logger.info("Writing Method Invocations at {}", path);

            File parentDir = new File(path).getParentFile();
            if(parentDir.exists() == false)
                parentDir.mkdirs();


            conn = DriverManager.getConnection(sqlitePath);
            conn.setAutoCommit(false); // --> you should call conn.commit();
            java.sql.Statement stmt = conn.createStatement();

            // First Table
            String q_t1_drop = String.format("DROP TABLE IF EXISTS %s;", TABLE_INVOCATION_MAIN);
            String q_t1_create = String.format("CREATE TABLE %s (method_invoc_id INTEGER, project_name TEXT, commit_sha TEXT, path TEXT, count INTEGER, qualified_class_name TEXT, method_name TEXT, return_type TEXT, n_args INTEGER, args_types TEXT, remark INTEGER, remark_str TEXT)",TABLE_INVOCATION_MAIN); // No "PRIMARY KEY(method_invoc_id)". Makes the insertion slow.
            if(withMethodSrcInformation)
               q_t1_create = String.format("CREATE TABLE %s (method_invoc_id INTEGER, project_name TEXT, commit_sha TEXT, path TEXT, count INTEGER, qualified_class_name TEXT, method_name TEXT, return_type TEXT, n_args INTEGER, args_types TEXT, is_local_invoc INTEGER, is_java_invoc INTEGER, library_groupId TEXT, library_artifactId TEXT, library_version TEXT, remark INTEGER, remark_str TEXT)", TABLE_INVOCATION_MAIN); // No "PRIMARY KEY(method_invoc_id)". Makes the insertion slow.
            stmt.execute(q_t1_drop);
            stmt.execute(q_t1_create);

            // Second Table
            if(withLineTable) {
                String q_t2_drop = String.format("DROP TABLE IF EXISTS %s;", TABLE_INVOCATION_LINES);
                stmt.execute(q_t2_drop );

                String q_t2_create = String.format("CREATE TABLE %s (id INTEGER, method_invoc_id INTEGER, line INTEGER)", TABLE_INVOCATION_LINES);
                stmt.execute(q_t2_create );
            }


            // First table
            String q_t1_insert = String.format("INSERT INTO %s VALUES (?,?,?,?,?,?,?,?,?,?,?,?)", TABLE_INVOCATION_MAIN);
            if(withMethodSrcInformation)
               q_t1_insert = String.format("INSERT INTO %s VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", TABLE_INVOCATION_MAIN);
            PreparedStatement pstmt_invocationInfo = conn.prepareStatement(q_t1_insert);

            // Second table
            PreparedStatement pstmt_invocationLine = null;
            if(withLineTable) {
                String q_t2_insert = String.format("INSERT INTO %s VALUES (?,?,?)", TABLE_INVOCATION_LINES);
                pstmt_invocationLine = conn.prepareStatement(q_t2_insert);
            }


            final int total_invocations = invocationsList.size();
            final int BATCH_SIZE = 500000;
            int lastReportedProgress = -1, currentProgress;
            int methodInvocationIndex=-1;
            int methodInvocationLineIndex = -1;


            for(MethodInvocationInfo methodInvInfo: invocationsList)
            {
                methodInvocationIndex++;

                currentProgress = (int)((methodInvocationIndex*100.0)/ total_invocations);
                if(currentProgress-lastReportedProgress>= 1)
                {
                    lastReportedProgress = currentProgress;
                    //logger.info("%{} ({}/{})", currentProgress, methodInvocationIndex, total_invocations-1);
                }

                pstmt_invocationInfo.setInt(1, useDatabaseIdFieldAsIds?methodInvInfo.databaseId:methodInvocationIndex);
                pstmt_invocationInfo.setString(2, methodInvInfo.projectName);
                pstmt_invocationInfo.setString(3, methodInvInfo.commitSHA);
                pstmt_invocationInfo.setString(4, methodInvInfo.fileRelativePath);


                int lCount = methodInvInfo.lineNumbers.size();
                if(withLineTable==false /*if withLineTable is true => methodInvInfo.lineNumbers is up-to-date*/
                        && lCount==0 && methodInvInfo.lineNumbersCount>0)
                    lCount = methodInvInfo.lineNumbersCount;
                pstmt_invocationInfo.setInt(5, lCount);
                pstmt_invocationInfo.setString(6,methodInvInfo.qualifiedClassName);
                pstmt_invocationInfo.setString(7,methodInvInfo.name);
                pstmt_invocationInfo.setString(8,methodInvInfo.returnType);
                pstmt_invocationInfo.setInt(9,methodInvInfo.nArgs);
                pstmt_invocationInfo.setString(10,methodInvInfo.argsTypes);

                if(!withMethodSrcInformation)
                {
                    pstmt_invocationInfo.setInt(11, methodInvInfo.remark);
                    pstmt_invocationInfo.setString(12, methodInvInfo.remark_str);
                }
                else {
                    pstmt_invocationInfo.setInt(11, methodInvInfo.isLocalInvocation?1:0);
                    pstmt_invocationInfo.setInt(12, methodInvInfo.isJavaInvocation?1:0);
                    if(methodInvInfo.originalLibrary==null) {
                        pstmt_invocationInfo.setString(13, null);
                        pstmt_invocationInfo.setString(14, null);
                        pstmt_invocationInfo.setString(15, null);
                    }
                    else {
                        pstmt_invocationInfo.setString(13, methodInvInfo.originalLibrary.groupId);
                        pstmt_invocationInfo.setString(14, methodInvInfo.originalLibrary.artifactId);
                        pstmt_invocationInfo.setString(15, methodInvInfo.originalLibrary.version);
                    }
                    pstmt_invocationInfo.setInt(16, methodInvInfo.remark);
                    pstmt_invocationInfo.setString(17, methodInvInfo.remark_str);
                }
                pstmt_invocationInfo.addBatch();

                if(withLineTable) {
                    for (int ll = 0; ll < methodInvInfo.lineNumbers.size(); ll++) {
                        methodInvocationLineIndex++;
                        pstmt_invocationLine.setInt(1, methodInvocationLineIndex);
                        pstmt_invocationLine.setInt(2, useDatabaseIdFieldAsIds?methodInvInfo.databaseId:methodInvocationIndex);
                        pstmt_invocationLine.setInt(3, methodInvInfo.lineNumbers.get(ll));
                        pstmt_invocationLine.addBatch();
                    }
                }


                if(methodInvocationIndex % BATCH_SIZE==0)
                    pstmt_invocationInfo.executeBatch();
                if (withLineTable && methodInvocationIndex % BATCH_SIZE == 0)
                    pstmt_invocationLine.executeBatch();


            }

            pstmt_invocationInfo.executeBatch();
            if(withLineTable)
                pstmt_invocationLine.executeBatch();


            stmt.execute(String.format("CREATE INDEX %s_methodInvId_index ON %s (method_invoc_id);",TABLE_INVOCATION_MAIN,TABLE_INVOCATION_MAIN));
            if(withMethodSrcInformation)
                stmt.execute(String.format("CREATE INDEX %s_library_index ON %s (library_groupId, library_artifactId, library_version);",TABLE_INVOCATION_MAIN,TABLE_INVOCATION_MAIN));
            if(withLineTable)
                stmt.execute(String.format("CREATE INDEX %s_methodInvId_index ON %s (method_invoc_id);",TABLE_INVOCATION_LINES,TABLE_INVOCATION_LINES));
            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }


    /**
     * A wrapper for {@link #ReadFromSqlite} to return result group by project name
     * Note: This implementaiton is easy to understand and reuse existing {@link #ReadFromSqlite}, so not very efficient
     *
     * @param path @see #ReadFromSqlite
     * @param withMethodSrcInformation @see #ReadFromSqlite
     */
    public static Map<String/*project name*/, List<MethodInvocationInfo>> ReadFromSqlite_GroupByProjects(Path path, boolean withMethodSrcInformation/*, boolean withLineTable*/)
    {
        Map<String, List<MethodInvocationInfo>> result = new HashMap<>();
        List<MethodInvocationInfo> methodInvocationList = ReadFromSqlite(path, withMethodSrcInformation);
        for(var m: methodInvocationList)
        {
            if(result.containsKey(m.projectName))
            {
                result.get(m.projectName).add(m);
            }
            else
            {
                List<MethodInvocationInfo> l = new ArrayList<>();
                l.add(m);
                result.put(m.projectName, l);
            }
        }
        return result;
    }


    /**
     * Corresponding method to {@link #WriteToSqlite}
     * @param withMethodSrcInformation Should we also read "is_local_invoc", "is_java_invoc", "library_groupId",
     *                                 "library_artifactId", and "library_version"  columns to fill
     *                                 {@link MethodInvocationInfo#isLocalInvocation/isJavaInvocation/originalLibrary}
     * TODO -> @param withLineTable Should we also read {@link #TABLE_INVOCATION_LINES} table and fill {@link MethodInvocationInfo#lineNumbers} list?
     */
    public static List<MethodInvocationInfo> ReadFromSqlite(Path path, boolean withMethodSrcInformation/*, boolean withLineTable*/)
    {
        if(!Files.exists(path) || !Files.isRegularFile(path)) {
            logger.error("Database not found at {}", path);
            return null;
        }

        List<MethodInvocationInfo> result = null;
        Connection conn = null;
        String sqlitePath = "jdbc:sqlite:" + path;
        try {
            conn = DriverManager.getConnection(sqlitePath);
            Statement stmt = conn.createStatement();


            // First Table
            String q_t1_read = String.format("SELECT * FROM %s",TABLE_INVOCATION_MAIN);
            ResultSet rs = stmt.executeQuery(q_t1_read);

            // Second Table
            //TODO if(withLineTable) // First concat_group second table and join this new table to the first table. This way we have one temporary new column for line numbers

            result = new ArrayList<>();
            while (rs.next())
            {
                int id = rs.getInt("method_invoc_id");
                String projectName = rs.getString("project_name");
                String commitSHA = rs.getString("commit_sha");
                String filePath = rs.getString("path");
                int count = rs.getInt("count");
                ////////////////////////////
                String qualifiedClassName = rs.getString("qualified_class_name");
                String name = rs.getString("method_name");
                String returnType = rs.getString("return_type");
                if(returnType.equals("")) returnType="void";
                int nArgs = rs.getInt("n_args");
                String argsTypes = rs.getString("args_types");
                ////////////////////////////
                boolean isLocalInvocation=false;
                boolean isJavaInvocation=false;
                String originalLibrary_groupId=null;
                String originalLibrary_artifactId=null;
                String originalLibrary_version=null;
                if(withMethodSrcInformation)
                {
                    isLocalInvocation = rs.getInt("is_local_invoc") == 1;
                    isJavaInvocation = rs.getInt("is_java_invoc")==1;
                    originalLibrary_groupId = rs.getString("library_groupId");
                    originalLibrary_artifactId = rs.getString("library_artifactId");
                    originalLibrary_version = rs.getString("library_version");
                }

                MethodInvocationInfo m = new MethodInvocationInfo(qualifiedClassName, returnType, name, nArgs, argsTypes);
                m.databaseId = id;
                m.projectName = projectName;
                m.commitSHA = commitSHA;
                m.fileRelativePath = filePath;
                m.lineNumbersCount = count;
                if(withMethodSrcInformation)
                {
                    m.isLocalInvocation = isLocalInvocation;
                    m.isJavaInvocation = isJavaInvocation;
                    if(originalLibrary_groupId!=null || originalLibrary_artifactId!=null || originalLibrary_version!=null)
                        m.originalLibrary = new MavenLibInfo(originalLibrary_groupId, originalLibrary_artifactId, originalLibrary_version);
                    else
                        m.originalLibrary = null;
                }

               m.remark = rs.getInt("remark");
               m.remark_str = rs.getString("remark_str");;

                result.add(m);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
