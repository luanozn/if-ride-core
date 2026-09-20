import * as ec2 from 'aws-cdk-lib/aws-ec2';
import {CfnOutput, RemovalPolicy, Stack} from 'aws-cdk-lib';
import {Construct} from 'constructs';
import {
    CfnEIP,
    IInstance,
    Instance,
    InstanceType, ISecurityGroup,
    KeyPair,
    MachineImage,
    SecurityGroup,
    SubnetType,
} from "aws-cdk-lib/aws-ec2";
import {ConfigProps} from "./utils/config-props";
import {Effect, ManagedPolicy, PolicyStatement, Role, ServicePrincipal} from "aws-cdk-lib/aws-iam";
import {SSM_PREFIX} from "./utils/constants";
import {EmailIdentity, Identity} from "aws-cdk-lib/aws-ses";
import {Repository} from "aws-cdk-lib/aws-ecr";

export class ServerStack extends Stack {
    instance: IInstance;
    securityGroup: ISecurityGroup;
    eIP: CfnEIP;

    constructor(scope: Construct, id: string, props: ConfigProps) {
        super(scope, id, props);

        const vpc = props.resources!.vpc!;

        const securityGroup = new SecurityGroup(this, 'InstanceSG', {
            vpc,
            description: 'Permitir acesso HTTP',
            allowAllOutbound: true,
        });

        securityGroup.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(8080), 'API Spring Boot');

        const ec2Role = new Role(this, 'IFRideEc2Role', {
            assumedBy: new ServicePrincipal('ec2.amazonaws.com'),
            description: 'Role para a instância EC2 acessar SSM Parameter Store e SES',
        });

        ec2Role.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName('AmazonSSMManagedInstanceCore'))

        ec2Role.addToPolicy(new PolicyStatement({
            effect: Effect.ALLOW,
            actions: ['ssm:GetParametersByPath', 'ssm:GetParameter'],
            resources: [`arn:aws:ssm:${this.region}:${this.account}:parameter${SSM_PREFIX}/*`],
        }));

        ec2Role.addToPolicy(new PolicyStatement({
            effect: Effect.ALLOW,
            actions: ['kms:Decrypt'],
            resources: [`arn:aws:kms:${this.region}:${this.account}:key/*`],
            conditions: {
                StringEquals: {
                    'kms:ViaService': [
                        `ssm.${this.region}.amazonaws.com`,
                        `secretsmanager.${this.region}.amazonaws.com`,
                    ],
                },
            },
        }));

        ec2Role.addToPolicy(new PolicyStatement({
            effect: Effect.ALLOW,
            actions: ['secretsmanager:GetSecretValue'],
            resources: [
                `arn:aws:secretsmanager:${this.region}:${this.account}:secret:${SSM_PREFIX}/*`,
            ],
        }));

        const emailIdentity = new EmailIdentity(this, 'IFGoianoIdentity', {
            identity: Identity.email(props.ses.email),
        });

        ec2Role.addToPolicy(new PolicyStatement({
            effect: Effect.ALLOW,
            actions: ['ses:SendEmail', 'ses:SendRawEmail'],
            resources: [
                `arn:aws:ses:${this.region}:${this.account}:identity/${emailIdentity.emailIdentityName}`
            ],
            conditions: {
                'StringEquals': {
                    'ses:FromAddress': props.ses.email
                }
            }
        }));

        const ecrRepo = new Repository(this, 'IfRideCoreRepo', {
            repositoryName: 'if-ride-core',
            removalPolicy: RemovalPolicy.DESTROY,
            emptyOnDelete: true,
            lifecycleRules: [
                {
                    maxImageCount: 1,
                    description: 'Limpeza de imagens antigas para economia de custo',
                },
            ],
        });

        const instance = new Instance(this, 'IfRideServer', {
            vpc,
            instanceType: InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.MICRO),
            machineImage: MachineImage.latestAmazonLinux2023(),
            securityGroup: securityGroup,
            role: ec2Role,
            vpcSubnets: {subnetType: SubnetType.PUBLIC},
        });

        const elasticIp = new CfnEIP(this, 'ElasticIp', { instanceId: instance.instanceId, domain: "vpc" })

        ecrRepo!.grantPull(instance)

        instance.addUserData(
            'sudo dnf update -y',
            'sudo dnf install -y docker',
            'sudo systemctl start docker',
            'sudo systemctl enable docker',
            'sudo usermod -aG docker ec2-user'
        );

        this.instance = instance;
        this.securityGroup = securityGroup;
        this.eIP = elasticIp;

        new CfnOutput(this, 'InstanceId', {
            value: instance.instanceId,
            exportName: 'IfRideInstanceId',
            description: 'Instance ID para uso do pipeline via SSM',
        });

        new CfnOutput(this, 'ElasticIpPublicIp', {
            value: elasticIp.attrPublicIp,
            exportName: 'IfRideElasticIp',
        });
    }
}