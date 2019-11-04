#!/bin/bash

set -o nounset \
  -o errexit \
  -o verbose \
  -o xtrace

# Generate CA key
openssl req -new -x509 -keyout ca-root.key -out ca-root.crt -days 101 -subj '/CN=ca.bank.openweb.nl/OU=TEST/O=Openweb/L=Nieuwegein/S=Utrecht/C=NL' -passin pass:kafka-graphql-examples -passout pass:kafka-graphql-examples

# Standard OpenSSL keys for non JVM use
openssl genrsa -des3 -passout "pass:kafka-graphql-examples" -out command-handler.key 2048
openssl req -passin "pass:kafka-graphql-examples" -passout "pass:kafka-graphql-examples" -key command-handler.key -new -out command-handler.client.req -subj '/CN=command-handler/OU=TEST/O=Openweb/L=Nieuwegein/S=Utrecht/C=NL'
openssl x509 -req -passin "pass:kafka-graphql-examples" -in command-handler.client.req -CA ca-root.crt -CAkey ca-root.key -CAserial ca-root.srl -out command-handler-signed.pem -days 100 -CAcreateserial

docker volume create --name kafka-graphql-certs
docker container create --name dummy -v kafka-graphql-certs:/root hello-world

for f in ca-root.crt command-handler.key command-handler-signed.pem; do
  docker cp ${f} dummy:/root/
done

for i in kafka-1 kafka-2 kafka-3 command-handler graphql-endpoint; do
  echo ${i}
  # Create keystores
  keytool -genkey -noprompt \
    -alias ${i} \
    -dname "CN=${i}, OU=TEST, O=Openweb, L=Nieuwegein, S=Utrecht, C=NL" \
    -keystore kafka.${i}.keystore.jks \
    -keyalg RSA \
    -storepass kafka-graphql-examples \
    -keypass kafka-graphql-examples

  # Create CSR, sign the key and import back into keystore
  keytool -keystore kafka.${i}.keystore.jks -alias ${i} -certreq -file ${i}.csr -storepass kafka-graphql-examples -keypass kafka-graphql-examples

  openssl x509 -req -CA ca-root.crt -CAkey ca-root.key -in ${i}.csr -out ${i}-ca-signed.crt -days 100 -CAserial ca-root.srl -passin "pass:kafka-graphql-examples"

  keytool -noprompt -keystore kafka.${i}.keystore.jks -alias CARoot -import -file ca-root.crt -storepass kafka-graphql-examples -keypass kafka-graphql-examples

  keytool -noprompt -keystore kafka.${i}.keystore.jks -alias ${i} -import -file ${i}-ca-signed.crt -storepass kafka-graphql-examples -keypass kafka-graphql-examples

  # Create truststore and import the CA cert.
  keytool -noprompt -keystore kafka.${i}.truststore.jks -alias CARoot -import -file ca-root.crt -storepass kafka-graphql-examples -keypass kafka-graphql-examples

  for f in kafka.${i}.keystore.jks kafka.${i}.truststore.jks; do
    docker cp ${f} dummy:/root/
  done

  if [[ ${i} == kafka-* ]]; then
    echo "kafka-graphql-examples" >${i}_sslkey_creds
    echo "kafka-graphql-examples" >${i}_keystore_creds
    echo "kafka-graphql-examples" >${i}_truststore_creds

    for f in ${i}_sslkey_creds ${i}_keystore_creds ${i}_truststore_creds; do
      docker cp ${f} dummy:/root/
    done
  fi
done

docker rm dummy
