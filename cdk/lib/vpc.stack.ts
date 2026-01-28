import { Construct } from 'constructs';
import {ParameterUtils} from "./utils/parameter-utils";
import {IVpc, SubnetType, Vpc} from "aws-cdk-lib/aws-ec2";
import {Stack, StackProps} from "aws-cdk-lib";
import {ConfigProps} from "./utils/config-props";

export class VpcStack extends Stack {
    vpc: IVpc;

    constructor(scope: Construct, id: string, props: ConfigProps) {
        super(scope, id, props);

        this.vpc = new Vpc(this, 'IfRideVpc', {
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

    }
}