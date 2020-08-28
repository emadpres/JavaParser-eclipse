package com.usi.emadpres.parser.parser;

import com.usi.emadpres.MavenUtils.ds.MavenLibInfo;
import com.usi.emadpres.parser.parser.ds.ParserConfig;
import com.usi.emadpres.parser.parser.ds.ProjectParsingResult;
import com.usi.emadpres.parser.parser.ParserCore.EclipseJavaParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A helper class which prepare projects dependencies (={@code classpathArr}) before being parsed by {@link EclipseJavaParser}
 *
 * Why we dont't merge it into {@link EclipseJavaParser}?
 *      Because it would pollute {@link EclipseJavaParser} with methods such as {@link ProjectParser#ConvertMavenDependenciesToJarPaths}
 */
public class ProjectParser {
    private static final Logger logger = LoggerFactory.getLogger(ProjectParser.class);


    public static void main(String[] args) {
        {
            // Testing on Maven Library
            MavenLibInfo libInfo = new MavenLibInfo("com.alibaba", "fastjson", "1.2.46");
            ProjectParsingResult result = ProjectParser.ParseProject(libInfo.GetIdentifier(), null, libInfo.GetPathToSrcDir(), null, new ParserConfig(true, true, true));
        }

        {
            // Testing on Java Project
            ProjectParsingResult result = ProjectParser.ParseProject("emadpres/some-project", null, "path-to-project", null, new ParserConfig(true, true, true));
        }
    }


    /**
     * @param dependencies (Optional)
     */
    public static ProjectParsingResult ParseProject(String projectName, String commitSHA, String projectPath, List<MavenLibInfo> dependencies, ParserConfig config)
    {
        String[] classpathArr = ConvertMavenDependenciesToJarPaths(dependencies, projectName);
        ProjectParsingResult result = EclipseJavaParser.ParseProject(projectName, commitSHA, projectPath, classpathArr, config);
        return result;
    }

    /**
     * @param dependencies (Optional)
     */
    public static ProjectParsingResult ParseProjectFiles(String projectName, String commitSHA, String projectPath, List<String> fileRelativePaths, List<MavenLibInfo> dependencies, ParserConfig config)
    {
        String[] classpathArr = ConvertMavenDependenciesToJarPaths(dependencies, projectName);
        ProjectParsingResult result = EclipseJavaParser.ParseProjectFiles(projectName, commitSHA, projectPath, fileRelativePaths, classpathArr, config);
        return result;
    }

    /**
     * @param dependencies (Optional)
     */
    public static ProjectParsingResult ParseSingleFile(String projectName, String commitSHA, String filePath, List<MavenLibInfo> dependencies, ParserConfig config)
    {
        String[] classpathArr = ConvertMavenDependenciesToJarPaths(dependencies, projectName);
        ProjectParsingResult result = EclipseJavaParser.ParseSingleFile(projectName, commitSHA, filePath, classpathArr, config);
        return result;
    }


    /**
     * Get "jar" path for dependencies from local Maven repository
     * @param dependencies
     * @param projectName_debugging
     * @return
     */
    private static String[] ConvertMavenDependenciesToJarPaths(List<MavenLibInfo> dependencies, String projectName_debugging)
    {
        if(dependencies==null)
            return new String[0];

        Set<MavenLibInfo> seenLibs = new HashSet<>();
        List<MavenLibInfo> availableLibs = new ArrayList<>();
        int miss=0;

        for(MavenLibInfo m: dependencies) {
            if(seenLibs.contains(m)) continue;
            else seenLibs.add(m);

            if (Files.exists(Paths.get(m.GetPathToLibJar())))
                availableLibs.add(m);
            else {
                miss++;
                //logger.warn("Project {}: Jar file of a dependency not found: {}", projectId_debugging, m);
            }
        }
//        if(miss>0)
//            logger.info("{} --> {} out of {} dependencies are not downloaded to local ~/.m2/ folder.", projectName_debugging, miss, dependencies.size());


        String[] classpathArr = new String[availableLibs.size()];
        for(int i=0; i<availableLibs.size(); i++)
            classpathArr[i] = availableLibs.get(i).GetPathToLibJar();

        return classpathArr;
    }
}
