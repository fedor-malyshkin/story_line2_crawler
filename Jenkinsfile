#!groovy
node {
   def gradleHome
   stage('Preparation') { // for display purposes
      // Get some code from a GitHub repository
	  checkout changelog: true, poll: true, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanBeforeCheckout'], [$class: 'RelativeTargetDirectory', relativeTargetDir: 'crawler']], submoduleCfg: [], userRemoteConfigs: [[url: 'git@story_line2_crawler.github.com:fedor-malyshkin/story_line2_crawler.git']]]
	  checkout changelog: true, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'git@github.com:fedor-malyshkin/story_line2_build.git']]]
	  checkout changelog: true, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'deployment']], submoduleCfg: [], userRemoteConfigs: [[url: 'git@story_line2_deployment.github.com:fedor-malyshkin/story_line2_deployment.git']]]

	  //git 'git@story_line2_crawler.github.com:fedor-malyshkin/story_line2_crawler.git'

      gradleHome = tool name: 'gradle', type: 'gradle'
   }
   stage('Build') {
      // Run the maven build
     sh "'${gradleHome}/bin/gradle' -Pproject.ext.stand_type=test crawler:build"
   }
   stage('Results') {
      junit 'crawler/build/test-results/test/TEST-*.xml'
//      archive 'target/*.jar'
   }
}
