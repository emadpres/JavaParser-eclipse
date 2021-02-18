package com.usi.emadpres.parser.parser.ds;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class JavaFileInfo {
    public int databaseId =-1; // Useful when we want to load from db and store them with exact same id for consistency with other tables/files
    public String projectName; // repoFullName OR groupId:artifactId:version
    public String commitSHA=null;
    public String fileRelativePath; // relative to the project's root
    public String content;
    public int nLines;

    // "Remark" field are useful to store project-specific information (project = PhD project)
    public int remark=-1;
    public String remark_str=null;

    public JavaFileInfo(String projectName, String commitSHA, String fileRelativePath, int nLines, String content) {
        this.projectName = projectName;
        this.commitSHA = commitSHA;
        this.fileRelativePath = fileRelativePath;
        this.nLines = nLines;
        this.content = content;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JavaFileInfo)) {
            return false;
        }
        JavaFileInfo other = (JavaFileInfo) obj;
        EqualsBuilder builder = new EqualsBuilder();
        builder.append(projectName, other.projectName);
        builder.append(commitSHA, other.commitSHA);
        builder.append(fileRelativePath, other.fileRelativePath);
        builder.append(content, other.content);
        return builder.isEquals();
    }


    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(projectName);
        builder.append(commitSHA);
        builder.append(fileRelativePath);
        builder.append(content);
        return builder.hashCode();
    }
}
