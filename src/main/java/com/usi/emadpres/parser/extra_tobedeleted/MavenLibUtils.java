package com.usi.emadpres.parser.extra_tobedeleted;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.usi.emadpres.MavenUtils.ds.MavenLibInfo;
import com.usi.emadpres.parser.parser.ds.PackageDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Deprecated //TODO: Move this class to MavenUtils Project at github.com/emadpres/mavenutils
public class MavenLibUtils {
    private static final Logger logger = LoggerFactory.getLogger(MavenLibUtils.class);

    /**
     * Read the maven library info csv files (e.g., ./inputs/finalReleases.csv)
     * @return An array of {@Link MavenLibInfo}
     */
    public static ArrayList<MavenLibInfo> loadMavenInfo(String csvPath) {

        ArrayList<MavenLibInfo> ret = new ArrayList<>();

        CSVReader reader = null;
        try {
            reader = new CSVReader(new FileReader(csvPath));
            String[] line;
//            boolean firstLineRead=false;
            while ((line = reader.readNext()) != null) {
//                if(!firstLineRead)
//                {
//                    firstLineRead = true;
//                    continue;
//                }
                String groupId = line[0];
                String artifactId = line[1];
                String version = line[2];
                MavenLibInfo info = new MavenLibInfo(groupId,artifactId,version);
                ret.add(info);
                //System.out.println(info);
            }
        } catch (IOException | CsvValidationException e) {
            logger.error("******Error (Critical) File Not Found: ***** {}", csvPath);
            logger.error("Exception: ",e);
            e.printStackTrace();
            System.exit(1);
        }

        return ret;
    }


    /**
     * Given list of Maven libraries (where for a certain "groupId:artifactId", newer version is upper in the list)
     * Return a Map from "groupId:artifactId" mapped to an array of sorted versions.
     * If onlyIfExistsOnThisMachine=true: the version list only includes versions existing in this machine
     * Sample output entry:   "junit:junit: [4.12, 4.11, 4.10, 4.9, 4.8.2, 4.8.1, 4.8]"
     */
    public static Map<String/*groupId:artifactId*/, List<String/*version*/>>
        giveVersionsSorted(ArrayList<MavenLibInfo> allMavenInfo,
                           boolean onlyIfExistsOnThisMachine,
                           Map<String, Set<PackageDeclaration>> libIdentifierToPackages /*this last param is a workaround to make the computation more accurate*/)
    {
        Map<String/*groupId:artifactId*/, List<String/*version*/>> ret = new HashMap<>();


        for(MavenLibInfo info: allMavenInfo)
        {

            if(onlyIfExistsOnThisMachine)
            {
                if(!info.IsLibDownloadedAndSrcExtracted()
                        || !libIdentifierToPackages.containsKey(info.GetIdentifier()) /*We might have even source, but it was corrupted. so we can make sure if we actually have some source by checking if we have extracted some packages from source*/ )
                    continue;
            }


            String libIdentifierWithoutVersion = info.groupId + ":" + info.artifactId;

            if(ret.containsKey(libIdentifierWithoutVersion))
                ret.get(libIdentifierWithoutVersion).add(info.version);
            else
                ret.put(libIdentifierWithoutVersion, new ArrayList<>(Arrays.asList(info.version))); // First in list = Latest Version

        }


        return ret;
    }

    public static void checkMavenRepositoryStatus(ArrayList<MavenLibInfo> allMavenInfo) {

        Set<String> libs_all = new HashSet<>();
        Set<String> libs_withAtLeastOneAvailableSourceCode = new HashSet<>();
        ////
        Set<String> releases_all = new HashSet<>();
        Set<String> releases_downloaded_and_withSrc = new HashSet<>();
        Set<String> releases_downloaded = new HashSet<>();
        Set<String> releases_missing = new HashSet<>();

        for(MavenLibInfo info: allMavenInfo) {

            String libName = info.groupId + ":" + info.artifactId;


            libs_all.add(libName);
            releases_all.add(info.GetIdentifier());

            if (info.IsLibDownloadedAndSrcExtracted()) {
                logger.info("OK ............. {}", info);
                releases_downloaded_and_withSrc.add(info.GetIdentifier());
                libs_withAtLeastOneAvailableSourceCode.add(libName);
            } else if (info.IsLibDownloaded()) {
                releases_downloaded.add(info.GetIdentifier());
                logger.error("[NO SRC] ....... {}", info);
            } else {
                releases_missing.add(info.GetIdentifier());
                logger.error("[NOT FOUND] .... {}", info);
            }
        }

            Set<String> libs_withZeroReleaseToBeAnalyzed = new HashSet<>(libs_all);
            libs_withZeroReleaseToBeAnalyzed.removeAll(libs_withAtLeastOneAvailableSourceCode);
            logger.info("============ Release ======");
            logger.info("{} Total Releases", releases_all.size());
            logger.info("{} Downloaded + Src", releases_downloaded_and_withSrc.size());
            logger.info("{} Missing Src (Only Downloaded) ..\t.\t.\t.\t. like: {}", releases_downloaded.size(), releases_downloaded.isEmpty()?"-":releases_downloaded.iterator().next());
            logger.info("{}(={}) Missing ..\t.\t.\t.\t.\t.\t.\t. like: {}", releases_missing.size(), releases_all.size()-releases_downloaded_and_withSrc.size()-releases_downloaded.size(), releases_missing.isEmpty()?"-":releases_missing.iterator().next());
            logger.info("============ Library ======");
            logger.info("{} Total Libraries", libs_all.size());
            logger.info("{} Libs with zero release no be analyzed ..\t.\t.\t.\t. like: {}", libs_withZeroReleaseToBeAnalyzed.size(), libs_withAtLeastOneAvailableSourceCode.isEmpty()?"-":libs_withAtLeastOneAvailableSourceCode.iterator().next());
    }

}
