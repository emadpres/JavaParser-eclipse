package com.usi.emadpres.parser.parser.db;

import com.usi.emadpres.parser.parser.ds.PackageDeclarationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public class PackagesDeclarationDB {
    private static final Logger logger = LoggerFactory.getLogger(PackagesDeclarationDB.class);
    public static final String TABLE_PACKAGES = "PackageDeclaration";

    public static void WriteToSQLite(Set<PackageDeclarationInfo> packageDeclarationList, String path)
    {
        if(packageDeclarationList==null) return;

        Connection conn = null;
        String sqlitePath = "jdbc:sqlite:" + path;

        try {
            logger.info("Writing Packages Declaration at {}", path);

            File parentDir = new File(path).getParentFile();
            if(parentDir.exists() == false)
                parentDir.mkdirs();


            conn = DriverManager.getConnection(sqlitePath);
            conn.setAutoCommit(false); // --> you should call conn.commit();
            java.sql.Statement stmt = conn.createStatement();



            String q_drop = String.format("DROP TABLE IF EXISTS %s;", TABLE_PACKAGES);
            String q_create = String.format("CREATE TABLE %s (package_decl_Id INTEGER, project_name TEXT, commit_sha TEXT, package TEXT, dir_path TEXT)", TABLE_PACKAGES); // No "PRIMARY KEY(XX)". Makes the insertion slow.

            stmt.execute(q_drop);
            stmt.execute(q_create);


            String q_insert = String.format("INSERT INTO %s VALUES (?,?,?,?,?)", TABLE_PACKAGES);
            PreparedStatement pstmt_insert = conn.prepareStatement(q_insert);

            final int BATCH_SIZE = 500000, total_packages = packageDeclarationList.size();;
            int lastReportedProgress = -1, currentProgress;
            int index=0;
            for(PackageDeclarationInfo p: packageDeclarationList)
            {
                index++;

                currentProgress = (int)((index*100.0)/ total_packages);
                if(currentProgress-lastReportedProgress>= 1)
                {
                    lastReportedProgress = currentProgress;
                    //logger.info("%{} ({}/{})", currentProgress, index, total_packages-1);
                }

                pstmt_insert.setInt(1, index);
                pstmt_insert.setString(2, p.projectName);
                pstmt_insert.setString(3, p.commitSHA);
                pstmt_insert.setString(4, p.fullyQualifiedPackageName);
                pstmt_insert.setString(5, p.dirRelativePath);
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

    public static List<PackageDeclarationInfo> ReadFromSqlite(Path filepath)
    {
        Map<String, Set<PackageDeclarationInfo>> res = ReadPackagesFromSqlite_GroupByProject(filepath);
        if(res==null)
            return null;
        List<PackageDeclarationInfo> flatten = new ArrayList<>();
        for(var v: res.values())
            flatten.addAll(v);
        return flatten;
    }

    /**
     * Corresponding method to {@link #WriteToSQLite}
     */
    public static Map<String/*RepoFullname*/, Set<PackageDeclarationInfo>> ReadPackagesFromSqlite_GroupByProject(Path filepath)
    {
        if(!Files.exists(filepath) || !Files.isRegularFile(filepath)) {
            logger.error("Database not found at {}", filepath);
            return null;
        }

        Map<String, Set<PackageDeclarationInfo>> all = new HashMap<>();
        Connection conn = null;
        String sqlitePath = "jdbc:sqlite:" + filepath;
        try {
            conn = DriverManager.getConnection(sqlitePath);
            Statement stmt = conn.createStatement();


            String query =  String.format("SELECT * FROM %s", TABLE_PACKAGES);
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next())
            {
                int id = rs.getInt("package_decl_Id");
                String projectName = rs.getString("project_name");
                String commitSHA = rs.getString("commit_sha");
                String packageName = rs.getString("package");
                String dir_path = rs.getString("dir_path");

                PackageDeclarationInfo pd = new PackageDeclarationInfo(projectName, dir_path, packageName);
                pd.commitSHA = commitSHA;

                if(all.containsKey(projectName))
                {
                    Set<PackageDeclarationInfo> packagesForThisProject = all.get(projectName);
                    packagesForThisProject.add(pd);
                }
                else
                {
                    Set<PackageDeclarationInfo> packagesForThisProject_new = new HashSet<>();
                    packagesForThisProject_new.add(pd);
                    all.put(projectName, packagesForThisProject_new);
                }
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
