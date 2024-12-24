/* This project is licensed under the Mulan PSL v2.
 You can use this software according to the terms and conditions of the Mulan PSL v2.
 You may obtain a copy of Mulan PSL v2 at:
     http://license.coscl.org.cn/MulanPSL2
 THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR
 PURPOSE.
 See the Mulan PSL v2 for more details.
 Create: 2023
*/

package com.datastat.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.opencsv.CSVReader;

import lombok.SneakyThrows;

public class CsvFileUtil {
    private static final Logger logger = LoggerFactory.getLogger(CsvFileUtil.class);
    public static List<HashMap<String, Object>> readFile(String file) {
        try {
            BufferedReader textFile = new BufferedReader(new FileReader(new File(file)));
            String lineDta;
            int lineNum = 0;
            String[] header = null;
            ArrayList<HashMap<String, Object>> res = new ArrayList<>();
            while ((lineDta = textFile.readLine()) != null) {
                if (lineNum == 0) {
                    header = lineDta.split(",");
                } else {
                    HashMap<String, Object> dataMap = new HashMap<>();
                    String[] datas = lineDta.split(",");
                    for (int i = 0; i < header.length; i++) {
                        dataMap.put(header[i], datas[i]);
                    }
                    res.add(dataMap);
                }
                lineNum++;
            }

            return res;

        } catch (Exception e) {
            logger.error("read file exception - {}", e.getMessage());
            return null;
        }
    }

    public static List<HashMap<String, Object>> readCsv(String filePath) {
        List<HashMap<String, Object>> records = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] line;
            String[] headers = reader.readNext();
            while ((line = reader.readNext()) != null) {
                HashMap<String, Object> recordMap = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    recordMap.put(headers[i], line[i]);
                }
                records.add(recordMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return records;
    }

    public static List<HashMap<String, Object>> getZipFile(String zipUrl, String downloadDir) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            zipUrl = URLDecoder.decode(zipUrl, "UTF-8");

            ResponseEntity<byte[]> response = restTemplate.getForEntity(zipUrl, byte[].class);
            byte[] zipData = response.getBody();
            try (FileOutputStream fos = new FileOutputStream(downloadDir + ".zip")) {
                fos.write(zipData);
            }
        } catch (Exception e) {
            logger.error("getZipFile exception", e);
        }

        List<String> paths = unzip(downloadDir + ".zip", downloadDir);
        List<HashMap<String, Object>> fileContent = new ArrayList<>();
        for (String path : paths) {
            List<HashMap<String, Object>> res = readCsv(path);
            fileContent.addAll(res);
        }
        return fileContent;
    }

    public static List<String> unzip(String zipFilePath, String destDirectory) {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        List<String> filePaths = new ArrayList<>();
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    extractFile(zipIn, filePath);
                } else {
                    File dir = new File(filePath);
                    dir.mkdirs();
                }
                zipIn.closeEntry();
                filePaths.add(filePath);
                entry = zipIn.getNextEntry();
            }
        } catch (Exception e) {
            logger.error("unzip exception", e);
        }
        return filePaths;
    }

    @SneakyThrows
    private static void extractFile(ZipInputStream zipIn, String filePath) {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] bytesIn = new byte[4096];
            int read;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }

}
