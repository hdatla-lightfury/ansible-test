import jenkins.model.*
import hudson.security.*
import hudson.util.*
import jenkins.plugins.jobdsl.*
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval
import java.security.MessageDigest

def jenkinsInstance = Jenkins.getInstance()

// Define the root directory to start searching for .groovy files (e.g., Jenkins home directory)
def rootDir = "/var/jenkins_home"  // You can modify this path based on your setup

def scriptApprovalList = jenkinsInstance.getExtensionList(ScriptApproval.class)
if (scriptApprovalList.isEmpty()) {
    throw new Exception("ScriptApproval extension not found!")
}

def scriptApproval = scriptApprovalList.get(0)

scriptApproval.preapproveAll()
