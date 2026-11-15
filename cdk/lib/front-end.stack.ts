import { Stack, CfnOutput, Duration } from "aws-cdk-lib";
import { Construct } from "constructs";
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as targets from 'aws-cdk-lib/aws-elasticloadbalancingv2-targets';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as cloudfront from 'aws-cdk-lib/aws-cloudfront';
import * as origins from 'aws-cdk-lib/aws-cloudfront-origins';
import { ConfigProps } from "./utils/config-props";
import {ParameterUtils} from "./utils/parameter-utils";

export class FrontendStack extends Stack {
    constructor(scope: Construct, id: string, props: ConfigProps) {
        super(scope, id, props);

        const vpc = props.resources!.vpc!;
        const instance = props.resources!.instance!;
        const instanceSg = props.resources!.securityGroup!;

        const albSg = new ec2.SecurityGroup(this, 'AlbSG', {
            vpc,
            description: 'Permitir acesso HTTP/HTTPS do CloudFront ou público',
            allowAllOutbound: true,
        });
        albSg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(80), 'HTTP Público');

        instanceSg.addIngressRule(albSg, ec2.Port.tcp(8080), 'Acesso do ALB na porta 8080');

        const alb = new elbv2.ApplicationLoadBalancer(this, 'IfRideAlb', {
            vpc,
            internetFacing: true,
            securityGroup: albSg,
            idleTimeout: Duration.seconds(4000),
        });

        const targetGroup = new elbv2.ApplicationTargetGroup(this, 'IfRideTg', {
            vpc,
            port: 8080,
            protocol: elbv2.ApplicationProtocol.HTTP,
            targets: [new targets.InstanceIdTarget(instance.instanceId, 8080)],
            healthCheck: {
                path: '/swagger-ui/index.html',
                interval: Duration.seconds(30),
                healthyThresholdCount: 2,
                unhealthyThresholdCount: 3,
            },
            deregistrationDelay: Duration.seconds(10),
        });

        // 5. Listener do ALB na porta 80
        alb.addListener('HttpListener', {
            port: 80,
            defaultTargetGroups: [targetGroup],
        });

        // 6. Criar o CloudFront (HTTPS gratuito + CDN)
        const distribution = new cloudfront.Distribution(this, 'IfRideDistribution', {
            defaultBehavior: {
                origin: new origins.LoadBalancerV2Origin(alb, {
                    protocolPolicy: cloudfront.OriginProtocolPolicy.HTTP_ONLY,
                }),
                allowedMethods: cloudfront.AllowedMethods.ALLOW_ALL,
                cachePolicy: cloudfront.CachePolicy.CACHING_DISABLED, // Não fazer cache de API/Chat
                originRequestPolicy: cloudfront.OriginRequestPolicy.ALL_VIEWER, // Repassa headers, cookies, query strings
            },
        });

        const cloudFrontUrl = `https://${distribution.distributionDomainName}`

        ParameterUtils.createParameter(
            this,
            'FrontendUrlParam',
            cloudFrontUrl,
            'frontend.url',
            'URL pública do CloudFront usada para CORS e links de email'
        );

        new CfnOutput(this, 'CloudFrontURL', {
            value: cloudFrontUrl,
            exportName: 'IfRideAppUrl',
            description: 'URL pública HTTPS para o App Flutter e Frontend'
        });
    }
}