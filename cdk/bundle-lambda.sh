#!/bin/bash
set -e

CDK_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
SOURCE_DIR="$CDK_DIR/../functions/authorizer"
DEST_DIR="$CDK_DIR/dist"
ZIP_NAME="authorizer.zip"

mkdir -p "$DEST_DIR"

echo "Limpando dist e empacotando Lambda..."
rm -f "$DEST_DIR/$ZIP_NAME"

if [ -d "$SOURCE_DIR" ]; then
    cd "$SOURCE_DIR"
    npm install --omit=dev
    zip -r "$DEST_DIR/$ZIP_NAME" . -x "*.ts" "*.map" "node_modules/.bin/*"
    echo "Lambda compactado com sucesso em: $DEST_DIR/$ZIP_NAME"
else
    echo "ERRO CRÍTICO: Diretório do Authorizer não encontrado em $SOURCE_DIR"
    exit 1
fi