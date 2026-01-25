import { Construct } from 'constructs';
import {CfnOutput, RemovalPolicy, Stack} from "aws-cdk-lib";
import {ConfigProps} from "./utils/config-props";
import {ParameterUtils} from "./utils/parameter-utils";
import {S3BucketOrigin} from "aws-cdk-lib/aws-cloudfront-origins";
import {
    CachePolicy, Distribution,
    S3OriginAccessControl, Signing,
    ViewerProtocolPolicy
} from "aws-cdk-lib/aws-cloudfront";
import {PolicyStatement, ServicePrincipal} from "aws-cdk-lib/aws-iam";
import {BlockPublicAccess, Bucket, BucketEncryption} from "aws-cdk-lib/aws-s3";

export class AssetsStack extends Stack {
    constructor(scope: Construct, id: string, props: ConfigProps) {
        super(scope, id, props);

        Stack.of(this)

        const bucket = new Bucket(this, 'IfRideAssetsBucket', {
            bucketName: `if-ride-assets-${props?.env?.region}-${props?.env?.account}`,
            versioned: true,
            encryption: BucketEncryption.S3_MANAGED,
            blockPublicAccess: BlockPublicAccess.BLOCK_ALL,
            removalPolicy: RemovalPolicy.DESTROY,
            autoDeleteObjects: true,
        });

        ParameterUtils.createParameter(this, "BucketParameterName", bucket.bucketName, props.parameterNames.assetsBucketName, "Nome do bucket S3 para armazenamento de ficheiros do IF Ride")

        const distribution = new Distribution(this, 'IfRideDistribution', {
            defaultBehavior: {
                origin: S3BucketOrigin.withOriginAccessControl(bucket, {
                    originAccessControl: new S3OriginAccessControl(this, 'OAC', {
                        signing: Signing.SIGV4_ALWAYS
                    }),
                }),
                viewerProtocolPolicy: ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
                cachePolicy: CachePolicy.CACHING_OPTIMIZED,
            },
            comment: 'CDN para os ativos do IF Ride',
        });

        const cloudFrontAllowedPolicy = new PolicyStatement({
            actions: ['s3:GetObject'],
            resources: [bucket.arnForObjects('public/*')],
            principals: [new ServicePrincipal('cloudfront.amazonaws.com')],
            conditions: {
                StringEquals: {
                    'AWS:SourceArn': `arn:aws:partition:cloudfront::${this.account}:distribution/${distribution.distributionId}`,
                },
            },
        });


        bucket.addToResourcePolicy(cloudFrontAllowedPolicy);

        new CfnOutput(this, 'CloudFrontURL', {
            value: distribution.distributionDomainName,
            description: 'URL para acessar os arquivos via CloudFront',
        });

        new CfnOutput(this, 'S3BucketName', {
            value: bucket.bucketName,
            exportName: 'IfRideBucketName',
        });
    }
}