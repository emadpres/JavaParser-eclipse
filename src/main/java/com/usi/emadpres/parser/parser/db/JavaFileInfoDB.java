package com.usi.emadpres.parser.parser.db;

import com.usi.emadpres.parser.parser.ds.JavaFileInfo;
import com.usi.emadpres.parser.parser.ds.MethodDeclarationInfo;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public class JavaFileInfoDB {
    private static final Logger logger = LoggerFactory.getLogger(JavaFileInfoDB.class);
    public static final String TABLE_JavaFile = "JavaFile";

    public static void WriteToSQLite(List<JavaFileInfo> javaFileContents, Path path, boolean useDatabaseIdFieldAsIds)
    {
        if(javaFileContents==null) return;

        Connection conn = null;
        String sqlitePath = "jdbc:sqlite:" + path;

        try {
            logger.info("Writing Java File Contents at {}", path);

            File parentDir = path.toFile().getParentFile();
            if(parentDir.exists() == false)
                parentDir.mkdirs();


            conn = DriverManager.getConnection(sqlitePath);
            conn.setAutoCommit(false); // --> you should call conn.commit();
            java.sql.Statement stmt = conn.createStatement();



            String q_drop = String.format("DROP TABLE IF EXISTS %s;", TABLE_JavaFile);
            String q_create = String.format("CREATE TABLE %s (java_file_id INTEGER, project_name TEXT, commit_sha TEXT, relative_path TEXT, lines INTEGER, content TEXT, remark INTEGER, remark_str TEXT)", TABLE_JavaFile); // No "PRIMARY KEY(XX)". Makes the insertion slow.

            stmt.execute(q_drop);
            stmt.execute(q_create);


            String q_insert = String.format("INSERT INTO %s VALUES (?,?,?,?,?,?,?,?)", TABLE_JavaFile);
            PreparedStatement pstmt_insert = conn.prepareStatement(q_insert);

            final int BATCH_SIZE = 500000, total_packages = javaFileContents.size();;
            int lastReportedProgress = -1, currentProgress;
            int index=0;
            for(JavaFileInfo p: javaFileContents)
            {
                index++;
                currentProgress = (int)((index*100.0)/ total_packages);
                if(currentProgress-lastReportedProgress>= 1)
                {
                    lastReportedProgress = currentProgress;
                    //logger.info("%{} ({}/{})", currentProgress, index, total_packages-1);
                }



                pstmt_insert.setInt(1, useDatabaseIdFieldAsIds?p.databaseId:index);
                pstmt_insert.setString(2, p.projectName);
                pstmt_insert.setString(3, p.commitSHA);
                pstmt_insert.setString(4, p.fileRelativePath);
                pstmt_insert.setInt(5, p.nLines);
                pstmt_insert.setString(6, p.content);
                pstmt_insert.setInt(7, p.remark);
                pstmt_insert.setString(8, p.remark_str);
                pstmt_insert.addBatch();
                if(index%BATCH_SIZE==0) {
                    pstmt_insert.executeBatch();
                }
            }
            pstmt_insert.executeBatch();
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
     * Corresponding method to {@link #WriteToSQLite}
     */
    public static List<JavaFileInfo> ReadFromSqlite(Path path) {
        Map<Pair<String, String>, JavaFileInfo> res = ReadFromSqlite_GroupedByProjectFilePath(path);
        if(res==null)
            return null;

        List<JavaFileInfo> flatten = new ArrayList<>(res.values());
        return flatten;
    }

    /**
     * Corresponding method to {@link #WriteToSQLite}
     */
    public static Map<Pair<String/*project name*/, String/*file path*/>, JavaFileInfo> ReadFromSqlite_GroupedByProjectFilePath(Path path) {
        if(!Files.exists(path) || !Files.isRegularFile(path)) {
            logger.error("Database not found at {}", path);
            return null;
        }

        Map<Pair<String,String>, JavaFileInfo> all = new HashMap<>();
        Connection conn = null;
        String sqlitePath = "jdbc:sqlite:" + path;
        try {
            conn = DriverManager.getConnection(sqlitePath);
            Statement stmt = conn.createStatement();


            String query =  String.format("SELECT * FROM %s", TABLE_JavaFile);
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next())
            {
                int id = rs.getInt("java_file_id");
                String projectName = rs.getString("project_name");
                String commitSHA = rs.getString("commit_sha");
                String filePath = rs.getString("relative_path");
                int nLines = rs.getInt("lines");
                String fileContent = rs.getString("content");
                int remark = rs.getInt("remark");
                String remark_str = rs.getString("remark_str");


                JavaFileInfo info = new JavaFileInfo(projectName, commitSHA, filePath, nLines, fileContent);
                info.databaseId = id;
                info.remark = remark;
                info.remark_str = remark_str;

                Pair<String, String> p = new ImmutablePair<>(projectName, filePath);
                all.put(p, info);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        return all;
    }

}
