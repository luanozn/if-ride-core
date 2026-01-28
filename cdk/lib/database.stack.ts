import {RemovalPolicy, Stack,} from "aws-cdk-lib";
import { Construct } from "constructs";
import { ParameterUtils } from "./utils/parameter-utils";
import { ConfigProps } from "./utils/config-props";
import {InstanceClass, InstanceSize, InstanceType, Port, SecurityGroup, SubnetType, Vpc} from "aws-cdk-lib/aws-ec2";
import {
    Credentials,
    DatabaseInstance,
    DatabaseInstanceEngine,
    DatabaseSecret,
    PostgresEngineVersion
} from "aws-cdk-lib/aws-rds";
import {SSM_PREFIX} from "./utils/constants";

export class DatabaseStack extends Stack {

    constructor(scope: Construct, id: string, props: ConfigProps) {
        super(scope, id, props);

        const vpc = props.resources?.vpc!

        const rdsSecurityGroup = new SecurityGroup(this, 'RdsSG', {
            vpc,
            allowAllOutbound: true,
            description: 'Acesso ao RDS PostgreSQL para o Backend',
        });

        const appSecurityGroup = props.resources?.securityGroup!

        rdsSecurityGroup.addIngressRule(
            appSecurityGroup,
            Port.tcp(5432),
            'Permitir-Backend-SpringBoot'
        );

        const databaseUsername = ParameterUtils.retrieveParameter(this, "DatabaseUser", props.parameterNames.databaseUsername).stringValue;

        const databaseSecret = new DatabaseSecret(this, 'IfRideDbSecret', {
            username: databaseUsername,
            secretName: `${SSM_PREFIX}/database/credentials`,
        });

        const dbInstance = new DatabaseInstance(this, 'IfRideDb', {
            engine: DatabaseInstanceEngine.postgres({
                version: PostgresEngineVersion.VER_16,
            }),
            instanceType: InstanceType.of(InstanceClass.T3, InstanceSize.MICRO),
            vpc,
            vpcSubnets: {
                subnetType: SubnetType.PRIVATE_ISOLATED,
            },
            securityGroups: [rdsSecurityGroup],
            allocatedStorage: 20,
            maxAllocatedStorage: 20,
            databaseName: 'if_ride_db',
            credentials: Credentials.fromSecret(databaseSecret),
            removalPolicy: RemovalPolicy.DESTROY,
            deletionProtection: false,
        });

    }
}