#!groovy
node {
   def projectName = 'crawler'
   def gradleHome = tool name: 'gradle', type: 'gradle'
   stage('Checkout') { // for display purposes
      // Get some code from a GitHub repository
	  // project itself
	  checkout changelog: true, poll: true, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanBeforeCheckout'], [$class: 'RelativeTargetDirectory', relativeTargetDir: 'crawler']], submoduleCfg: [], userRemoteConfigs: [[url: "story_line2_${projectName}.github.com:fedor-malyshkin/story_line2_${projectName}.git"]]]
	  // parent directory
	  checkout changelog: true, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'story_line2_build.github.com:fedor-malyshkin/story_line2_build.git']]]
	  // deployment
	  checkout changelog: true, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'deployment']], submoduleCfg: [], userRemoteConfigs: [[url: 'story_line2_deployment.github.com:fedor-malyshkin/story_line2_deployment.git']]]
   }
   stage('Assemble') {
      // Run the maven build
     sh "'${gradleHome}/bin/gradle' -Pproject.ext.stand_type=test ${projectName}:assemble"
   }
   stage('Test') {
      sh "'${gradleHome}/bin/gradle' -Pproject.ext.stand_type=test ${projectName}:test"
   }
   stage('Collect Reports') {
      junit "${projectName}/build/test-results/test/TEST-*.xml"
   }
}
