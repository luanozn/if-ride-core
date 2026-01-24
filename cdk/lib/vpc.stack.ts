import { Construct } from 'constructs';
import {ParameterUtils} from "./utils/parameter-utils";
import {SubnetType, Vpc} from "aws-cdk-lib/aws-ec2";
import {Stack, StackProps} from "aws-cdk-lib";

export class VpcStack extends Stack {
    constructor(scope: Construct, id: string, props?: StackProps) {
        super(scope, id, props);

        const vpc = new Vpc(this, 'IfRideVpc', {
            maxAzs: 2,
            natGateways: 0,
            subnetConfiguration: [
                {
                    name: 'Public',
                    subnetType: SubnetType.PUBLIC,
                    cidrMask: 24,
                },
                {
                    name: 'Private',
                    subnetType: SubnetType.PRIVATE_ISOLATED,
                    cidrMask: 24,
                },
            ],
        });

        ParameterUtils.createParameter(this, "VpcIdParam", vpc.vpcId, 'vpc-id', 'ID da VPC do projeto IF Ride');

        const publicSubnetIds = vpc.publicSubnets.map(s => s.subnetId).join(',');
        ParameterUtils.createParameter(this, "PublicSubnetsParam", publicSubnetIds, 'public-subnets')

        const privateSubnetIds = vpc.isolatedSubnets.map(s => s.subnetId).join(',');
        ParameterUtils.createParameter(this, "PrivateSubnetsParam", privateSubnetIds, 'private-subnets')

    }
}