# Use the official Jenkins LTS image with JDK 17 as the base image
FROM jenkins/jenkins:lts-jdk17
# Switch to root user to install additional packages
USER root

# Update the package list and install necessary packages
RUN apt-get update && apt-get install -y \
    lsb-release \
    python3-pip \
    wget \
    curl \
    apt-transport-https \
    ca-certificates \
    gnupg

# Add Docker's official GPG key and Docker repository
RUN curl -fsSLo /usr/share/keyrings/docker-archive-keyring.asc https://download.docker.com/linux/debian/gpg && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.asc] \
    https://download.docker.com/linux/debian $(lsb_release -cs) stable" > /etc/apt/sources.list.d/docker.list

# Update the package list again and install Docker CLI
RUN apt-get update && apt-get install -y docker-ce-cli

# Install Perforce CLI using updated key management
RUN wget -qO - https://package.perforce.com/perforce.pubkey | gpg --dearmor > /usr/share/keyrings/perforce-archive-keyring.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/perforce-archive-keyring.gpg] http://package.perforce.com/apt/ubuntu focal release" > /etc/apt/sources.list.d/perforce.list && \
    apt-get update && apt-get install -y helix-cli

# Create Docker group if not exists and add Jenkins user to it and give sudo permission
RUN apt-get update && apt-get install -y sudo && \
    groupadd -f docker && \
    usermod -aG docker jenkins && \
    echo "jenkins ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

COPY plugin-list.txt /var/jenkins_home/plugin-list.txt

RUN jenkins-plugin-cli --plugin-file /var/jenkins_home/plugin-list.txt
# Copy all job DSL scripts from the jobs directory
COPY job-dsl /var/jenkins_home/job-dsl

RUN mkdir /var/jenkins_home/init.groovy.d

COPY job-dsl/init.groovy.d /var/jenkins_home/init.groovy.d

COPY jenkins-casc.yaml /var/jenkins_home/jenkins-casc.yaml

ARG CASC_JENKINS_CONFIG \
    JENKINS_ADMIN_PASSWORD \
    JENKINS_URL \
    GOOGLE_CLIENT_ID \
    GOOGLE_CLIENT_SECRET \
    SLACK_TOKEN \
    SWARM_AGENT_TOKEN \
    AUTHSPHERE_TOKEN \
    PERFORCE_TRIGGER_API_TOKEN \
    JENKINS_ADMIN_GOOGLE_APP_PASSWORD

ENV CASC_JENKINS_CONFIG=${CASC_JENKINS_CONFIG}\
    JENKINS_ADMIN_PASSWORD=${JENKINS_ADMIN_PASSWORD} \
    JENKINS_URL=${JENKINS_URL} \
    GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID} \
    GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET} \
    SLACK_TOKEN=${SLACK_TOKEN} \
    SWARM_AGENT_TOKEN=${SWARM_AGENT_TOKEN} \
    AUTHSPHERE_TOKEN=${AUTHSPHERE_TOKEN} \
    PERFORCE_TRIGGER_API_TOKEN=${PERFORCE_TRIGGER_API_TOKEN} \
    JENKINS_ADMIN_GOOGLE_APP_PASSWORD=${JENKINS_ADMIN_GOOGLE_APP_PASSWORD}

# Switch back to the Jenkins user
USER jenkins
# Use the official Jenkins LTS image with JDK 17 as the base image
FROM jenkins/jenkins:lts-jdk17
# Switch to root user to install additional packages
USER root

# Update the package list and install necessary packages
RUN apt-get update && apt-get install -y \
    lsb-release \
    python3-pip \
    wget \
    curl \
    apt-transport-https \
    ca-certificates \
    gnupg

# Add Docker's official GPG key and Docker repository
RUN curl -fsSLo /usr/share/keyrings/docker-archive-keyring.asc https://download.docker.com/linux/debian/gpg && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.asc] \
    https://download.docker.com/linux/debian $(lsb_release -cs) stable" > /etc/apt/sources.list.d/docker.list

# Update the package list again and install Docker CLI
RUN apt-get update && apt-get install -y docker-ce-cli

# Install Perforce CLI using updated key management
RUN wget -qO - https://package.perforce.com/perforce.pubkey | gpg --dearmor > /usr/share/keyrings/perforce-archive-keyring.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/perforce-archive-keyring.gpg] http://package.perforce.com/apt/ubuntu focal release" > /etc/apt/sources.list.d/perforce.list && \
    apt-get update && apt-get install -y helix-cli

# Create Docker group if not exists and add Jenkins user to it and give sudo permission
RUN apt-get update && apt-get install -y sudo && \
    groupadd -f docker && \
    usermod -aG docker jenkins && \
    echo "jenkins ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

COPY plugin-list.txt /var/jenkins_home/plugin-list.txt

RUN jenkins-plugin-cli --plugin-file /var/jenkins_home/plugin-list.txt
# Copy all job DSL scripts from the jobs directory
COPY job-dsl /var/jenkins_home/job-dsl

RUN mkdir /var/jenkins_home/init.groovy.d

COPY job-dsl/init.groovy.d /var/jenkins_home/init.groovy.d

COPY jenkins-casc.yaml /var/jenkins_home/jenkins-casc.yaml

ARG CASC_JENKINS_CONFIG \
    JENKINS_ADMIN_PASSWORD \
    JENKINS_URL \
    GOOGLE_CLIENT_ID \
    GOOGLE_CLIENT_SECRET \
    SLACK_TOKEN \
    SWARM_AGENT_TOKEN \
    AUTHSPHERE_TOKEN \
    PERFORCE_TRIGGER_API_TOKEN \
    JENKINS_ADMIN_GOOGLE_APP_PASSWORD

ENV CASC_JENKINS_CONFIG=${CASC_JENKINS_CONFIG}\
    JENKINS_ADMIN_PASSWORD=${JENKINS_ADMIN_PASSWORD} \
    JENKINS_URL=${JENKINS_URL} \
    GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID} \
    GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET} \
    SLACK_TOKEN=${SLACK_TOKEN} \
    SWARM_AGENT_TOKEN=${SWARM_AGENT_TOKEN} \
    AUTHSPHERE_TOKEN=${AUTHSPHERE_TOKEN} \
    PERFORCE_TRIGGER_API_TOKEN=${PERFORCE_TRIGGER_API_TOKEN} \
    JENKINS_ADMIN_GOOGLE_APP_PASSWORD=${JENKINS_ADMIN_GOOGLE_APP_PASSWORD}

# Switch back to the Jenkins user
USER jenkins
