package com.pujiyam.chatter.infra.util;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

public class YamlUtilTest {

    @Test
    public void should_load_contents_when_file_found() throws IOException {
        Map<String, Object> contents = YamlUtil.load("kube/deployment.yml");

        Assertions.assertTrue(contents != null && !contents.isEmpty());
    }

    @Test
    public void should_throw_when_file_not_found() {
        Assertions.assertThrows(IOException.class, () -> YamlUtil.load("not/a/real/path"));
    }
}
