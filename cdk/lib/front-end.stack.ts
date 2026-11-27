import { Stack, CfnOutput, Duration } from "aws-cdk-lib";
import { Construct } from "constructs";
import * as cloudfront from 'aws-cdk-lib/aws-cloudfront';
import * as origins from 'aws-cdk-lib/aws-cloudfront-origins';
import { ConfigProps } from "./utils/config-props";
import { ParameterUtils } from "./utils/parameter-utils";

export class FrontendStack extends Stack {
    constructor(scope: Construct, id: string, props: ConfigProps) {
        super(scope, id, props);

        const instance = props.resources!.instance!;

        const distribution = new cloudfront.Distribution(this, 'IfRideDistribution', {
            defaultBehavior: {
                origin: origins.VpcOrigin.withEc2Instance(instance, {
                    httpPort: 8080,
                    protocolPolicy: cloudfront.OriginProtocolPolicy.HTTP_ONLY,
                }),
                allowedMethods: cloudfront.AllowedMethods.ALLOW_ALL,
                cachePolicy: cloudfront.CachePolicy.CACHING_DISABLED,
                originRequestPolicy: cloudfront.OriginRequestPolicy.ALL_VIEWER,
                viewerProtocolPolicy: cloudfront.ViewerProtocolPolicy.HTTPS_ONLY,
            },
        });

        const cloudFrontUrl = `https://${distribution.distributionDomainName}`;

        ParameterUtils.createParameter(
            this,
            'FrontendUrlParam',
            cloudFrontUrl,
            'frontend.url',
            'URL pública do CloudFront'
        );

        new CfnOutput(this, 'CloudFrontURL', {
            value: cloudFrontUrl,
            exportName: 'IfRideAppUrl',
            description: 'URL pública HTTPS para o App Flutter e Frontend'
        });
    }
}