package com.pujiyam.chatter.infra.cicd;

import com.pujiyam.chatter.infra.eks.EksStack;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.Stage;
import software.amazon.awscdk.StageProps;
import software.constructs.Construct;

import java.io.IOException;

public class EksStage extends Stage {

    public EksStage(final Construct scope, final String id) throws IOException {
        this(scope, id, null);
    }

    public EksStage(final Construct scope, final String id, final StageProps props) throws IOException {
        super(scope, id, props);

        Stack eksStack = new EksStack(this, "EksStack");
    }
}
