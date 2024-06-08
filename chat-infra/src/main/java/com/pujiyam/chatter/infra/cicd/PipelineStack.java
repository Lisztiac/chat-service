package com.pujiyam.chatter.infra.cicd;

import software.amazon.awscdk.SecretValue;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.pipelines.CodeBuildStep;
import software.amazon.awscdk.pipelines.CodePipeline;
import software.amazon.awscdk.pipelines.CodePipelineSource;
import software.amazon.awscdk.pipelines.ShellStep;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildEnvironmentVariable;
import software.amazon.awscdk.services.codebuild.LinuxArmBuildImage;
import software.amazon.awscdk.services.codebuild.Project;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageProps;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.GitHubSourceAction;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class PipelineStack extends Stack {

    public PipelineStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public PipelineStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

//        ShellStep synth = ShellStep.Builder
//                .create("Synth")
//                .input(CodePipelineSource.gitHub("Lisztiac/chat-service", "main"))
//                .commands(List.of(
//                        "cd chat-infra",
//                        "npm install -g aws-cdk",
//                        "cdk synth"))
//                .primaryOutputDirectory("chat-infra/cdk.out")
//                .build();

        IRepository imgRepo = Repository.fromRepositoryName(this, "ChatterRepo", "cdk-hnb659fds-container-assets-984235857022-us-east-1");

        BuildEnvironment buildEnv = BuildEnvironment.builder()
                .buildImage(LinuxArmBuildImage.AMAZON_LINUX_2_STANDARD_3_0)
                .privileged(true)
                .environmentVariables(Map.of(
                        "AWS_REGION", buildVar(Stack.of(this).getRegion()),
                        "AWS_ACCOUNT_ID", buildVar(Stack.of(this).getAccount()),
                        "IMAGE_REPO_URI", buildVar(imgRepo.getRepositoryUri())
                )).build();

        ShellStep synth = CodeBuildStep.Builder
                .create("Synth")
                .input(CodePipelineSource.gitHub("Lisztiac/chat-service", "main"))
                .buildEnvironment(buildEnv)
                .commands(List.of("chat-infra/build.sh"))
                .primaryOutputDirectory("chat-infra/cdk.out")
                .build();

        CodePipeline pipeline = CodePipeline.Builder
                .create(this, "ChatterPipeline")
                .pipelineName("chatter-pipeline")
                .dockerEnabledForSelfMutation(true)
                .synth(synth)
                .build();

//        pipeline.addStage(new EksStage(this, "eks-stage"));
    }

    private BuildEnvironmentVariable buildVar(Object value) {
        return BuildEnvironmentVariable.builder()
                .value(value)
                .build();
    }
}
