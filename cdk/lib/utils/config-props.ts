import {StackProps} from "aws-cdk-lib";
import {IInstance, ISecurityGroup, IVpc} from "aws-cdk-lib/aws-ec2";
import {IBucket} from "aws-cdk-lib/aws-s3";

export interface ConfigProps extends StackProps {
    ses: {
        email: string;
    },
    resources?: {
        vpc?: IVpc;
        bucket?: IBucket;
        instance?: IInstance;
        securityGroup?: ISecurityGroup;
    }
    parameterNames: {
        databaseUsername: string;
    }
}