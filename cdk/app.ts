import {App} from "aws-cdk-lib/core";
import {VpcStack} from "./lib/vpc.stack";
import {ConfigProps} from "./lib/utils/config-props";
import {AssetsStack} from "./lib/assets.stack";
import {ServerStack} from "./lib/server.stack";
import {DatabaseStack} from "./lib/database.stack";
import {GithubPipelineStack} from "./lib/github-pipeline.stack";
import {ApiGatewayStack} from "./lib/api-gateway.stack";

const app = new App();

const env = {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: process.env.CDK_DEFAULT_REGION
};

const configProps: ConfigProps = {
    env,
    ses: {
        email: "luan.ribeiro@estudante.ifgoiano.edu.br"
    },
    parameterNames: {
        databaseUsername: "database.username",
    }
}

new GithubPipelineStack(app, "IfRideFoundation", { env })

const vpcStack = new VpcStack(app, "IfRideNetwork", configProps);

const assets = new AssetsStack(app, "IfRideStaticAssets", configProps);

const server = new ServerStack(app, "IfRideServer", {
    ...configProps,
    resources: {
        vpc: vpcStack.vpc,
        bucket: assets.assetsBucket,
        ecrRepo: assets.ecrRepo,
    }
});

const database = new DatabaseStack(app, "IfRidePersistence", {
    ...configProps,
    resources: {
        vpc: vpcStack.vpc,
        securityGroup: server.securityGroup,

    }
});

const gateway = new ApiGatewayStack(app, "IfRideGateway", {
    ...configProps,
    resources: {
        instance: server.instance
    }
});


database.addDependency(server)

gateway.addDependency(server)
