package com.usi.emadpres.parser.parser.db;

import com.usi.emadpres.parser.parser.ds.UserTypeDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.util.*;

public class UserTypeDeclarationDB {
    private static final Logger logger = LoggerFactory.getLogger(UserTypeDeclarationDB.class);
    public static final String TABLE_USER_TYPE_DECLARATIONS = "UserTypeDeclaration";

    public static void WriteToSQLite(Set<UserTypeDeclaration> userTypeDeclarationList, String path)
    {
        if(userTypeDeclarationList==null) return;

        Connection conn = null;
        String sqlitePath = "jdbc:sqlite:" + path;

        try {
            logger.info("Writing User Type Declaration at {}", path);

            File parentDir = new File(path).getParentFile();
            if(parentDir.exists() == false)
                parentDir.mkdirs();


            conn = DriverManager.getConnection(sqlitePath);
            conn.setAutoCommit(false); // --> you should call conn.commit();
            java.sql.Statement stmt = conn.createStatement();



            String q_drop = String.format("DROP TABLE IF EXISTS %s;", TABLE_USER_TYPE_DECLARATIONS);
            String q_create = String.format("CREATE TABLE %s (type_decl_Id INTEGER, project_name INTEGER, type_name TEXT, type_category INTEGER, is_binded INTEGER, path TEXT)", TABLE_USER_TYPE_DECLARATIONS); // No "PRIMARY KEY(XX)". Makes the insertion slow.

            stmt.execute(q_drop);
            stmt.execute(q_create);


            String q_insert = String.format("INSERT INTO %s VALUES (?,?,?,?,?,?)", TABLE_USER_TYPE_DECLARATIONS);
            PreparedStatement pstmt_insert = conn.prepareStatement(q_insert);

            final int BATCH_SIZE = 500000, total_userTypes = userTypeDeclarationList.size();
            int lastReportedProgress = -1, currentProgress;
            int index=0;
            for(UserTypeDeclaration u: userTypeDeclarationList)
            {
                index++;

                currentProgress = (int)((index*100.0)/ total_userTypes);
                if(currentProgress-lastReportedProgress>= 1)
                {
                    lastReportedProgress = currentProgress;
                    //logger.info("%{} ({}/{})", currentProgress, index, total_userTypes-1);
                }

                pstmt_insert.setInt(1, index);
                pstmt_insert.setString(2, u.projectName);
                pstmt_insert.setString(3, u.fullyQualifiedName);
                pstmt_insert.setInt(4, u.typeDeclType.ordinal());
                pstmt_insert.setInt(5, u.isBinded==true?1:0);
                pstmt_insert.setString(6, u.fileRelativePath);
                pstmt_insert.addBatch();
                if(index%BATCH_SIZE==0) {
                    pstmt_insert.executeBatch();
                }
            }
            pstmt_insert.executeBatch();
            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static Map<String/*RepoFullname*/, Set<String>> readAllRepoTypes(String resultPath)
    {
        /*Connection conn = null;
        Map<String, Set<String>> all = new HashMap<>();
        try {
            String sqlitePath = "jdbc:sqlite:" + resultPath;
            conn = DriverManager.getConnection(sqlitePath);
            Statement stmt = conn.createStatement();
            String query =  String.format("SELECT R.repoId, R.repoFullname, P.typeName\n" +
                    "FROM %s R JOIN %s P on R.repoId = P.repoId", RESULT_TABLE_NAME_REPOS, RESULT_TABLE_NAME_TYPE);
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next())
            {
                String repoFullname = rs.getString("repoFullname");
                String typeName = rs.getString("typeName");


                if(all.containsKey(repoFullname))
                {
                    Set<String> packagesForThisLibId = all.get(repoFullname);
                    packagesForThisLibId.add(typeName);
                }
                else
                {
                    Set<String> packagesForThisLibId_new = new HashSet<>();
                    packagesForThisLibId_new.add(typeName);
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
