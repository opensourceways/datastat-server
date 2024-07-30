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

import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;


public class YamlUtil {
    private static final Logger logger = LoggerFactory.getLogger(YamlUtil.class);
    public <T> T readLocalYaml(String yamlFile, Class<T> classType) {
        Yaml yaml = new Yaml();
        InputStream inputStream;
        T t = null;
        try {
            inputStream = new FileInputStream(yamlFile);
            t = yaml.loadAs(inputStream, classType);
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return t;
    }

    public <T> T readYaml(String yamlFile) {
        Yaml yaml = new Yaml();
        InputStream inputStream;
        T t = null;
        try {
            inputStream = new FileInputStream(yamlFile);
            t = yaml.load(inputStream);
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return t;
    }

    public String wget(String urlFile, String path) {
        Runtime runtime = Runtime.getRuntime();
        String cmd = String.format("wget -N -P %s %s", path, urlFile);

        String[] sp = urlFile.split("/");
        String localFile = path + sp[sp.length - 1];

        try {
            Process process = runtime.exec(cmd);
            InputStream is1 = process.getInputStream();
            InputStream is2 = process.getErrorStream();
            new Thread() {
                @Override
                public void run() {
                    BufferedReader bi = null;
                    String line;
                    try {
                        bi = new BufferedReader(new InputStreamReader(is1));
                        ;
                        while ((line = bi.readLine()) != null) {
                        }
                    } catch (Exception e) {
                        logger.error("exception", e);
                    } finally {
                        if (bi != null) {
                            try {
                                bi.close();
                            } catch (Exception e) {
                                logger.error("exception", e);
                            }
                        }
                    }
                }
            }.start();
            new Thread() {
                @Override
                public void run() {
                    BufferedReader bi = null;
                    String line;
                    try {
                        bi = new BufferedReader(new InputStreamReader(is2));
                        ;
                        while ((line = bi.readLine()) != null) {
                        }
                    } catch (Exception e) {
                        logger.error("exception", e);
                    } finally {
                        if (bi != null) {
                            try {
                                bi.close();
                            } catch (Exception e) {
                                logger.error("exception", e);
                            }
                        }
                    }
                }
            }.start();
            process.waitFor();
        } catch (Exception e) {
            logger.error("exception", e);
        }

        return localFile;
    }
}
