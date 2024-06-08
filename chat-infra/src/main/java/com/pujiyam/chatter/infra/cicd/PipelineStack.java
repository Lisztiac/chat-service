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

        Artifact sourceOutput = new Artifact();
        Artifact buildOutput = new Artifact();

        Pipeline buildPipeline = Pipeline.Builder
                .create(this, "Pipeline")
                .restartExecutionOnUpdate(true)
                .stages(List.of(
                        createSourceStage("Source", sourceOutput),
                        createImageBuildStage("ImageBuild", sourceOutput, buildOutput, imgRepo)
                ))
                .build();

        ShellStep synth = CodeBuildStep.Builder
                .create("Synth")
                .input(CodePipelineFileSet.fromArtifact(sourceOutput))
                .buildEnvironment(buildEnv)
                .commands(List.of(
                        "cd chat-infra",
                        "npm install -g aws-cdk",
                        "cdk synth"
                ))
                .primaryOutputDirectory("chat-infra/cdk.out")
                .build();

        CodePipeline codePipeline = CodePipeline.Builder
                .create(this, "ChatterPipeline")
                .codePipeline(buildPipeline)
                .synth(synth)
                .build();

//        pipeline.addStage(new EksStage(this, "eks-stage"));
    }

    private StageProps createSourceStage(String stageName, Artifact output) {
        GitHubSourceAction action = GitHubSourceAction.Builder
                .create()
                .actionName("GitHubAction")
                .owner("Lisztian")
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

        CodeBuildAction action = CodeBuildAction.Builder
                .create()
                .actionName("ImageBuildAction")
                .project(project)
                .input(input)
                .outputs(List.of(output))
                .build();

        return StageProps.builder()
                .stageName(stageName)
                .actions(List.of(action))
                .build();
    }

    private BuildEnvironmentVariable buildVar(Object value) {
        return BuildEnvironmentVariable.builder()
                .value(value)
                .build();
    }
}
