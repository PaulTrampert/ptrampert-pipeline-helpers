import com.ptrampert.SemVer

def call() {
    def describeString
    if (isUnix()) {
        describeString = sh returnStdout: true, script: 'git describe --tags'
    }
    else {
        describeString = bat returnStdout: true, script: 'git describe --tags'
    }
    echo "describeString = ${describeString}"
    def result = calculateSemver describeString
    echo "semver.toString() = ${result}"
    return result
}

def calculateSemver(str) {
    def semver = SemVer.Parse str
    semver.minor++
    return semver.toString()
}