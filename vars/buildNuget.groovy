/**
* Builds and tests a nuget package. Assumes nunit3 as the test runner.
* Config Values:
*   project: The project to Build
*   testProject: The test project for running tests. Defaults to '${project}.Test'
*   isRelease: True if performing a release build. False if pre-release.
*/
def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def project = config.project ? "${config.project}/${config.project}.csproj" : null
    def packProjects = config.packProjects
    def testProject = config.testProject ? "${config.testProject}/${config.testProject}.csproj" : (config.project ? "${config.project}.Test/${config.project}.Test.csproj" : null)
    def testProjects = config.testProjects
    def isRelease = config.isRelease
    def releaseVersion = config.releaseVersion
    def isOpenSource = config.isOpenSource

    try {

        stage("Build") {
            def buildArgs = []
            if (releaseVersion) {
                buildArgs << "/p:VersionPrefix=${releaseVersion}"
            }
            dotnetBuild('', buildArgs)
        }

        if (testProject) {
            stage("Test") {
                dotnetTest(testProject, ['--logger', 'trx', '--noBuild'])
            }
        }

        if (testProjects) {
            stage("Test") {
                testProjects.each {
                    dotnetTest("${it}/${it}.csproj", ['--logger', 'trx', '--noBuild'])
                }
            }
        }

        if (project) {
            stage("Package") {
                def packArgs = ['--noBuild']
                if (!isRelease) {
                    packArgs << "--version-suffix ${env.BRANCH_NAME.take(10)}-${env.BUILD_NUMBER}"
                }
                if (isOpenSource) {
                    packArgs << '--include-source'
                }
                if (releaseVersion) {
                    packArgs << "/p:VersionPrefix=${releaseVersion}"
                }
                dotnetPack(project, packArgs)
            }
        }

        if (packProjects) {
            stage("Package") {
                packProjects.each {
                    def packArgs = ['--noBuild']
                    if (!isRelease) {
                        packArgs << "--version-suffix ${env.BRANCH_NAME.take(10)}-${env.BUILD_NUMBER}"
                    }
                    if (isOpenSource) {
                        packArgs << '--include-source'
                    }
                    if (releaseVersion) {
                        packArgs << "/p:VersionPrefix=${releaseVersion}"
                    }
                    dotnetPack("${it}/${it}.csproj", packArgs)
                }
            }
        }
        
        stage("Reporting") {
            reportMSTestResults("**/*.trx")
            archiveArtifacts artifacts: "**/*.nupkg", excludes: "**/*.symbols.nupkg"
            stash excludes: "**/*.symbols.nupkg", includes: "**/*.nupkg", name: "nupkg"
        }
    } catch (any) {
        currentBuild.result = "FAILURE"
        throw any
    } finally {
        deleteDir()
        emailext attachLog: true, recipientProviders: [[$class: 'CulpritsRecipientProvider']]
    }
}