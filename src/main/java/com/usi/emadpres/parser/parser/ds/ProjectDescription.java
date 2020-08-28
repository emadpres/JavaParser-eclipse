package com.usi.emadpres.parser.parser.ds;

public class ProjectDescription {
    // Can be repo full name (emadpres/ctm),
    // or Maven library identifier (com.alibaba:fastjson:1.2.46)
    public String projectId;

    // path to root of project
    public String fullPath;

    ProjectDescription(String projectId, String fullPath)
    {
        this.projectId = projectId;
        this.fullPath = fullPath;
    }


    @Override
    public String toString() {
        return projectId + " | " + fullPath;
    }
}
