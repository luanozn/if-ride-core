import { Construct } from 'constructs';
import {
    IVpc,
    SubnetType,
    Vpc,
    InstanceType,
    InstanceClass,
    NatProvider,
    GatewayVpcEndpointAwsService, InstanceSize
} from "aws-cdk-lib/aws-ec2";
import { Stack } from "aws-cdk-lib";
import { ConfigProps } from "./utils/config-props";

export class VpcStack extends Stack {
    vpc: IVpc;

    constructor(scope: Construct, id: string, props: ConfigProps) {
        super(scope, id, props);

        const natProvider = NatProvider.instanceV2({
            instanceType: InstanceType.of(InstanceClass.T4G, InstanceSize.NANO),
            defaultAllowedTraffic: undefined,
        });

        this.vpc = new Vpc(this, 'IfRideVpc', {
            maxAzs: 2,
            natGatewayProvider: natProvider,
            natGateways: 1,
            subnetConfiguration: [
                {
                    name: 'Public',
                    subnetType: SubnetType.PUBLIC,
                    cidrMask: 24,
                },
                {
                    name: 'PrivateWithEgress',
                    subnetType: SubnetType.PRIVATE_WITH_EGRESS,
                    cidrMask: 24,
                },
                {
                    name: 'Private',
                    subnetType: SubnetType.PRIVATE_ISOLATED,
                    cidrMask: 24,
                },
            ],
        });

        this.vpc.addGatewayEndpoint('S3Endpoint', {
            service: GatewayVpcEndpointAwsService.S3,
        });
    }
}