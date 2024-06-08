package com.pujiyam.chatter.infra.cicd;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.pipelines.CodePipeline;
import software.amazon.awscdk.pipelines.CodePipelineSource;
import software.amazon.awscdk.pipelines.ShellStep;
import software.constructs.Construct;

import java.util.List;

public class PipelineStack extends Stack {

    public PipelineStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public PipelineStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        ShellStep synth = ShellStep.Builder
                .create("Synth")
                .input(CodePipelineSource.gitHub("Lisztiac/chat-service", "main"))
                .commands(List.of(
                        "cd chat-infra",
                        "npm install -g aws-cdk",
                        "cdk synth"))
                .primaryOutputDirectory("chat-infra/cdk.out")
                .build();

        CodePipeline pipeline = CodePipeline.Builder
                .create(this, "ChatterPipeline")
                .pipelineName("chatter-pipeline")
                .synth(synth)
                .build();

//        pipeline.addStage(new EksStage(this, "eks-stage"));
    }
}
