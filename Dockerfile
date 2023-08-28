FROM ubuntu:20.04

ARG NEW_YEAR_USER
ARG BRANCH

MAINTAINER zhongjun <jun.zhongjun2@gmail.com>

RUN mkdir -p /var/lib/ds
WORKDIR /var/lib/ds

RUN apt-get update && \
    apt-get install --yes software-properties-common

RUN apt install --yes openjdk-17-jdk
RUN apt-get install --yes wget
RUN apt-get install --yes git

RUN wget https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/3.8.8/binaries/apache-maven-3.8.8-bin.tar.gz && \
        tar -xzvf apache-maven-3.8.8-bin.tar.gz
ENV MAVEN_HOEM=/var/lib/ds/apache-maven-3.8.8
ENV PATH=$MAVEN_HOEM/bin:$PATH

COPY settings.xml ${MAVEN_HOEM}/conf/settings.xml

RUN git clone -b ${BRANCH} https://github.com/opensourceways/datastat-server && \
        cd datastat-server && \
        mvn clean install package -Dmaven.test.skip && \
        mv ./target/ds-0.0.1-SNAPSHOT.jar ../ds.jar

RUN useradd -u 1000 datastat -s /bin/bash -m -U && \
    git clone https://${NEW_YEAR_USER}@gitee.com/lixianlin01/new-year.git && \
    git clone https://gitee.com/opensourceway/om-data.git && \
    chown -R datastat:datastat new-year && \
    chown -R datastat:datastat om-data

USER datastat
CMD java -jar ds.jar
