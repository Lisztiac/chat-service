package com.pujiyam.chatter.infra.cicd;

import software.amazon.awscdk.SecretValue;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.pipelines.*;
import software.amazon.awscdk.services.codebuild.*;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageProps;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.GitHubSourceAction;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.constructs.Construct;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class PipelineStack extends Stack {

    public PipelineStack(final Construct scope, final String id) throws IOException {
        this(scope, id, null);
    }

    public PipelineStack(final Construct scope, final String id, final StackProps props) throws IOException {
        super(scope, id, props);

        // Get reference to existing image repo for build-pipeline - using the repo created during bootstrapping for now
        IRepository imgRepo = Repository.fromRepositoryName(this, "ChatterRepo", "cdk-hnb659fds-container-assets-984235857022-us-east-1");

        // Output artifacts to be passed from one pipeline action to the next
        Artifact sourceOutput = new Artifact();
        Artifact buildOutput = new Artifact();

        // Build-pipeline is a lower level construct, using for finer control (pull repo, build image, upload to ECR)
        Pipeline buildPipeline = Pipeline.Builder
                .create(this, "Pipeline")
                .pipelineName("Chatter_Pipeline")
                .restartExecutionOnUpdate(true)
                .stages(List.of(
                        createSourceStage("Source", sourceOutput),
                        createImageBuildStage("ImageBuild", sourceOutput, buildOutput, imgRepo)
                )).build();

        // Defining configs for deploy-pipeline, giving same repo source as build-pipeline
        ShellStep synth = CodeBuildStep.Builder
                .create("Synth")
                .input(CodePipelineFileSet.fromArtifact(sourceOutput))
                .commands(List.of(
                        "cd chat-infra",
                        "npm install -g aws-cdk",
                        "cdk synth"
                )).primaryOutputDirectory("chat-infra/cdk.out")
                .build();

        // Using higher level construct to simplify deployment, passing build-pipeline to use
        CodePipeline deployPipeline = CodePipeline.Builder
                .create(this, "ChatterPipeline")
                .codePipeline(buildPipeline)
                .synth(synth)
                .build();

        // Adding eks stage to provision and run our image from build-pipeline
        deployPipeline.addStage(new EksStage(this, "EksStage"))
                .addPost(ShellStep.Builder
                        .create("UpdateKubeImage")
                        .commands(List.of("./chat-infra/updateKubeImage.sh"))
                        .build());
    }

    private StageProps createSourceStage(String stageName, Artifact output) {
        GitHubSourceAction action = GitHubSourceAction.Builder
                .create()
                .actionName("Connect_GitHub")
                .owner("Lisztiac")
                .repo("chat-service")
                .branch("main")
                .oauthToken(SecretValue.secretsManager("github-token"))
                .output(output)
                .build();

        return StageProps.builder()
                .stageName(stageName)
                .actions(List.of(action))
                .build();
    }

    private StageProps createImageBuildStage(String stageName, Artifact input, Artifact output, IRepository imgRepo) {
        BuildEnvironment buildEnv = BuildEnvironment.builder()
                .buildImage(LinuxBuildImage.AMAZON_LINUX_2_5)
                .privileged(true)
                .environmentVariables(Map.of(
                        "AWS_REGION", buildEnvVar(Stack.of(this).getRegion()),
                        "AWS_ACCOUNT_ID", buildEnvVar(Stack.of(this).getAccount()),
                        "IMAGE_REPO_URI", buildEnvVar(imgRepo.getRepositoryUri())
                )).build();

        PipelineProject project = PipelineProject.Builder
                .create(this, "ChatterProject")
                .buildSpec(BuildSpec.fromAsset("buildspec.yml"))
                .environment(buildEnv)
                .build();

        imgRepo.grantPullPush(project.getGrantPrincipal());

        CodeBuildAction action = CodeBuildAction.Builder
                .create()
                .actionName("Build_Image")
                .project(project)
                .input(input)
                .outputs(List.of(output))
                .build();

        return StageProps.builder()
                .stageName(stageName)
                .actions(List.of(action))
                .build();
    }

    private BuildEnvironmentVariable buildEnvVar(Object value) {
        return BuildEnvironmentVariable.builder()
                .value(value)
                .build();
    }
}
