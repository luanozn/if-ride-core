import { Construct } from 'constructs';
import {CfnOutput, RemovalPolicy, Stack} from "aws-cdk-lib";
import {ConfigProps} from "./utils/config-props";
import {S3BucketOrigin} from "aws-cdk-lib/aws-cloudfront-origins";
import {
    CachePolicy, Distribution,
    S3OriginAccessControl, Signing,
    ViewerProtocolPolicy
} from "aws-cdk-lib/aws-cloudfront";
import {PolicyStatement, ServicePrincipal} from "aws-cdk-lib/aws-iam";
import {BlockPublicAccess, Bucket, BucketEncryption, IBucket} from "aws-cdk-lib/aws-s3";
import {IRepository, Repository} from "aws-cdk-lib/aws-ecr";

export class AssetsStack extends Stack {
    assetsBucket: IBucket;
    ecrRepo: IRepository;

    constructor(scope: Construct, id: string, props: ConfigProps) {
        super(scope, id, props);

        const bucket = new Bucket(this, 'IfRideAssetsBucket', {
            bucketName: `if-ride-assets-${props?.env?.region}-${props?.env?.account}`,
            versioned: true,
            encryption: BucketEncryption.S3_MANAGED,
            blockPublicAccess: BlockPublicAccess.BLOCK_ALL,
            removalPolicy: RemovalPolicy.DESTROY,
            autoDeleteObjects: true,
        });

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

        this.ecrRepo = new Repository(this, 'IfRideCoreRepo', {
            repositoryName: 'if-ride-core',
            removalPolicy: RemovalPolicy.DESTROY,
            emptyOnDelete: true,
            lifecycleRules: [
                {
                    maxImageCount: 1,
                    description: 'Limpeza de imagens antigas para economia de custo',
                },
            ],
        });

        bucket.addToResourcePolicy(cloudFrontAllowedPolicy);

        this.assetsBucket = bucket;

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