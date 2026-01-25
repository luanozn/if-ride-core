import {StringParameter} from "aws-cdk-lib/aws-ssm";
import {Construct} from "constructs";
import {SSM_PREFIX} from "./constants";

export class ParameterUtils {

    public static createParameter(context: Construct, id: string, parameterValue: string, path: string, description?: string): StringParameter {
        return new StringParameter(context, id, {
            parameterName: `${SSM_PREFIX}/${path}/`,
            stringValue: parameterValue,
            description: description,
        });
    }

    public static retrieveParameter(context: Construct, id: string, parameterName: string) {
        return StringParameter.fromStringParameterName(context, id, `${SSM_PREFIX}/${parameterName}`);
    }

    public static retrieveSecureParameter(context: Construct, id: string, parameterName: string, version?: number) {
        return StringParameter.fromSecureStringParameterAttributes(context, id, {
            parameterName: `${SSM_PREFIX}/${parameterName}`,
            version: version ?? 1
        });
    }
}