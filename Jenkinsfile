#!groovy
node {
   def gradleHome
   stage('Preparation') { // for display purposes
      // Get some code from a GitHub repository
    checkout([
        $class: 'GitSCM',
        branches: [[name: '*/master']],
        doGenerateSubmoduleConfigurations: true,
        extensions: scm.extensions + [[$class: 'SubmoduleOption', parentCredentials: true]],
        userRemoteConfigs: [[url: 'git@github.com:fedor-malyshkin/story_line2_build.git']]
        ])

      gradleHome = tool name: 'gradle', type: 'gradle'
   }
   stage('Build') {
      // Run the maven build
     sh "'${gradleHome}/bin/gradle' crawler:build"
   }
   stage('Results') {
      junit '**/target/surefire-reports/TEST-*.xml'
      archive 'target/*.jar'
   }
}
