package com.pujiyam.chatter.infra.util;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

public class YamlUtil {

    public static Map<String, Object> load(String pathname) throws IOException {
        Yaml yaml = new Yaml();
        File file = new File(pathname);

        try (InputStream is = new FileInputStream(file)) {
            return yaml.load(is);
        }
    }
}
