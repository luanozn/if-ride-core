import {App} from "aws-cdk-lib/core";
import {VpcStack} from "./lib/vpc.stack";
import {ConfigProps} from "./lib/utils/config-props";
import {AssetsStack} from "./lib/assets.stack";
import {ServerStack} from "./lib/server.stack";
import {DatabaseStack} from "./lib/database.stack";
import {GithubPipelineStack} from "./lib/github-pipeline.stack";

const app = new App();

const env = {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: process.env.CDK_DEFAULT_REGION
};

const configProps: ConfigProps = {
    env,
    vpc: {
        name: "if-ride-vpc"
    },
    parameterNames: {
        assetsBucketName: "assets.bucket.name",
        databaseUsername: "database.username",
        appSecurityGroupId: "network.app.security-group.id"
    }
}

new GithubPipelineStack(app, "GithubPipelineOIDC", { env })

const vpc = new VpcStack(app, "IfRideNetwork", configProps);
const assets = new AssetsStack(app, "IfRideStaticAssets", configProps);
const database = new DatabaseStack(app, "IfRidePersistence", configProps);
const server = new ServerStack(app, "IfRideServer", configProps);

server.addDependency(vpc)
server.addDependency(assets)

database.addDependency(vpc)
database.addDependency(server)



