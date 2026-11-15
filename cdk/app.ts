import {App} from "aws-cdk-lib/core";
import {VpcStack} from "./lib/vpc.stack";
import {ConfigProps} from "./lib/utils/config-props";
import {ServerStack} from "./lib/server.stack";
import {DatabaseStack} from "./lib/database.stack";
import {GithubPipelineStack} from "./lib/github-pipeline.stack";
import {FrontendStack} from "./lib/front-end.stack";

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
        secret: "api.security.token.secret"
    }
}

new GithubPipelineStack(app, "IfRideFoundation", { env })

const vpcStack = new VpcStack(app, "IfRideNetwork", configProps);

const server = new ServerStack(app, "IfRideServer", {
    ...configProps,
    resources: {
        vpc: vpcStack.vpc,
    }
});

const database = new DatabaseStack(app, "IfRidePersistence", {
    ...configProps,
    resources: {
        vpc: vpcStack.vpc,
        instance: server.instance,
        securityGroup: server.securityGroup,
    }
});

const frontend = new FrontendStack(app, 'FrontendStack', {
    ...configProps,
    resources: {
        vpc: vpcStack.vpc,
        instance: server.instance,
        securityGroup: server.securityGroup
    }
});


database.addDependency(server)
frontend.addDependency(server)
