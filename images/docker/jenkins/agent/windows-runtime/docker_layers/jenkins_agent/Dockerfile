ARG BASE_IMAGE
FROM $BASE_IMAGE

# Define additional build arguments
ARG JENKINS_URL SWARM_MASTER SWARM_USER SWARM_PASS SWARM_EXECUTORS LABELS \
    AGENT_NAME JENKINS_AGENT_WORKSPACE

ENV JENKINS_URL=$JENKINS_URL SWARM_MASTER=$SWARM_MASTER SWARM_USER=$SWARM_USER \
    SWARM_PASS=$SWARM_PASS SWARM_EXECUTORS=$SWARM_EXECUTORS LABELS=$LABELS AGENT_NAME=$AGENT_NAME \
    JENKINS_AGENT_WORKSPACE=$JENKINS_AGENT_WORKSPACE

WORKDIR C:\\jenkins

# Copy the Jenkins agent swarm client jar file from Jenkins master
ADD ${SWARM_MASTER}/swarm/swarm-client.jar C:\\jenkins\\swarm-client.jar
# Create a directory for the cached workspace
RUN mkdir C:\\workspace

COPY entrypoint.bat C:\\jenkins\\entrypoint.bat

COPY pip-requirements.txt C:\\jenkins\\pip-requirements.txt

COPY scripts/install_symsorter.py C:\\jenkins\\install_symsorter.py

ENTRYPOINT ["cmd", "/S", "/C", "C:\\jenkins\\entrypoint.bat"]
