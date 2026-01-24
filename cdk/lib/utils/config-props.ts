import {StackProps} from "aws-cdk-lib";

export interface ConfigProps extends StackProps {
    parameterNames: {
        vpcId: string;
        assetsBucketName: string;
        databaseUsername: string;
        appSecurityGroupId: string
    }
}