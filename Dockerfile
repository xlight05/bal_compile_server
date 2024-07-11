# --------------------------------------------------------------------
# Copyright (c) 2024, WSO2 LLC. (http://wso2.com)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# -----------------------------------------------------------------------

FROM gradle:7.6.0-jdk17 AS gradle-build

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
COPY src/main/resources/application.properties src/main/resources/application.properties
RUN gradle clean build --no-daemon

FROM ballerina/ballerina:2201.8.5 AS ballerina-pack

FROM eclipse-temurin:21-jdk-alpine


RUN mkdir -p /work-dir \
    && addgroup troupe \
    && adduser -G troupe \
    --disabled-password \
    --gecos "" \
    --home "/home/choreo" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid 10014 \
    "choreo" \
    && mkdir -p /home/choreo \
    && chown choreo:troupe /home/choreo

COPY --chown=choreo:troupe --from=gradle-build /home/gradle/src/build/libs/*.jar /work-dir/app/project-api-2.0.0.jar
COPY --chown=choreo:troupe --from=ballerina-pack /ballerina/runtime /work-dir/ballerina
ENV BALLERINA_HOME /work-dir/ballerina

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS "-XX:+UseContainerSupport -XX:MaxRAMPercentage=80.0 -XX:TieredStopAtLevel=1"

USER 10014

ENTRYPOINT [ "java", "-jar", "/work-dir/app/project-api-2.0.0.jar" ]