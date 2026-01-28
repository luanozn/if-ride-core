#!/bin/bash
set -e

ROOT_DIR="${GITHUB_WORKSPACE:-$(pwd)}"

CDK_DIR="$ROOT_DIR/cdk"
SOURCE_DIR="$ROOT_DIR/functions"
DEST_DIR="$CDK_DIR/dist"
ZIP_NAME="authorizer.zip"

mkdir -p "$DEST_DIR"

echo "Limpando dist e empacotando Lambda..."
rm -f "$DEST_DIR/$ZIP_NAME"

if [ -d "$SOURCE_DIR" ]; then
    cd "$SOURCE_DIR"

    echo "Instalando dependências..."
    npm ci --omit=dev

    echo "Compactando Lambda..."
    zip -r "$DEST_DIR/$ZIP_NAME" . \
      -x "*.js" "*.map" "node_modules/.bin/*"

    echo "Lambda compactado com sucesso em: $DEST_DIR/$ZIP_NAME"
else
    echo "ERRO CRÍTICO: Diretório do Authorizer não encontrado em $SOURCE_DIR"
    echo "Conteúdo de functions:"
    ls -la "$ROOT_DIR/functions" || true
    exit 1
fi

cd "$CDK_DIR"
