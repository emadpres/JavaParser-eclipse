package com.emadpres.javaparsermains;

import com.usi.emadpres.parser.parser.ParserCore.EclipseJavaParser;
import com.usi.emadpres.parser.parser.db.ProjectParsingResultDB;
import com.usi.emadpres.parser.parser.ds.ParserConfig;
import com.usi.emadpres.parser.parser.ds.ProjectParsingResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ParsingApacheCommons {

    private static final Logger logger = LoggerFactory.getLogger(ParsingApacheCommons.class);

    public static void main(String[] args) {

        File dir = new File("/Users/emadpres/code/postdoc-usi/APIRep/apache-commons-libs/");
        File[] directoryListing = dir.listFiles();

        ParserConfig parserConfig = new ParserConfig(false, true, true, false);

        List<ProjectParsingResult> allProjectParsingResults = new ArrayList<>();
        int libIndex = 0;
        for (File child : directoryListing) {
            if(child.isDirectory())
            {
                logger.info("Parsing #{}, {} ({})", ++libIndex, child.getName(), child.getPath());
                ProjectParsingResult res = EclipseJavaParser.ParseProject(child.getName(), null, child.getPath(), null, parserConfig);

                allProjectParsingResults.add(res);
            }
        }

        ProjectParsingResult merged = ProjectParsingResultDB.Merge(allProjectParsingResults);
        ProjectParsingResultDB.WriteToSQLite(merged, Path.of("/Users/emadpres/code/postdoc-usi/APIRep/apache-commons-libs/apache-commons.sqlite"));
    }
}
