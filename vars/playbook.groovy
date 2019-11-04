import com.intercress.*
import groovy.json.JsonOutput

String httpRequestCookie(String username, String password) {
    def credentials = JsonOutput.toJson([auth: username, password: password])
    def requestParams = [:]
    requestParams.acceptType = 'APPLICATION_JSON'
    requestParams.consoleLogResponseBody = true
    requestParams.contentType = 'APPLICATION_JSON'
    requestParams.httpMode = 'POST'
    requestParams.requestBody = credentials
    requestParams.url = 'http://localhost:3000/api/auth/login'
    def cookie = httpRequest requestParams
    def cookieContent = cookie.headers.get("Set-Cookie")    
    return cookieContent
}

String httpGetProjects(String cookie) {
    def cookieHeader = [:]
    cookieHeader.name = 'Cookie'
    cookieHeader.value = cookie
    def requestParams = [:]
    requestParams.acceptType = 'APPLICATION_JSON'
    requestParams.consoleLogResponseBody = true
    requestParams.contentType = 'APPLICATION_JSON'
    requestParams.customHeaders = [cookieHeader]
    requestParams.httpMode = 'GET'
    requestParams.url = 'http://localhost:3000/api/projects'
    def response = httpRequest requestParams
    return response.content
}

String httpGetTemplates(String cookie) {
    def cookieHeader = [:]
    cookieHeader.name = 'Cookie'
    cookieHeader.value = cookie
    def requestParams = [:]
    requestParams.acceptType = 'APPLICATION_JSON'
    requestParams.consoleLogResponseBody = true
    requestParams.contentType = 'APPLICATION_JSON'
    requestParams.customHeaders = [cookieHeader]
    requestParams.httpMode = 'GET'
    requestParams.url = 'http://localhost:3000/api/project/1/templates?sort=alias&order=asc'
    def response = httpRequest requestParams
    return response.content
}

String httpSendTask(String playbook, String cookie) {
    def schema = JsonOutput.toJson([template_id: 1, debug: false, dry_run: false, playbook: playbook, environment: ''])
    def cookieHeader = [:]
    cookieHeader.name = 'Cookie'
    cookieHeader.value = cookie
    def requestParams = [:]
    requestParams.acceptType = 'APPLICATION_JSON'
    requestParams.consoleLogResponseBody = true
    requestParams.contentType = 'APPLICATION_JSON'
    requestParams.customHeaders = [cookieHeader]
    requestParams.httpMode = 'POST'
    requestParams.requestBody = schema
    requestParams.url = 'http://localhost:3000/api/project/1/tasks'
    def response = httpRequest requestParams
    return response
}

def call(String name) {
    node {
        def String cookie
        def String project
        def String template
        
        stage ('authenticate') {
            def credentials = [:]
            credentials.credentialsId = 'semaphore'
            credentials.usernameVariable = 'username'
            credentials.passwordVariable = 'password'
            withCredentials([usernamePassword(credentials)]) {
                cookie = httpRequestCookie(username, password)[0]
            }
        }
    
        stage ('project') {
            projects = httpGetProjects(cookie)
            project = Semaphore.FindProject(projects, name)
        }
    
        stage ('template') {
            templates = httpGetTemplates(cookie)
//            template = Semaphore.FindTemplate(templates, project)
        }
    
        stage ('playbook') {
            status = httpSendTask(name, cookie)
        }

        echo "Hello, ${project}"
    }
}
