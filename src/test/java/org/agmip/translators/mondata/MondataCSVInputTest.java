package org.agmip.translators.mondata;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import org.agmip.util.JSONAdapter;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for simple App.
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
        expected = JSONAdapter.fromJSON(br.readLine());
        
        if (result != null) {
            assertTrue(!result.isEmpty());
            assertEquals(expected, result);
        }
    }
    
}
