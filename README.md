# Twitter Sentiment Analysis

This README describes the installation routine and how to run the Twitter Sentiment Analysis tool.

# Basic operation system
We used "Windows 7/8/8.1 64 bit". It's not tested on linux/mac or *nix systems, it should work though. If not please contact us.

## Dependencies

This tool depends on the following applications:

Apache Maven v3.1.1 (http://maven.apache.org/download.cgi)
MongoDB v2.4.8 (newer versions should also work - http://www.mongodb.org/downloads)

Make sure these two applications are installed and added to your PATH!

## How to run

To start the program do the following steps:

### Linux/Mac/*nix
As mentioned above we don't know if the application works out of the box on this systems - it should though.

1. install.sh
2. start-mongo.sh
3. start-server.sh
4. open the [webinterface](http://localhost:8080/) in a browser

### Windows

1. install.bat
2. start-mongo.bat
3. start-server.bat
4. open the [webinterface](http://localhost:8080/) in a browser

### What can I do?

1. Register
2. Login
3. List of all tasks
4. Subscribe to a new topic for a specified amount of time
5. Query a subscription (respectively the tweets collected via this subscription)
6. Choose a model for the classification
7. Be amazed by the output!