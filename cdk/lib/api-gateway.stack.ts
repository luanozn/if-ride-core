import { Construct } from 'constructs';
import { ConfigProps } from './utils/config-props';
import * as path from "node:path";
import { ParameterUtils } from "./utils/parameter-utils";
import {Duration, Stack} from "aws-cdk-lib";
import {Code, Runtime, Function} from "aws-cdk-lib/aws-lambda";
import {AuthorizationType, Cors, HttpIntegration, RestApi, TokenAuthorizer} from "aws-cdk-lib/aws-apigateway";
import {Effect, PolicyStatement} from "aws-cdk-lib/aws-iam";
import {LogGroup, RetentionDays} from "aws-cdk-lib/aws-logs";

export class ApiGatewayStack extends Stack {
    constructor(scope: Construct, id: string, props: ConfigProps) {
        super(scope, id, props);

        const apiSecret = ParameterUtils.retrieveSecureParameter(this, "APISecret", props.parameterNames.secret, 1)

        const authorizerLogGroup = new LogGroup(this, 'AuthorizerLogs', {
            retention: RetentionDays.ONE_WEEK,
        });

        const authorizerLambda = new Function(this, 'IfRideAuthorizer', {
            runtime: Runtime.NODEJS_20_X,
            handler: 'index.handler',
            environment: {
                "IF_RIDE_SECRET_PARAM": apiSecret.parameterName
            },
            logGroup: authorizerLogGroup,
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

        const ec2Endpoint = `http://${props.resources?.eip?.attrPublicIp}:8080`
        const authIntegration = new HttpIntegration(`${ec2Endpoint}/v1/auth/{proxy}`, {
            httpMethod: 'ANY',
            options: {
                cacheKeyParameters: ['method.request.path.proxy'],
                requestParameters: {
                    'integration.request.path.proxy': 'method.request.path.proxy',
                },
            },
        });

        const staticFilesIntegration = new HttpIntegration(`${ec2Endpoint}/auth/{proxy}`, {
            httpMethod: 'ANY',
            options: {
                cacheKeyParameters: ['method.request.path.proxy'],
                requestParameters: {
                    'integration.request.path.proxy': 'method.request.path.proxy',
                },
            },
        });

        const swaggerIntegration = new HttpIntegration(`${ec2Endpoint}/swagger-ui/{proxy}`, {
            httpMethod: 'ANY',
            options: {
                cacheKeyParameters: ['method.request.path.proxy'],
                requestParameters: {
                    'integration.request.path.proxy': 'method.request.path.proxy',
                },
            },
        });

        const globalIntegration = new HttpIntegration(`${ec2Endpoint}/{proxy+}`, {
            httpMethod: 'ANY',
            options: {
                cacheKeyParameters: ['method.request.path.proxy'],
                requestParameters: {
                    'integration.request.path.proxy': 'method.request.path.proxy',
                },
            },
        });

        const v1 = api.root.addResource('v1');
        const v1Auth = v1.addResource('auth');
        v1Auth.addProxy({
            defaultIntegration: authIntegration,
            anyMethod: true,
            defaultMethodOptions: {
                requestParameters: {
                    'method.request.path.proxy': true,
                },
            },
        });


        const staticAuth = api.root.addResource('auth');
        staticAuth.addProxy({
            defaultIntegration: staticFilesIntegration,
            anyMethod: true,
            defaultMethodOptions: {
                requestParameters: {
                    'method.request.path.proxy': true,
                },
            },
        });

        const swaggerAuth = api.root.addResource('swagger-ui')
        swaggerAuth.addProxy({
            defaultIntegration: swaggerIntegration,
            anyMethod: true,
            defaultMethodOptions: {
                requestParameters: {
                    'method.request.path.proxy': true,
                },
            },
        })

        api.root.addProxy({
            defaultIntegration: globalIntegration,
            anyMethod: true,
            defaultMethodOptions: {
                authorizer: authorizer,
                authorizationType: AuthorizationType.CUSTOM,
                requestParameters: {
                    'method.request.path.proxy': true,
                }
            }
        });

        apiSecret.grantRead(authorizerLambda);
        authorizerLambda.role?.addToPrincipalPolicy(new PolicyStatement({
            effect: Effect.ALLOW,
            actions: ['kms:Decrypt'],
            resources: ['*'],
            conditions: {
                StringEquals: {
                    'kms:ViaService': `ssm.${this.region}.amazonaws.com`,
                },
            },
        }));
    }
}