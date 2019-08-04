#!/bin/bash

mkdir secrets && cd secrets

set -o nounset \
  -o errexit \
  -o verbose \
  -o xtrace

# Generate CA key
openssl req -new -x509 -keyout ca-root.key -out ca-root.crt -days 101 -subj '/CN=ca.bank.openweb.nl/OU=TEST/O=Openweb/L=Nieuwegein/S=Utrecht/C=NL' -passin pass:openbanksafe -passout pass:openbanksafe

# Standard OpenSSL keys for non JVM use
openssl genrsa -des3 -passout "pass:openbanksafe" -out command-handler.key 2048
openssl req -passin "pass:openbanksafe" -passout "pass:openbanksafe" -key command-handler.key -new -out command-handler.client.req -subj '/CN=command-handler/OU=TEST/O=Openweb/L=Nieuwegein/S=Utrecht/C=NL'
openssl x509 -req -passin "pass:openbanksafe" -in command-handler.client.req -CA ca-root.crt -CAkey ca-root.key -CAserial ca-root.srl -out command-handler-signed.pem -days 100 -CAcreateserial

docker volume create --name open-bank-certs
docker container create --name dummy -v open-bank-certs:/root hello-world

for f in ca-root.crt command-handler.key command-handler-signed.pem; do
  docker cp ${f} dummy:/root/
done

for i in kafka-1 kafka-2 kafka-3 command-handler command-generator heartbeat graphql-endpoint; do
  echo ${i}
  # Create keystores
  keytool -genkey -noprompt \
    -alias ${i} \
    -dname "CN=${i}, OU=TEST, O=Openweb, L=Nieuwegein, S=Utrecht, C=NL" \
    -keystore kafka.${i}.keystore.jks \
    -keyalg RSA \
    -storepass openbanksafe \
    -keypass openbanksafe

  # Create CSR, sign the key and import back into keystore
  keytool -keystore kafka.${i}.keystore.jks -alias ${i} -certreq -file ${i}.csr -storepass openbanksafe -keypass openbanksafe

  openssl x509 -req -CA ca-root.crt -CAkey ca-root.key -in ${i}.csr -out ${i}-ca-signed.crt -days 100 -CAserial ca-root.srl -passin "pass:openbanksafe"

  keytool -noprompt -keystore kafka.${i}.keystore.jks -alias CARoot -import -file ca-root.crt -storepass openbanksafe -keypass openbanksafe

  keytool -noprompt -keystore kafka.${i}.keystore.jks -alias ${i} -import -file ${i}-ca-signed.crt -storepass openbanksafe -keypass openbanksafe

  # Create truststore and import the CA cert.
  keytool -noprompt -keystore kafka.${i}.truststore.jks -alias CARoot -import -file ca-root.crt -storepass openbanksafe -keypass openbanksafe

  for f in kafka.${i}.keystore.jks kafka.${i}.truststore.jks; do
    docker cp ${f} dummy:/root/
  done

  if [[ ${i} == kafka-* ]]; then
    echo "openbanksafe" >${i}_sslkey_creds
    echo "openbanksafe" >${i}_keystore_creds
    echo "openbanksafe" >${i}_truststore_creds

    for f in ${i}_sslkey_creds ${i}_keystore_creds ${i}_truststore_creds; do
      docker cp ${f} dummy:/root/
    done
  fi
done

docker rm dummy && cd .. && rm -rf secrets
