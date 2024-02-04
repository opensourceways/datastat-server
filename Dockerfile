FROM openeuler/openeuler:22.03

ARG NEW_YEAR_USER
ARG BRANCH

MAINTAINER zhongjun <jun.zhongjun2@gmail.com>

RUN mkdir -p /var/lib/ds
WORKDIR /var/lib/ds


RUN yum install -y wget \
    && wget https://mirrors-i.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/linux/OpenJDK17U-jdk_x64_linux_hotspot_17.0.10_7.tar.gz \
    && tar -zxvf OpenJDK17U-jdk_x64_linux_hotspot_17.0.10_7.tar.gz \
    && wget https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/3.8.8/binaries/apache-maven-3.8.8-bin.tar.gz \
    && tar -xzvf apache-maven-3.8.8-bin.tar.gz \
    && yum install -y git

ENV JAVA_HOME=/var/lib/ds/jdk-17.0.10+7
ENV PATH=${JAVA_HOME}/bin:$PATH

ENV MAVEN_HOEM=/var/lib/ds/apache-maven-3.8.8
ENV PATH=$MAVEN_HOEM/bin:$PATH
ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8

RUN git clone -b ${BRANCH} https://github.com/opensourceways/datastat-server && \
        cd datastat-server && \
        mvn clean install package -Dmaven.test.skip && \
        mv ./target/ds-0.0.1-SNAPSHOT.jar ../ds.jar

RUN useradd -u 1000 datastat -s /bin/bash -m -U && \
    git clone https://gitee.com/opensourceway/om-data.git && \
    chown -R datastat:datastat om-data

USER datastat
CMD java -jar ds.jar
