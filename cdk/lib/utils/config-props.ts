import {StackProps} from "aws-cdk-lib";

export interface ConfigProps extends StackProps {
    vpc: {
        name: string;
    }
    parameterNames: {
        assetsBucketName: string;
        databaseUsername: string;
        appSecurityGroupId: string
    }
}