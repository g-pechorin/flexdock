pipeline {
	agent any

	stages {

		stage('chMod') {
			steps {
				sh 'chmod +=rwx gradlew'
			}
		}

		stage('clone m2-repo') {
			steps {
		        dir('.m2repo/') {
                    deleteDir()
				}
				sh 'git init .m2repo/'
				dir ('.m2repo/') {
					sh 'git remote add origin git@github.com:g-pechorin/m2-repo.git'
					sh 'git fetch'
					sh 'git checkout gh-pages'
					sh 'git pull origin gh-pages'
				}
			}
		}

		stage('gradle - clean') {
			steps {
				sh './gradlew clean'
			}
		}

		stage('gradle - assemble') {
			steps {
				sh './gradlew assemble'
			}
		}

		stage('gradle - test') {
			steps {
				sh './gradlew test'
			}
		}

		stage('gradle - build') {
			steps {
				sh './gradlew build'
			}
		}

		stage('gradle - publish') {
			steps {
				sh './gradlew publish'
			}
		}
		
		stage('Deploy m2-repo') {
			steps {
				dir ('.m2repo/') {
					sh 'git add .'
					sh 'git commit -m "${BUILD_TAG}"'
					sh 'git push origin gh-pages'
				}
			}
		}
	}
}
