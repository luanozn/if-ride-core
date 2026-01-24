import { Construct } from 'constructs';
import { ConfigProps } from './utils/config-props';
import * as path from "node:path";
import { ParameterUtils } from "./utils/parameter-utils";
import {Duration, Stack} from "aws-cdk-lib";
import {Code, Runtime, Function} from "aws-cdk-lib/aws-lambda";
import {Cors, HttpIntegration, RestApi, TokenAuthorizer} from "aws-cdk-lib/aws-apigateway";

export class ApiGatewayStack extends Stack {
    constructor(scope: Construct, id: string, props: ConfigProps) {
        super(scope, id, props);

        const authorizerLambda = new Function(this, 'IfRideAuthorizer', {
            runtime: Runtime.NODEJS_20_X,
            handler: 'index.handler',
            code: Code.fromAsset(path.join(__dirname, '../dist/authorizer.zip')),
        });

        const authorizer = new TokenAuthorizer(this, 'JwtAuthorizer', {
            handler: authorizerLambda,
            resultsCacheTtl: Duration.minutes(0),
        });

        const api = new RestApi(this, 'IfRideApi', {
            restApiName: 'IF Ride Service',
            description: 'API Gateway atuando como escudo para a t3.micro',
            defaultCorsPreflightOptions: {
                allowOrigins: Cors.ALL_ORIGINS,
                allowMethods: Cors.ALL_METHODS,
            },
        });

        const ec2Endpoint = ParameterUtils.retrieveParameter(this, 'Ec2Endpoint', props.parameterNames.ec2Url);
        const httpIntegration = new HttpIntegration(ec2Endpoint + '/{proxy}', {
            httpMethod: 'ANY',
            options: {
                cacheKeyParameters: ['method.request.path.proxy'],
                requestParameters: {
                    'integration.request.path.proxy': 'method.request.path.proxy',
                },
            },
        });

        api.root.addProxy({
            defaultIntegration: httpIntegration,
            anyMethod: true,
            defaultMethodOptions: {
                authorizer: authorizer,
                requestParameters: {
                    'method.request.path.proxy': true,
                },
            },
        });
    }
}