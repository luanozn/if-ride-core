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
    vpc: {
        name: "if-ride-vpc"
    },
    parameterNames: {
        ec2Url: "ec2.url",
        assetsBucketName: "assets.bucket.name",
        databaseUsername: "database.username",
        appSecurityGroupId: "network.app.security-group.id"
    }
}

new GithubPipelineStack(app, "IfRideFoundation", { env })

const apiGateway = new ApiGatewayStack(app, "IfRideGateway",configProps);
const vpc = new VpcStack(app, "IfRideNetwork", configProps);
const assets = new AssetsStack(app, "IfRideStaticAssets", configProps);
const database = new DatabaseStack(app, "IfRidePersistence", configProps);
const server = new ServerStack(app, "IfRideServer", configProps);

server.addDependency(vpc)
server.addDependency(assets)

database.addDependency(vpc)
database.addDependency(server)

apiGateway.addDependency(server)



