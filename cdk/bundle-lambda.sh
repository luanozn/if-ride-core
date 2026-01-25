#!/bin/bash

SOURCE_DIR="../functions/authorizer"
DEST_DIR="./dist"
ZIP_NAME="authorizer.zip"

mkdir -p $DEST_DIR

echo "Empacotando Lambda Authorizer..."
cd $SOURCE_DIR
npm install --production
zip -r ../../cdk/$DEST_DIR/$ZIP_NAME . -x "*.ts" "*.map"

echo "Lambda compactado em $DEST_DIR/$ZIP_NAME"