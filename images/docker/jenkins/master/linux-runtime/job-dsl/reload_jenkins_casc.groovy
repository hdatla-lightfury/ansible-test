pipelineJob('Reload Jenkins Casc') {
    definition {
        cps {
            script('''
                pipeline {
                    agent {
                        label 'built-in'
                    }
                    environment {
                        JOB_DSL_DEST = "/var/jenkins_home/"
                        JCASC_DEST   = "/var/jenkins_home/"
                    }
                    stages {
                        stage('Checkout from Perforce') {
                            steps {
                                p4sync(
                                    charset: 'none',
                                    credential: 'perforce-credentials-build-lightfury',

                                    populate: autoClean(
                                                delete: true,
                                                modtime: false,
                                                force: true,
                                                parallel: [enable: false, minbytes: '1024', minfiles: '1', threads: '4'],
                                                pin: '',
                                                quiet: true,
                                                replace: true,
                                                tidy: false
                                               ),

                                    workspace: manualSpec(
                                                    charset: 'none',
                                                    name: 'build-lightfury_lfg_titan_ci-jenkins-master',
                                                    spec: clientSpec(
                                                        allwrite: false,
                                                        clobber: false,
                                                        compress: false,
                                                        line: 'LOCAL',
                                                        locked: false,
                                                        modtime: false,
                                                        rmdir: false,
                                                        streamName: '',
                                                        view: """//titan-ci/mainline/src/docker/docker_stages/jenkins_master/linux/... //build-lightfury_lfg_titan_ci-jenkins-master/..."""
                                                 )
                                    )
                                )
                            }
                        }

                        stage('Copy Job DSL and JCasC Files') {
                            steps {
                                script {
                                    echo "Copying Job DSL folder..."
                                    sh """
                                        sudo cp -r "/var/jenkins_home/workspace/Reload Jenkins Casc/job-dsl" "${JOB_DSL_DEST}"
                                    """
                                    echo "Copying JCasC file..."
                                    sh """
                                        sudo cp "/var/jenkins_home/workspace/Reload Jenkins Casc/jenkins-casc.yaml" "${JCASC_DEST}"
                                    """
                                }
                            }
                        }
                    }
                }
            ''')
        }
    }
}
