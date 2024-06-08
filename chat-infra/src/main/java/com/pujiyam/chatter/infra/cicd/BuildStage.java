package com.pujiyam.chatter.infra.cicd;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.Stage;
import software.amazon.awscdk.StageProps;
import software.amazon.awscdk.services.codebuild.*;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class BuildStage extends Stage {

    public BuildStage(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public BuildStage(final Construct scope, final String id, final StageProps props) {
        super(scope, id, props);

        IRepository imgRepo = Repository.fromRepositoryName(this, "ChatterRepo", "cdk-hnb659fds-container-assets-984235857022-us-east-1");

        BuildEnvironment buildEnv = BuildEnvironment.builder()
                .buildImage(LinuxArmBuildImage.AMAZON_LINUX_2_STANDARD_3_0)
                .privileged(true)
                .environmentVariables(Map.of(
                        "AWS_REGION", buildVar(Stack.of(this).getRegion()),
                        "AWS_ACCOUNT_ID", buildVar(Stack.of(this).getAccount()),
                        "IMAGE_REPO_URI", buildVar(imgRepo.getRepositoryUri())
                ))
                .build();

        PipelineProject project = PipelineProject.Builder
                .create(this, "ChatterProject")
                .buildSpec(BuildSpec.fromAsset("buildspec.yml"))
                .environment(buildEnv)
                .build();

        imgRepo.grantPullPush(project.getGrantPrincipal());
    }

    public BuildEnvironmentVariable buildVar(Object value) {
        return BuildEnvironmentVariable.builder()
                .value(value)
                .build();
    }
}
