#!/bin/bash

oc config set-context --current --namespace=bnl-test-app-namespace

oc delete serviceaccount test-app-java-sdk-jwt-sa --ignore-not-found=true
oc create serviceaccount test-app-java-sdk-jwt-sa

oc delete secret generic java-sdk-credentials-jwt --ignore-not-found=true

oc create secret generic java-sdk-credentials-jwt  \
        --from-literal=conjur-service-id=ocp-cluster-dev  \
        --from-literal=conjur-account=conjur \
        --from-literal=conjur-jwt-token-path=/var/run/secrets/tokens/jwt \
        --from-literal=conjur-appliance-url=https://emea-cybr.secretsmgr.cyberark.cloud/api

# DEPLOYMENT
oc replace --force -f deployment.yml
if ! oc wait deployment test-app-java-sdk-jwt --for condition=Available=True --timeout=90s
  then exit 1
fi

oc get pods

