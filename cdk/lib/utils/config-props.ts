import {StackProps} from "aws-cdk-lib";

export interface ConfigProps extends StackProps {
    vpc: {
        name: string;
    }
    parameterNames: {
        ec2Url: string;
        assetsBucketName: string;
        databaseUsername: string;
        appSecurityGroupId: string
    }
}