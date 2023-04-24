# Sets up a proxy and (optionally) remote datastore
# for any number of interactive clients
# Format for running the script is:
# python interactiveClient.py configFileName nbClients

import sys
sys.path.append('../../../../../scripts/util/')
from compile_util import *
from prop_util import *
from ssh_util import *
import time
import random

if (len(sys.argv) != 3):
    print "Incorrect format: Expected <configFileName nbClients>"
    exit()

# Obtain config file (this will be the same for all nodes)
# NB: as the file is the same, the port number should never
# be specified (and instead generated automatically by the
# NodeConfiguration module in the system)

configFileName = sys.argv[1]
nbClients = int(sys.argv[2])

jarPath = "/media/ncrooks/Data/Documents/PhD/shield/target/"
jarName = "shield-1.0-SNAPSHOT.jar"
clientMain = "shield.tests.InteractiveClient"
proxyMain = "shield.benchmarks.utils.StartProxy"
datastoreMain = "shield.benchmarks.utils.StartDataStore"

properties = loadPropertyFile(configFileName)
localSrcDir = properties['localsrcdir']

executeCommand("cp " + jarPath + "/" + jarName + " . ")

#Start Proxy
properties['node_listening_port'] =  properties['proxy_listening_port']
properties['node_ip_address'] =  properties['proxy_ip_address']
with open(configFileName, 'w') as fp:
   json.dump(properties, fp, indent = 2)
cmd ="java -cp " + jarName + " " + proxyMain + " " + configFileName
proxyThread = executeNonBlockingCommand(cmd)
proxyThread.start()

time.sleep(20)
print "Proxy Started"

# Start Datastore
properties['node_listening_port'] =  properties['remote_store_listening_port']
properties['node_ip_address'] =  properties['remote_store_ip_address']
with open(configFileName, 'w') as fp:
   json.dump(properties, fp, indent = 2)
#cmd = "java -jar " + datastoreJar + " " + configFileName
#datastoreThread = executeNonBlockingCommand(cmd)
#datastoreThread.start()

# Start specified number of clients
clientThreads = list()
for i in range(0,nbClients):
    port = random.randint(10000,20000)
    properties['node_listening_port'] = str(port)
    properties['node_uid'] = str(i);
    configFileNameC = configFileName + str(i)
    cmd = "gnome-terminal -e \" java -cp " + jarName + " " + clientMain +  " " + configFileNameC + " \""
    with open(configFileNameC, 'w') as fp:
        json.dump(properties, fp, indent = 2)
    clientThreads.append(executeNonBlockingCommand(cmd))
for t in clientThreads:
    t.start()
for t in clientThreads:
    t.join()
