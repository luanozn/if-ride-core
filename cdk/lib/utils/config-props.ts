import {StackProps} from "aws-cdk-lib";
import {CfnEIP, IInstance, ISecurityGroup, IVpc} from "aws-cdk-lib/aws-ec2";
import {IBucket} from "aws-cdk-lib/aws-s3";
import {IRepository} from "aws-cdk-lib/aws-ecr";

export interface ConfigProps extends StackProps {
    ses: {
        email: string;
    },
    resources?: {
        vpc?: IVpc;
        bucket?: IBucket;
        instance?: IInstance;
        securityGroup?: ISecurityGroup;
        eip?: CfnEIP;
    }
    parameterNames: {
        databaseUsername: string;
        secret: string;
    }
}