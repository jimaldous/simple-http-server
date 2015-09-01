Simple HTTP Server
==================

Simple HTTP Server demonstrates a simple implementation of a web server with
the following features:
* HTTP support for GET and HEAD
* Multi-threaded processing of requests using thread pooling
* Support for HTTP/1.1 keep-alive
* Content stored as files in directories
* Implemented in Java (1.7+)

## Running the Server
To build the server in a jar file, use the following Maven command in the
root directory:

`mvn clean package`

To run with the default properties (found in src/main/resources) you may start
the server with the following command:

`java -jar target/simple-http-server.jar`

You may use your own version of the properties files (copied from
src/main/resources) by using the following command:

`java -cp .:target/simple-http-server.jar org.aldous.http.Server`

## Design Approach
This project was written as a learning experience. As such, I looked at some
existing implementations of simple web servers and learned of different
design strategies that have been used. Some implementations I looked at include:
* https://github.com/NanoHttpd/nanohttpd
* https://github.com/ibogomolov/WebServer

After studying this implementations and various other code snippets, I went
about writing my own implementation that combined the ideas from others into
a design that I felt was appropriate for my goal.

## Implementation Scope
In order to keep things simple, for now, I have made a few decisions about what
will and will not be supported in the web server. With additional work and
some refactoring, many of these missing features could be added in the future.
This web server:
* Was only built and tested on MacOS
* Must be run from command line
* Only supports HTTP/1.1 the HTTP-Version
* Has no support for HTTPS
* Only supports Content-Types of text/plain and text/html (utf-8 text)
* Has no built-in extensibility, such as the ability to plug in new handlers
* Has no authentication--all resources are public
* Has no support for Transfer-Encoding--the request-body is assumed to be text
that is transferred in a single request


## Design Overview
The general design of this server is similar to other simple HTTP servers. It
includes the following main components:
* A main class that control starting, configuration, and stopping, along with
a main() method for execution at the command line
* A main thread that loops to listen for HTTP traffic with a ServerSocket and
 that creates a new thread to manage each client's connection within threads
 in a thread pool.
* Classes to parse HTTP requests into an internal representation (class
 instance variables), handle each request to produce an appropriate response,
 and send responses back to the client.
