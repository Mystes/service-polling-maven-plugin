
def buildLog = new File(basedir, "build.log")

assert buildLog.text.contains("We got correct response code: 200")
assert buildLog.text.contains("Resuming!")