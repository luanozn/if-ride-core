import {Construct} from 'constructs';
import {FederatedPrincipal, ManagedPolicy, OpenIdConnectProvider, Role} from "aws-cdk-lib/aws-iam";
import {CfnOutput, Duration, Stack, StackProps} from "aws-cdk-lib";

export class GithubPipelineStack extends Stack {
    constructor(scope: Construct, id: string, props: StackProps) {
        super(scope, id, props);

        const provider = new OpenIdConnectProvider(this, 'GitHubProvider', {
            url: 'https://token.actions.githubusercontent.com',
            clientIds: ['sts.amazonaws.com'],
        });

        const githubRole = new Role(this, 'GitHubDeployRole', {
            assumedBy: new FederatedPrincipal(
                provider.openIdConnectProviderArn,
                {
                    StringEquals: {
                        'token.actions.githubusercontent.com:aud': 'sts.amazonaws.com',
                    },
                    StringLike: {
                        'token.actions.githubusercontent.com:sub': 'repo:luanozn/if-ride-core:ref:refs/heads/main',
                    },
                },
                'sts:AssumeRoleWithWebIdentity'
            ),
            description: 'Role para deploy automatizado via GitHub Actions',
            maxSessionDuration: Duration.hours(1)
        });

        githubRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName('AdministratorAccess'));

        new CfnOutput(this, 'GitHubRoleArn', { value: githubRole.roleArn });
    }
}