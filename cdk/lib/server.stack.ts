import * as ec2 from 'aws-cdk-lib/aws-ec2';
import { RemovalPolicy, Stack, StackProps} from 'aws-cdk-lib';
import {Construct} from 'constructs';
import {Network} from "node:inspector";
import {Instance, InstanceType, KeyPair, MachineImage, SecurityGroup, SubnetType, Vpc} from "aws-cdk-lib/aws-ec2";
import {ParameterUtils} from "./utils/parameter-utils";
import {ConfigProps} from "./utils/config-props";
import {Bucket} from "aws-cdk-lib/aws-s3";

export class ServerStack extends Stack {
  constructor(scope: Construct, id: string, props: ConfigProps) {
    super(scope, id, props);

    const vpc = Vpc.fromLookup(this, "CoreVPC", {
      vpcName: props.vpc.name,
    })

    const bucketName = ParameterUtils.retrieveParameter(this, "BucketName", props.parameterNames.assetsBucketName).stringValue
    const bucket = Bucket.fromBucketName(this, "Bucket", bucketName)


    const securityGroup = new SecurityGroup(this, 'InstanceSG', {
      vpc,
      description: 'Permitir acesso HTTP e SSH',
      allowAllOutbound: true,
    });

    securityGroup.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(8080), 'API Spring Boot');
    securityGroup.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(22), 'Acesso SSH');

    const key = KeyPair.fromKeyPairName(this, 'IfRideKeyPair', 'if-ride-key');

    const instance = new Instance(this, 'IfRideServer', {
      vpc,
      instanceType: InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.MICRO),
      machineImage: MachineImage.latestAmazonLinux2023(),
      securityGroup: securityGroup,
      vpcSubnets: { subnetType: SubnetType.PUBLIC },
      keyPair: key
    });

    bucket.grantReadWrite(instance)

    instance.addUserData(
        'sudo dnf update -y',
        'sudo dnf install -y docker',
        'sudo systemctl start docker',
        'sudo systemctl enable docker',
        'sudo usermod -aG docker ec2-user'
    );

    ParameterUtils.createParameter(this, "SecurityGroupId", securityGroup.securityGroupId, props.parameterNames.appSecurityGroupId);
  }
}