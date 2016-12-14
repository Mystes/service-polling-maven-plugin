
def buildLog = new File(basedir, "build.log")

assert buildLog.text.contains("No connection. Retrying...")
assert buildLog.text.contains("BUILD FAILURE")