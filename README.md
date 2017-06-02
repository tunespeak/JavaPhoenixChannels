
# Phoenix Channel Client for Java and Android

This is a fork of the following repo:

- https://github.com/eoinsha/JavaPhoenixChannels

Changes have been made to optimise the behaviour to match the use cases of the Agent App and Messenger SDKs. A few of those changes are:
- Reduce unnecessary dependencies like SLF4J
- Add additional logging to catch internal exceptions and track issues
- Multiple bug fixes and code optimisations 


## Internal Testing

For production usage, bintray is used to host the jar file generated from this library. But for internal testing, Jitpack.io is used. 

[![](https://jitpack.io/v/metalwihen/JavaPhoenixChannels.svg)](https://jitpack.io/#metalwihen/JavaPhoenixChannels)
