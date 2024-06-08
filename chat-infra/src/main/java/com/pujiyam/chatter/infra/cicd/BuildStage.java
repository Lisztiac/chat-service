package com.pujiyam.chatter.infra.cicd;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.Stage;
import software.amazon.awscdk.StageProps;
import software.amazon.awscdk.services.codebuild.*;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class BuildStage extends Stage {

    public BuildStage(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public BuildStage(final Construct scope, final String id, final StageProps props) {
        super(scope, id, props);

        GitHubSourceProps ghProps = GitHubSourceProps.builder()
                .owner("Lisztiac")
                .repo("")
                .webhookFilters(List.of(
                        FilterGroup.inEventOf(EventAction.PUSH)
                                .andBranchIs("main")))
                .build();

        PipelineProject.Builder
                .create(this, "ChatterProject")
                .buildSpec(BuildSpec.fromAsset("buildspec.yml"))
                .environment(BuildEnvironment.builder()
                        .buildImage(LinuxArmBuildImage.AMAZON_LINUX_2_STANDARD_3_0)
                        .environmentVariables(Map.of(
                                "AWS_DEFAULT_REGION", buildVar(Stack.of(this).getRegion()),
                                "AWS_ACCOUNT_ID", buildVar(Stack.of(this).getAccount()),
                                "IMAGE_REPO_NAME", buildVar(""),
                                "IMAGE_REPO_URI", buildVar(""),
                                "IMAGE_TAG", buildVar("latest")
                        ))
                        .build());
    }

    public BuildEnvironmentVariable buildVar(Object value) {
        return BuildEnvironmentVariable.builder()
                .value(value)
                .build();
    }
}
