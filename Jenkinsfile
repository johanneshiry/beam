pipeline {
  agent any
  stages {
    stage('checkout') {
      steps {
        git(url: 'https://github.com/LBNL-UCB-STI/beam.git', branch: '**', changelog: true, poll: true)
      }
    }
    stage('build') {
      steps {
        sh './gradlew build'
      }
    }
  }
}
