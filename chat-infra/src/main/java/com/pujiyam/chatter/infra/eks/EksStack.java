package com.pujiyam.chatter.infra.eks;

import com.pujiyam.chatter.infra.util.YamlUtil;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.eks.*;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class EksStack extends Stack {

    public EksStack(final Construct scope, final String id) throws IOException {
        this(scope, id, null);
    }

    public EksStack(final Construct scope, final String id, final StackProps props) throws IOException {
        super(scope, id, props);

        Vpc vpc = Vpc.Builder
                .create(this, "ChatterVPC")
                .maxAzs(2)
                .build();

        AlbControllerOptions alb = AlbControllerOptions.builder()
                .version(AlbControllerVersion.V2_6_2)
                .build();

        Cluster cluster = Cluster.Builder
                .create(this, "ChatterCluster")
                .version(KubernetesVersion.V1_29)
                .vpc(vpc)
                .albController(alb)
                // T2.micro is free tier, but has max pods limit of 4.
                // With ALB, we need at least 3 instances if using t2.micro to deploy our app pods.
                // Note: not only our pods run on these nodes.
                .defaultCapacity(3)
                .defaultCapacityInstance(InstanceType.of(InstanceClass.T2, InstanceSize.MICRO))
                .build();

        KubernetesManifest manifest = cluster.addManifest("ChatterManifests",
                getManifest("namespace"),
                getManifest("deployment"),
                getManifest("service"),
                getManifest("ingress"));

        // Adding explicit dependency as deleting controller before manifests may leave dangling alb resources.
        Optional.ofNullable(cluster.getAlbController())
                .ifPresent(albc -> manifest.getNode().addDependency(albc));
    }

    private Map<String, Object> getManifest(String manifestName) throws IOException {
        String pathname = "kube/%s.yml".formatted(manifestName);

        return YamlUtil.load(pathname);
    }
}
