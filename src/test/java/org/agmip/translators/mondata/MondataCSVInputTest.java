package org.agmip.translators.mondata;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import org.agmip.util.JSONAdapter;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for simple App.
 *
 * @author Meng Zhang
 */
public class MondataCSVInputTest {

    MondataCSVInput input;
    URL resource;
    URL expectedRes;
    String fileName = "qryAgMIPS_Extract_Phase1.zip";
    String expectedRetfileName = "qryAgMIPS_Extract_Phase1.json";

    @Before
    public void setUp() throws Exception {
        input = new MondataCSVInput();
        resource = this.getClass().getResource("/" + fileName);
    }

    @Test
    public void test() throws IOException, Exception {
        HashMap result;
        HashMap expected;

        // Get running result
        result = input.readFile(resource.getPath());

        // Output json for reading
//        BufferedOutputStream bo;
//        File f = new File(fileName.replaceAll("\\.\\w+$", ".json"));
//        bo = new BufferedOutputStream(new FileOutputStream(f));
//        bo.write(JSONAdapter.toJSON(result).getBytes());
//        bo.close();
//        f.delete();

        // Get expected json
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(expectedRetfileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line.trim());
        }
        expected = JSONAdapter.fromJSON(sb.toString());

        if (result != null) {
            assertTrue(!result.isEmpty());
            int expRet = loopCompare(result, expected, "experiments", "exname");
            int soilRet = loopCompare(result, expected, "soils", "soil_id");
            int wthRet = loopCompare(result, expected, "weathers", "wst_id");
            assertEquals(expRet, 0);
            assertEquals(soilRet, 0);
            assertEquals(wthRet, 0);
        }
    }

    private int loopCompare(HashMap result, HashMap expected, String priKey, String subKey) {
        ArrayList<HashMap> expArrEpc = (ArrayList) expected.get(priKey);
        ArrayList<HashMap> expArrRet = (ArrayList) result.get(priKey);
        return compareList(expArrEpc, expArrRet, priKey);
    }

    private int compareMap(HashMap<String, Object> expected, HashMap<String, Object> result, String upperKey) {

        int count = 0;
        if (expected == null || result == null) {
            try {
                assertEquals(expected, result);
            } catch (Error e) {
                count++;
                System.out.println(upperKey + "\t" + e.getMessage());
            }
            return count;
        }

        ArrayList<String> expKeys = new ArrayList(expected.keySet());
        ArrayList<String> retKeys = new ArrayList(result.keySet());

        for (int i = 0; i < expKeys.size(); i++) {
            String key = expKeys.get(i);
            Object expVal = expected.get(key);
            Object retVal = result.get(key);
            if (expVal instanceof HashMap) {
                count += compareMap((HashMap) expVal, (HashMap) retVal, upperKey + "\t" + key);
            } else if (expVal instanceof ArrayList) {
                count += compareList((ArrayList) expVal, (ArrayList) retVal, upperKey + "\t" + key);
            } else {
                try {
                    assertEquals(expVal, retVal);
                } catch (Error e) {
                    count++;
                    System.out.print(upperKey);
                    System.out.print("\t" + String.format("%-16s ", key));
                    System.out.println(e.getMessage());
                }
            }
            retKeys.remove(key);
        }

        for (int i = 0; i < retKeys.size(); i++) {
            String key = retKeys.get(i);
            Object expVal = expected.get(key);
            Object retVal = result.get(key);
            if (expVal instanceof HashMap) {
                count += compareMap((HashMap) expVal, (HashMap) retVal, upperKey + "\t" + key);
            } else if (expVal instanceof ArrayList) {
                count += compareList((ArrayList) expVal, (ArrayList) retVal, upperKey + "\t" + key);
            } else {
                try {
                    assertEquals(expVal, retVal);
                } catch (Error e) {
                    count++;
                    System.out.print(upperKey);
                    System.out.print("\t" + String.format("%-16s", key));
                    System.out.println(e.getMessage());
                }
            }
        }
        return count;

    }

    private int compareList(ArrayList<HashMap> expected, ArrayList<HashMap> result, String key) {

        int count = 0;
        int limit = Math.min(expected.size(), result.size());
        if (limit != expected.size()) {
            count++;
            System.out.println(key + "\tAcutally " + (limit - expected.size()) + " more records");
        }
        if (limit != result.size()) {
            count++;
            System.out.println(key + "\tExpected " + (limit - expected.size()) + " more records");
        }
        if (key.contains("weathers")) {
            for (int i = 0; i < expected.size(); i++) {

                String wstId = (String) expected.get(i).get("wst_id");
                if (wstId == null) {
                    count++;
                    System.out.println(key + "\tMissing WST_ID in expeceted JSON at [" + i + "]");
                    continue;
                }
                HashMap retData = null;
                for (int j = 0; j < result.size(); j++) {
                    if (wstId.equals(result.get(j).get("wst_id"))) {
                        retData = result.get(j);
                        break;
                    }
                }
                count += compareMap(expected.get(i), retData, key + "[" + i + "]\t" + wstId + "\t");
            }
        } else {
            for (int i = 0; i < limit; i++) {
                count += compareMap(expected.get(i), result.get(i), key + "[" + i + "]");
            }
        }
        return count;
    }
}
