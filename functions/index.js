const jwt = require('jsonwebtoken');
const { SSMClient, GetParameterCommand } = require("@aws-sdk/client-ssm");

let cachedSecret;
const ssmClient = new SSMClient({});

exports.handler = async (event) => {
    try {
        if (!cachedSecret) {
            const command = new GetParameterCommand({
                Name: process.env.IF_RIDE_SECRET_PARAM,
                WithDecryption: true,
            });
            const response = await ssmClient.send(command);
            cachedSecret = response.Parameter.Value;
        }

        const authHeader = event.headers?.authorization || event.headers?.Authorization || event.authorizationToken;

        if (!authHeader || !authHeader.startsWith('Bearer ')) {
            console.log("Token ausente ou malformado");
            return generatePolicy('user', 'Deny', event.methodArn);
        }

        const token = authHeader.split(' ')[1];
        const decoded = jwt.verify(token, cachedSecret);

        return generatePolicy(decoded.sub || 'user', 'Allow', event.methodArn);

    } catch (error) {
        console.error('Falha na autorização:', error.message);
        return generatePolicy('user', 'Deny', event.methodArn);
    }
};

const generatePolicy = (principalId, effect, resource) => {
    return {
        principalId,
        policyDocument: {
            Version: '2012-10-17',
            Statement: [{
                Action: 'execute-api:Invoke',
                Effect: effect,
                Resource: resource,
            }],
        },
    };
};