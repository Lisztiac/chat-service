package com.pujiyam.chatter.infra;

import com.pujiyam.chatter.infra.cicd.PipelineStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.io.IOException;

public class ChatInfraApp {

    private final static String DEFAULT_ACCOUNT = "984235857022";

    private final static String DEFAULT_REGION = "us-east-1";

    public static void main(final String[] args) throws IOException {
        App app = new App();

        Environment defaultEnv = getEnv(DEFAULT_ACCOUNT, DEFAULT_REGION);

        new PipelineStack(app, "PipelineStack", StackProps.builder()
                .env(defaultEnv)
                .build());

        app.synth();
    }

    private static Environment getEnv(String account, String region) {
        return Environment.builder()
                .account(account)
                .region(region)
                .build();
    }
}

