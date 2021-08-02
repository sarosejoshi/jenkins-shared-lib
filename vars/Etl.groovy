def call(body)
{
    def pipelineParams= [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()
pipeline {
  agent any
 environment{
    registryCredential = 'docker_id1'
    gpg_secret = credentials("saroj_gpg_etl_keys")
    gpg_passphrase = credentials("saroj_gpg_etl_keys_pass")
 }
    
stages {
    stage("Master") {
      when {
        branch 'master'

      }
      steps {
        echo 'we are in Master branch'
        
        
      }

    }
    stage("Develop") {
      when {
        branch 'develop'

      }
      steps {
        echo 'we are in Develop branch'
      }

    }
    stage("Test") {
      when {
        branch 'test'

      }
      steps {
        echo 'we are in test branch'

      }

    }
   stage('Decrypt a master secret file')
    {
      when {
        branch 'master'
      }
      steps{
        
        script{
          sh '''
               
                
               cd config/
               gpg --batch --import $gpg_secret
               
                git secret reveal -p $gpg_passphrase
                '''
                
          
        }
      }
    }
    stage('Decrypt a develop secret file')
    {
      when {
        branch 'develop'
      }
      steps{
        
        script{
          sh '''
               
                
               cd config/
               gpg --batch --import $gpg_secret
               
                git secret reveal -p $gpg_passphrase
                '''
                
          
        }
      }
    }
    stage('Decrypt a test secret file')
    {
      when{
      branch 'test'
      }
      steps{
        
        script{
          sh '''
               
                
               cd config/
               gpg --batch --import $gpg_secret
               
                git secret reveal -p $gpg_passphrase
                '''
                
          
        }
      }
    }
 stage('Building a image for amazon-associate-etl ') {
      when {
        changeset "amazon-associate-etl/docker-images/amazon-associate-service/**"
      }
      steps {
        script {
          docker.withRegistry('', registryCredential) {
            sh '''
               cd amazon-associate-etl/docker-images/amazon-associate-service/
               make build-image '''

          }
        }

      }
      
    }
    stage('Test a image for  amazon-associate-etl ') {
      when {
        changeset "amazon-associate-etl/docker-images/amazon-associate-service/**"
      }
      steps {
        script {
          
            sh '''
               cd amazon-associate-etl/docker-images/amazon-associate-service/
               make test-image '''

          
        }

      }
      
    }
    stage('Push a image amazon-associate-etl ') {
      when {
        changeset "amazon-associate-etl/docker-images/amazon-associate-service/**"
      }
      steps {
        script {
          docker.withRegistry('', registryCredential) {
            sh '''
               cd amazon-associate-etl/docker-images/amazon-associate-service/
               make push-image '''

          }
        }

      }
      
    }
    stage('Pre-deploy image for amazon-associate-etl ') {
      when {
        changeset "amazon-associate-etl/docker-images/amazon-associate-service/**"
      }
      steps {
        script {
          
            sh '''
               cd amazon-associate-etl/docker-images/amazon-associate-service/
               make pre-deploy-image '''

        }
        }

      }
      
    
    stage('deploy image for amazon-associate-etl ') {
      when {
        changeset "amazon-associate-etl/docker-images/amazon-associate-service/**"
      }
      steps {
        script {
          docker.withRegistry('', registryCredential) {
            sh '''
               cd amazon-associate-etl/docker-images/amazon-associate-service/
               make deploy-dockerimage '''

          }
        }

      }
      
    }
    stage('Post-deploy image for amazon-associate-etl ') {
      when {
        branch 'test'
        changeset "amazon-associate-etl/docker-images/amazon-associate-service/**"
      }
      steps {
        script {
        
            sh '''
               cd amazon-associate-etl/docker-images/amazon-associate-service/
               make post-deploy-image '''

          
        }

      }
      
    }
    stage('Release Tag') {
      when{
     
        expression {
    env.TAG_NAME != null
          
    }
      }
      steps {
        script {
          
          docker.withRegistry( '', registryCredential ) {
            sh '''
               cd ch1-2-migration/docker-images/ch-entity-validation/
                ENV=${TAG_NAME} make deploy '''
           }
        }
      }
    }

    stage('Copy json file to container'){
      steps {
        script {
          
          sh '''
          cd amazon-associate-etl/dag/
          docker cp amazon-associate-etl/dag/amazon_associate_etl.py eeb82e397165:/opt/airflow/dags

          '''
            sh '''
               cd config
                docker cp common/airflow/amazon_associate_etl_config.json eeb82e397165:/opt/airflow/dags
                docker exec -i eeb82e397165 airflow variables import /opt/airflow/dags/amazon_associate_etl_config.json
                docker exec -i eeb82e397165 airflow variables get amazon_associate_etl_config

                 '''
        }
      }
    }

}
}
}
