package org.agmip.translators.mondata;

import au.com.bytecode.opencsv.CSVReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.agmip.ace.util.AcePathfinderUtil;
import org.agmip.core.types.TranslatorInput;
import org.agmip.translators.mondata.MondataAgmipMappingHelper.ConvertType;
import static org.agmip.util.MapUtil.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mondata Experiment Data I/O API Class
 *
 * @author Meng Zhang
 * @version 1.0
 */
public abstract class MondataCSVInput implements TranslatorInput {
    
    private enum DataType {
        
        EXPERIMENT, SOIL_LAYER, WEATHER_STATION, OTHER
    }
    private static final Logger log = LoggerFactory.getLogger(MondataCSVInput.class);

    /**
     * Mondata XFile Data input method, always return the first data object
     *
     * @param filePath file name
     * @return result data holder object
     */
    @Override
    public HashMap readFile(String filePath) throws FileNotFoundException, IOException {
        
        ArrayList<HashMap> expDataArr = new ArrayList();
        ArrayList<HashMap> wthDataArr = new ArrayList();
        ArrayList<HashMap> soilDataArr = new ArrayList();
        
        LinkedHashMap<String, HashMap> expDataMap;
        LinkedHashMap<String, HashMap> soilDataMap = new LinkedHashMap();
        LinkedHashMap<String, HashMap> wthDataMap = new LinkedHashMap();
        HashMap ret = new HashMap();


        // Get CsvReader for each CSV file
        HashMap<String, List<String[]>> csvFiles = getCsvReaders(filePath);

        // Read experiment data
        expDataMap = readExpData(csvFiles);

        // Read weather and soil data
        for (String fileName : csvFiles.keySet()) {
            List<String[]> reader = csvFiles.get(fileName);
            if (reader == null || reader.isEmpty()) {
                log.debug("Can not read content from " + fileName);
                continue;
            }
            String[] titles = reader.get(0);
            
            LinkedHashMap<String, Integer> titleToId;
            if (titles == null) {
                log.debug("There is no content in the file of " + fileName);
                continue;
            } else {
                titleToId = getTitles(titles);
            }
            
            HashMap expData;
            for (int i = 1; i < reader.size(); i++) {
                String[] values = reader.get(i);
                String exname = values[titleToId.get("exname")];
                if (!expDataMap.containsKey(exname)) {
                    expData = new HashMap();
                    expDataMap.put(exname, expData);
                } else {
                    expData = expDataMap.get(exname);
                }
                int limit = Math.min(titles.length, values.length);
                for (int j = 0; j < limit; j++) {
                    if (!values[j].trim().equals("")) {
                        ConvertType convType = MondataAgmipMappingHelper.getConvertType(titles[j]);
                        if (ConvertType.DATE.equals(convType)) {
                            values[j] = translateDateStr(values[j]);
                        }
                        AcePathfinderUtil.insertValue(expData, titles[j], values[j].trim(), 0);
                    }
                }

                // Convert mon_wsta_distfromfield to mon_wst_infoX (X = [1,) )
                if (judgeContentType(titleToId).equals(DataType.WEATHER_STATION)) {
                    String wstDist = (String) expData.remove("mon_wsta_distfromfield");
                    String wstId = (String) expData.remove("wst_id");
                    String monWstInfo = wstId + "|" + wstDist;
                    int count = 1;
                    while (expData.containsKey("mon_wst_info" + count)) {
                        count++;
                    }
                    System.out.println(exname + ", mon_wst_info" + count + ", " + monWstInfo);
                    expData.put("mon_wst_info" + count, monWstInfo);
                }
            }
        }

        // combine the maps
        for (HashMap expData : expDataMap.values()) {
            addRecord(soilDataMap, (HashMap) expData.remove("soil"), "soil_id", soilDataArr);
            addRecord(wthDataMap, (HashMap) expData.remove("weather"), "wst_id", wthDataArr);
        }
        ret.put("experiments", combineData(expDataMap, expDataArr));
        ret.put("soils", combineData(soilDataMap, soilDataArr));
        ret.put("weathers", combineData(wthDataMap, wthDataArr));
        
        return ret;
    }
    
    private void addRecord(HashMap<String, HashMap> to, HashMap record, String keyName, ArrayList<HashMap> toArr) {
        if (record != null && !record.isEmpty()) {
            String key = getObjectOr(record, keyName, "");
            if (key.equals("")) {
                toArr.add(record);
            } else {
                to.put(key, record);
            }
        }
    }
    
    private ArrayList<HashMap> combineData(HashMap<String, HashMap> toMap, ArrayList<HashMap> toArr) {
        ArrayList<HashMap> ret = new ArrayList();
        for (HashMap m : toMap.values()) {
            ret.add(m);
        }
        ret.addAll(toArr);
        return ret;
    }
    
    private LinkedHashMap<String, HashMap> readExpData(HashMap<String, List<String[]>> csvFiles) throws IOException {
        
        ArrayList<String> unreadable = new ArrayList();
        LinkedHashMap<String, HashMap> expDataMap = new LinkedHashMap();
        
        for (String fileName : csvFiles.keySet()) {
            List<String[]> reader = csvFiles.get(fileName);
            if (reader == null || reader.isEmpty()) {
                log.debug("Can not read content from " + fileName);
                unreadable.add(fileName);
                continue;
            }
            String[] titles = reader.get(0);
            LinkedHashMap<String, Integer> titleToId;
            if (titles == null) {
                log.debug("Can not read title line from " + fileName);
                unreadable.add(fileName);
                continue;
            } else {
                titleToId = getTitles(titles);
            }
            
            if (judgeContentType(titleToId).equals(DataType.EXPERIMENT)) {

//                expDataArr = new ArrayList();
                HashMap data;
                int exnameCol = titleToId.get("exname");

//                String[] values;
//                while ((values = reader.readNext()) != null) {
                for (int i = 1; i < reader.size(); i++) {
                    String[] values = reader.get(i);
                    String exname = values[exnameCol];
                    if (!expDataMap.containsKey(exname)) {
                        data = new HashMap();
//                        expDataArr.add(data);
                        expDataMap.put(exname, data);
                    } else {
                        data = expDataMap.get(exname);
                    }
                    int limit = Math.min(titles.length, values.length);
                    for (int j = 0; j < limit; j++) {
                        if (!values[j].trim().equals("")) {
                            AcePathfinderUtil.insertValue(data, titles[j], values[j].trim());
                        }
                    }
                }
                csvFiles.remove(fileName);
                break;
            }
        }
        
        for (int i = 0; i < unreadable.size(); i++) {
            csvFiles.remove(unreadable.get(i));
        }
        
        return expDataMap;
    }
    
    protected LinkedHashMap<String, Integer> getTitles(String[] titleLine) {
        LinkedHashMap<String, Integer> ret = new LinkedHashMap();
        for (int i = 0; i < titleLine.length; i++) {
            titleLine[i] = MondataAgmipMappingHelper.getAgmipName(titleLine[i]);
            ret.put(titleLine[i], i);
        }
        return ret;
    }

    /**
     * Judge data type
     *
     * @param titleToId the map contain titles of csv data and their index
     * number of order
     * @return the type of csv data
     */
    private DataType judgeContentType(HashMap<String, Integer> titleToId) {
        
        if (titleToId.containsKey("wst_id")) {
            return DataType.WEATHER_STATION;
        } else if (titleToId.containsKey("mon_soilhorizonid")) {
            return DataType.SOIL_LAYER;
        } else if (titleToId.containsKey("exname")) {
            return DataType.EXPERIMENT;
        } else {
            return DataType.OTHER;
        }
    }

    /**
     * Translate data str from "dd.MM.yyyy HH:mm:ss" to "yyyyMMdd"
     *
     * @param datetime date string with format of "dd.MM.yyyy HH:mm:ss"
     * @return result date string with format of "yyyyMMdd"
     */
    protected String translateDateStr(String datetime) {

        // Initial Calendar object
        if (datetime == null || datetime.trim().equals("")) {
            return "";
        }
        try {
            String[] tmp = datetime.split("[\\._ /\\\\]");
            // Set date with input value
            int date = Integer.parseInt(tmp[0]);
            int month = Integer.parseInt(tmp[1]);
            int year = Integer.parseInt(tmp[2]);
            // translatet to yyddd format
            return String.format("%1$04d%2$02d%3$02d", year, month, date);
        } catch (Exception e) {
            // if tranlate failed, then use default value for date
            // sbError.append("! Waring: There is a invalid date [").append(startDate).append("]");
            return datetime;
        }
        
    }

    /**
     * Get BufferReader for each type of file
     *
     * @param filePath the full path of the input file
     * @return result the holder of BufferReader for different type of files
     * @throws FileNotFoundException
     * @throws IOException
     */
    protected HashMap<String, List<String[]>> getCsvReaders(String filePath) throws FileNotFoundException, IOException {
        
        HashMap<String, List<String[]>> result = new HashMap();

        // If input File is ZIP file
        if (filePath.toUpperCase().endsWith(".ZIP")) {
            
            ZipFile zf = new ZipFile(filePath);
            Enumeration<? extends ZipEntry> e = zf.entries();
            while (e.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                if (entry.getName().toUpperCase().endsWith(".CSV")) {
                    CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(zf.getInputStream(entry))));
//                    ArrayList<String[]> arr = new ArrayList();
//                    String[] strs;
//                    while ((strs = reader.readNext()) != null) {
//                        arr.add(strs);
//                    }
                    result.put(
                            entry.getName().toUpperCase(),
                            reader.readAll());
                }
            }
            zf.close();
        } // If input File is not ZIP file
        else {
            File f = new File(filePath);
            if (filePath.toUpperCase().endsWith(".CSV")) {
                result.put(
                        f.getName().toUpperCase(),
                        new CSVReader(new BufferedReader(new InputStreamReader(new FileInputStream(filePath)))).readAll());
            }
        }
        
        return result;
    }
}
