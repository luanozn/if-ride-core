import * as ec2 from 'aws-cdk-lib/aws-ec2';
import {Stack} from 'aws-cdk-lib';
import {Construct} from 'constructs';
import {
    IInstance,
    Instance,
    InstanceType, ISecurityGroup,
    KeyPair,
    MachineImage,
    SecurityGroup,
    SubnetType,
    Vpc
} from "aws-cdk-lib/aws-ec2";
import {ParameterUtils} from "./utils/parameter-utils";
import {ConfigProps} from "./utils/config-props";
import {Bucket} from "aws-cdk-lib/aws-s3";
import {Effect, PolicyStatement, Role, ServicePrincipal} from "aws-cdk-lib/aws-iam";
import {SSM_PREFIX} from "./utils/constants";
import {EmailIdentity, Identity} from "aws-cdk-lib/aws-ses";

export class ServerStack extends Stack {
    instance: IInstance;
    securityGroup: ISecurityGroup;

    constructor(scope: Construct, id: string, props: ConfigProps) {
        super(scope, id, props);

        const vpc = props.resources!.vpc!;
        const bucket = props.resources!.bucket!;


        const securityGroup = new SecurityGroup(this, 'InstanceSG', {
            vpc,
            description: 'Permitir acesso HTTP e SSH',
            allowAllOutbound: true,
        });

        securityGroup.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(8080), 'API Spring Boot');
        securityGroup.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(22), 'Acesso SSH');

        const key = KeyPair.fromKeyPairName(this, 'IfRideKeyPair', 'if-ride-key');

        const ec2Role = new Role(this, 'IFRideEc2Role', {
            assumedBy: new ServicePrincipal('ec2.amazonaws.com'),
            description: 'Role para a instância EC2 acessar SSM Parameter Store e SES',
        });

        ec2Role.addToPolicy(new PolicyStatement({
            effect: Effect.ALLOW,
            actions: ['ssm:GetParametersByPath', 'ssm:GetParameter'],
            resources: [`arn:aws:ssm:${this.region}:${this.account}:parameter${SSM_PREFIX}/*`],
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

        const instance = new Instance(this, 'IfRideServer', {
            vpc,
            instanceType: InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.MICRO),
            machineImage: MachineImage.latestAmazonLinux2023(),
            securityGroup: securityGroup,
            role: ec2Role,
            vpcSubnets: {subnetType: SubnetType.PUBLIC},
            keyPair: key
        });

        bucket.grantReadWrite(instance)
        props.resources?.ecrRepo!.grantPull(instance)

        instance.addUserData(
            'sudo dnf update -y',
            'sudo dnf install -y docker',
            'sudo systemctl start docker',
            'sudo systemctl enable docker',
            'sudo usermod -aG docker ec2-user'
        );

        this.instance = instance;
        this.securityGroup = securityGroup;
    }
}