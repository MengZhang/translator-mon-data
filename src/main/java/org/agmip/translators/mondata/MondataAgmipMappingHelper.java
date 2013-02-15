package org.agmip.translators.mondata;

import au.com.bytecode.opencsv.CSVReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MON <-> AgMIP Mapping Relation Helper class
 *
 * @author Meng Zhang
 */
public class MondataAgmipMappingHelper {

    public enum ConvertType {

        DATE
    }
    private static HashMap<String, String> monToAgMIP;
    private static HashMap<String, ConvertType> convertMap;
    private static long lastModified = 0;
    private static int TYPE_COL = 0;
    private static int MON_COL = 1;
    private static int AGMIP_COL = 2;
    private static int COMMENT_COL = 3;
    private static String COMMENT = "!";
    private static String TITLE = "#";
    private static String VALUE = "*";
    private static String MAPPING_FILE = "/mapping.csv";
    private static final Logger LOG = LoggerFactory.getLogger(MondataAgmipMappingHelper.class);

    /**
     * Load mapping CSV file
     */
    public static void loadMapping() {

        // Check if mapping exists
        URL mappingUrl = MondataAgmipMappingHelper.class.getResource(MAPPING_FILE);
        if (mappingUrl == null) {
            LOG.error("Mapping file [" + MAPPING_FILE + "] is missing.");
            return;
        }

        // Check if there is new change on mapping file
        File f = new File(mappingUrl.getPath());
        if (lastModified < f.lastModified()) {
            LOG.info("Loading mapping file...");

            // Initializing mapping map
            monToAgMIP = new HashMap();
            convertMap = new HashMap();

            // Load file
            InputStream stream = MondataAgmipMappingHelper.class.getResourceAsStream(MAPPING_FILE);
            BufferedReader br = new BufferedReader(new InputStreamReader(stream));
            CSVReader reader = new CSVReader(br);
            String[] nextLine;

            try {
                while ((nextLine = reader.readNext()) != null) {
                    // Only record the mapping with different name between MON and AgMIP
                    if (nextLine[TYPE_COL].contains(VALUE)) {
                        nextLine[MON_COL] = nextLine[MON_COL].toLowerCase();
                        nextLine[AGMIP_COL] = nextLine[AGMIP_COL].toLowerCase();
                        if (!nextLine[MON_COL].equalsIgnoreCase(nextLine[AGMIP_COL])) {
                            if (monToAgMIP.containsKey(nextLine[MON_COL])) {
                                LOG.warn("Conflict with variable: " + nextLine[MON_COL] + " Original Value: " + monToAgMIP.get(nextLine[MON_COL]) + "; New Value: " + nextLine[AGMIP_COL]);
                            }
                            monToAgMIP.put(nextLine[MON_COL], nextLine[AGMIP_COL]);
                        }

                        nextLine[COMMENT_COL] = nextLine[COMMENT_COL].toUpperCase();
                        if (nextLine[COMMENT_COL].contains("CONVERT")) {
                            if (nextLine[COMMENT_COL].contains(ConvertType.DATE.toString())) {
                                convertMap.put(nextLine[AGMIP_COL], ConvertType.DATE);
                            }
                        }
                    }
                }

                // Save lastest modified time of mapping file
                lastModified = f.lastModified();
                LOG.info("Finished");
                LOG.debug("Loading result:{}", monToAgMIP);
                reader.close();

            } catch (FileNotFoundException e) {
                LOG.info("Failed");
                LOG.error(e.getMessage());
            } catch (IOException e) {
                LOG.info("Failed");
                LOG.error(e.getMessage());
            }

        } else {
            LOG.debug("No change on mapping file, keep using last loaded records.");
        }
    }

    /**
     * Get AgMIP variable name related to the given MON variable name
     *
     * @param monName The MON variable name
     * @return The AgMIP variable name, if nod found in the mapping map, then
     * return the lower case of input
     */
    public static String getAgmipName(String monName) {
        String ret = "";
        if (monName != null) {
            loadMapping();
            ret = monToAgMIP.get(monName.toLowerCase());
            if (ret == null) {
                ret = monName.toLowerCase();
            }
        }
        LOG.debug("Find mapping for <MON> " + monName + ": <AgMIP> " + ret);
        return ret;
    }

    /**
     * Get the convert type for given variable name
     * 
     * @param name The variable name (Mondata or AgMIP)
     * @return The convert type (defined by mapping file)
     */
    public static ConvertType getConvertType(String name) {

        if (!convertMap.containsKey(name)) {
            return convertMap.get(monToAgMIP.get(name));
        } else {
            return convertMap.get(name);
        }
    }
}
