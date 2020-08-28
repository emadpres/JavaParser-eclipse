package com.usi.emadpres.parser.parser.db;

import com.usi.emadpres.parser.parser.ds.PackageDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.util.*;

public class PackagesDeclarationDB {
    private static final Logger logger = LoggerFactory.getLogger(PackagesDeclarationDB.class);
    public static final String TABLE_PACKAGES = "PackageDeclaration";

    public static void WriteToSQLite(Set<PackageDeclaration> packageDeclarationList, String path)
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
            String q_create = String.format("CREATE TABLE %s (package_decl_Id INTEGER, project_name INTEGER, commit_sha TEXT, package TEXT, dir_path TEXT)", TABLE_PACKAGES); // No "PRIMARY KEY(XX)". Makes the insertion slow.

            stmt.execute(q_drop);
            stmt.execute(q_create);


            String q_insert = String.format("INSERT INTO %s VALUES (?,?,?,?,?)", TABLE_PACKAGES);
            PreparedStatement pstmt_insert = conn.prepareStatement(q_insert);

            final int BATCH_SIZE = 500000, total_packages = packageDeclarationList.size();;
            int lastReportedProgress = -1, currentProgress;
            int index=0;
            for(PackageDeclaration p: packageDeclarationList)
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


    public static Map<String/*RepoFullname*/, Set<PackageDeclaration>> readAllRepoPackages(String resultPath)
    {
        /*Connection conn = null;
        Map<String, Set<PackageDeclaration>> all = new HashMap<>();
        try {
            String sqlitePath = "jdbc:sqlite:" + resultPath;
            conn = DriverManager.getConnection(sqlitePath);
            Statement stmt = conn.createStatement();
            String query =  String.format("SELECT R.repoId, R.repoFullname, P.packageDefId, P.package\n" +
                    "FROM %s R JOIN %s P on R.repoId = P.repoId", RESULT_TABLE_NAME_PACKAGE_REPO, RESULT_TABLE_NAME_PACKAGE_P);
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next())
            {
//                int repoId = rs.getInt("repoId");
                String projectName = rs.getString("project_name");
                String commitSHA = rs.getString("commit_sha");
                String packageName = rs.getString("package");

                var fff = new PackageDeclaration(packageName, null, null);
                fff.commitSHA = commitSHA;
                if(all.containsKey(repoFullname))
                {
                    Set<PackageDeclaration> packagesForThisLibId = all.get(repoFullname);
                    packagesForThisLibId.add( );
                }
                else
                {
                    Set<PackageDeclaration> packagesForThisLibId_new = new HashSet<>();
                    packagesForThisLibId_new.add( fff);
                    all.put(repoFullname, packagesForThisLibId_new);
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

        return all;*/
        return null;
    }

}
