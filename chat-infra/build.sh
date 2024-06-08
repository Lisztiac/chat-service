echo Setting env vars...
COMMIT_HASH=$(echo $CODEBUILD_RESOLVED_SOURCE_VERSION | cut -c 1-7)
IMAGE_TAG=${COMMIT_HASH:=latest}

echo Logging into AWS ECR...
cd chat-app
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

echo Building Docker image...
docker build -t $IMAGE_REPO_URI:latest .
docker tag $IMAGE_REPO_URI:latest $IMAGE_REPO_URI:$IMAGE_TAG

echo Pushing Docker image...
docker push $IMAGE_REPO_URI:latest
docker push $IMAGE_REPO_URI:$IMAGE_TAG

echo Synthesizing...
cd ../chat-infra
npm install -g aws-cdk
cdk synth
